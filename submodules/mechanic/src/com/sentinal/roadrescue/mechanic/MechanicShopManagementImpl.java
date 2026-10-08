package com.sentinal.roadrescue.mechanic;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;
import com.sentinal.roadrescue.common.DateUtil;
import com.sentinal.roadrescue.roles.UserRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MechanicShopManagementImpl implements MechanicShopManagementOperations
{
	@Override
	public Map<String, Object> getMyShop(long mechanicID)
	{
		Row mechanic = getMechanic(mechanicID);

		requireMechanicAdmin(mechanic);

		Object mechanicShopIDValue = mechanic.get("MechanicShopID");

		if (mechanicShopIDValue == null)
		{
			Map<String, Object> response = new LinkedHashMap<>();

			response.put("mechanic_id", mechanicID);
			response.put("shop", null);

			return response;
		}

		long mechanicShopID = ((Number) mechanicShopIDValue).longValue();

		Row shop = getShop(mechanicShopID);

		return buildShopResponse(shop);
	}

	@Override
	public Map<String, Object> createShop(long mechanicID, String name, String address, double latitude, double longitude, String phone)
	{
		Row mechanic = getMechanic(mechanicID);

		requireMechanicAdmin(mechanic);

		if (mechanic.get("MechanicShopID") != null)
		{
			throw new IllegalStateException("Mechanic admin already belongs to a shop");
		}

		if (name == null || name.trim().isEmpty())
		{
			throw new IllegalArgumentException("Mechanic shop name is required");
		}

		if (latitude < -90 || latitude > 90)
		{
			throw new IllegalArgumentException("Invalid latitude");
		}

		if (longitude < -180 || longitude > 180)
		{
			throw new IllegalArgumentException("Invalid longitude");
		}

		Row shop = new Row("MechanicShop");

		shop.set("Name", name.trim());
		shop.set("Address", address);
		shop.set("Latitude", latitude);
		shop.set("Longitude", longitude);
		shop.set("Phone", phone);
		shop.set("Status", MechanicShopStatus.OPENED.getCode());

		PersistenceObject dataObject = new PersistenceObject();

		dataObject.addRowForInsert(shop);

		PersistenceManager.update(dataObject);

		long mechanicShopID = ((Number) shop.get("MechanicShopID")).longValue();

		mechanic.set("MechanicShopID", mechanicShopID);

		PersistenceObject mechanicObject = new PersistenceObject();

		mechanicObject.updateRow(mechanic);

		PersistenceManager.update(mechanicObject);

		Map<String, Object> response = buildShopResponse(shop);

		response.put("message", "Mechanic shop created successfully");

		return response;
	}

	@Override
	public List<Map<String, Object>> getShopMechanics(long mechanicID)
	{
		Row mechanicAdmin = getMechanic(mechanicID);

		requireMechanicAdmin(mechanicAdmin);

		long mechanicShopID = getMechanicShopID(mechanicAdmin);

		PersistenceQuery query = new PersistenceQuery("Mechanic").where(new Criteria("MechanicShopID", Criteria.EQUAL, mechanicShopID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		List<Map<String, Object>> mechanics = new ArrayList<>();

		for (Row mechanic : dataObject.getRows("Mechanic"))
		{
			long userID = ((Number) mechanic.get("UserID")).longValue();

			Row user = getUser(userID);

			int availabilityCode = ((Number) mechanic.get("AvailabilityStatus")).intValue();

			MechanicAvailabilityStatus availability = MechanicAvailabilityStatus.fromCode(availabilityCode);

			Map<String, Object> response = new LinkedHashMap<>();

			response.put("mechanic_id", mechanic.get("MechanicID"));

			response.put("user_id", userID);

			response.put("name", user.get("Name"));

			response.put("email", user.get("Email"));

			response.put("phone", user.get("Phone"));

			response.put("availability_status", availability.getNodeRepresentation());

			response.put("availability_status_formatted", availability.getDisplayName());

			response.put("latitude", mechanic.get("Latitude"));

			response.put("longitude", mechanic.get("Longitude"));

			Object lastModifiedTime = mechanic.get("LastModifiedTime");

			response.put("last_modified_time", lastModifiedTime);

			response.put("last_modified_time_formatted", DateUtil.formatEpochTime(lastModifiedTime));

			mechanics.add(response);
		}

		return mechanics;
	}

	@Override
	public Map<String, Object> makeShopMechanicAvailable(long mechanicID, long targetMechanicID)
	{
		return updateShopMechanicAvailability(mechanicID, targetMechanicID, MechanicAvailabilityStatus.AVAILABLE);
	}

	@Override
	public Map<String, Object> makeShopMechanicOffline(long mechanicID, long targetMechanicID)
	{
		return updateShopMechanicAvailability(mechanicID, targetMechanicID, MechanicAvailabilityStatus.OFFLINE);
	}

	private Map<String, Object> updateShopMechanicAvailability(long mechanicID, long targetMechanicID, MechanicAvailabilityStatus status)
	{
		Row mechanicAdmin = getMechanic(mechanicID);

		requireMechanicAdmin(mechanicAdmin);

		long mechanicShopID = getMechanicShopID(mechanicAdmin);

		Row targetMechanic = getMechanic(targetMechanicID);

		long targetShopID = getMechanicShopID(targetMechanic);

		if (mechanicShopID != targetShopID)
		{
			throw new IllegalStateException("Mechanic does not belong to your shop");
		}

		int currentStatusCode = ((Number) targetMechanic.get("AvailabilityStatus")).intValue();

		MechanicAvailabilityStatus currentStatus = MechanicAvailabilityStatus.fromCode(currentStatusCode);

		if (currentStatus == MechanicAvailabilityStatus.BUSY)
		{
			throw new IllegalStateException("Busy mechanic availability cannot be changed");
		}

		targetMechanic.set("AvailabilityStatus", status.getCode());

		PersistenceObject dataObject = new PersistenceObject();

		dataObject.updateRow(targetMechanic);

		PersistenceManager.update(dataObject);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("mechanic_id", targetMechanicID);

		response.put("availability_status", status.getNodeRepresentation());

		response.put("availability_status_formatted", status.getDisplayName());

		response.put("message", "Mechanic availability updated");

		return response;
	}

	private void requireMechanicAdmin(Row mechanic)
	{
		long userID = ((Number) mechanic.get("UserID")).longValue();

		Row user = getUser(userID);

		String userType = (String) user.get("UserType");

		UserRole role = UserRole.findByNodeRepresentation(userType);

		if (role != UserRole.MECHANIC_ADMIN)
		{
			throw new IllegalStateException("Mechanic admin permission required");
		}
	}

	private long getMechanicShopID(Row mechanic)
	{
		Object value = mechanic.get("MechanicShopID");

		if (value == null)
		{
			throw new IllegalStateException("Mechanic does not belong to a shop");
		}

		return ((Number) value).longValue();
	}

	private Row getMechanic(long mechanicID)
	{
		PersistenceQuery query = new PersistenceQuery("Mechanic").where(new Criteria("MechanicID", Criteria.EQUAL, mechanicID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("Mechanic").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Mechanic not found: " + mechanicID));
	}

	private Row getUser(long userID)
	{
		PersistenceQuery query = new PersistenceQuery("Users").where(new Criteria("UserID", Criteria.EQUAL, userID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("Users").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("User not found: " + userID));
	}

	private Row getShop(long mechanicShopID)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicShop").where(new Criteria("MechanicShopID", Criteria.EQUAL, mechanicShopID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("MechanicShop").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Mechanic shop not found: " + mechanicShopID));
	}

	private Map<String, Object> buildShopResponse(Row shop)
	{
		int statusCode = ((Number) shop.get("Status")).intValue();

		MechanicShopStatus status = MechanicShopStatus.fromCode(statusCode);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("mechanic_shop_id", shop.get("MechanicShopID"));

		response.put("name", shop.get("Name"));

		response.put("address", shop.get("Address"));

		response.put("latitude", shop.get("Latitude"));

		response.put("longitude", shop.get("Longitude"));

		response.put("phone", shop.get("Phone"));

		response.put("status", status.getNodeRepresentation());

		response.put("status_formatted", status.getDisplayName());

		Object lastModifiedTime = shop.get("LastModifiedTime");

		response.put("last_modified_time", lastModifiedTime);

		response.put("last_modified_time_formatted", DateUtil.formatEpochTime(lastModifiedTime));

		return response;
	}
}