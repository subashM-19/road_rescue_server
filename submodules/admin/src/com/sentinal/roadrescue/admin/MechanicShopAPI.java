package com.sentinal.roadrescue.admin;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;
import com.sentinal.roadrescue.common.DateUtil;
import com.sentinal.roadrescue.mechanic.MechanicShopStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MechanicShopAPI
{
	public List<Map<String, Object>> getMechanicShops()
	{
		PersistenceQuery query = new PersistenceQuery("MechanicShop");

		PersistenceObject dataObject = PersistenceManager.get(query);

		List<Map<String, Object>> shops = new ArrayList<>();

		for (Row row : dataObject.getRows("MechanicShop"))
		{
			Map<String, Object> shop = new LinkedHashMap<>();

			shop.put("mechanic_shop_id", row.get("MechanicShopID"));

			shop.put("name", row.get("Name"));

			shop.put("address", row.get("Address"));

			shop.put("latitude", row.get("Latitude"));

			shop.put("longitude", row.get("Longitude"));

			shop.put("phone", row.get("Phone"));

			int status = ((Number) row.get("Status")).intValue();
			MechanicShopStatus mechanicShopStatus = MechanicShopStatus.fromCode(status);

			shop.put("status", mechanicShopStatus.getNodeRepresentation());

			shop.put("status_formatted", mechanicShopStatus.getDisplayName());

			Object lastModifiedTime = row.get("LastModifiedTime");

			shop.put("last_modified_time", lastModifiedTime);

			shop.put("last_modified_time_formatted", DateUtil.formatEpochTime(lastModifiedTime));

			shops.add(shop);
		}

		return shops;
	}

	public Map<String, Object> createMechanicShop(String name, String address, double latitude, double longitude, String phone)
	{
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

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("mechanic_shop_id", shop.get("MechanicShopID"));

		response.put("name", shop.get("Name"));

		response.put("status", MechanicShopStatus.OPENED.getNodeRepresentation());

		response.put("status_formatted", MechanicShopStatus.OPENED.getDisplayName());

		response.put("message", "Mechanic shop created successfully");

		return response;
	}

	public Map<String, Object> getMechanicShop(long mechanicShopID)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicShop").where(new Criteria("MechanicShopID", Criteria.EQUAL, mechanicShopID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		Row shop = dataObject.getRows("MechanicShop").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Mechanic shop not found: " + mechanicShopID));

		return buildMechanicShopResponse(shop);
	}

	public Map<String, Object> openMechanicShop(long mechanicShopID)
	{
		return updateMechanicShopStatus(mechanicShopID, MechanicShopStatus.OPENED, "Mechanic shop opened successfully");
	}

	public Map<String, Object> closeMechanicShop(long mechanicShopID)
	{
		return updateMechanicShopStatus(mechanicShopID, MechanicShopStatus.CLOSED, "Mechanic shop closed successfully");
	}

	private Map<String, Object> updateMechanicShopStatus(long mechanicShopID, MechanicShopStatus status, String message)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicShop").where(new Criteria("MechanicShopID", Criteria.EQUAL, mechanicShopID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		Row shop = dataObject.getRows("MechanicShop").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Mechanic shop not found: " + mechanicShopID));

		shop.set("Status", status.getCode());

		dataObject.updateRow(shop);

		PersistenceManager.update(dataObject);

		Map<String, Object> response = buildMechanicShopResponse(shop);

		response.put("message", message);

		return response;
	}

	private Map<String, Object> buildMechanicShopResponse(Row shop)
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