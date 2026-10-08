package com.sentinal.roadrescue.admin;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.Join;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;
import com.sentinal.roadrescue.common.DateUtil;
import com.sentinal.roadrescue.mechanic.MechanicAvailabilityStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MechanicAdminImpl implements MechanicAdminOperations
{
	@Override
	public List<Map<String, Object>> getMechanics()
	{
		PersistenceQuery query = new PersistenceQuery("Mechanic").join(Join.INNER, "Users", "Mechanic", "UserID", "Users", "UserID");

		PersistenceObject dataObject = PersistenceManager.get(query);

		List<Row> mechanicRows = dataObject.getRows("Mechanic");

		List<Map<String, Object>> mechanics = new ArrayList<>();

		for (Row mechanicRow : mechanicRows)
		{
			long userID = ((Number) mechanicRow.get("UserID")).longValue();

			Row userRow = dataObject.getRow("Users", new Criteria("UserID", Criteria.EQUAL, userID));
			int availabilityCode = ((Number) mechanicRow.get("AvailabilityStatus")).intValue();

			MechanicAvailabilityStatus availability = MechanicAvailabilityStatus.fromCode(availabilityCode);

			Map<String, Object> mechanic = new LinkedHashMap<>();

			mechanic.put("mechanic_id", mechanicRow.get("MechanicID"));

			mechanic.put("user_id", userID);

			mechanic.put("name", userRow.get("Name"));

			mechanic.put("availability_status", availability.getNodeRepresentation());

			mechanic.put("availability_status_formatted", availability.getDisplayName());

			mechanic.put("latitude", mechanicRow.get("Latitude"));

			mechanic.put("longitude", mechanicRow.get("Longitude"));

			Object lastModifiedTime = mechanicRow.get("LastModifiedTime");

			mechanic.put("last_modified_time", lastModifiedTime);

			mechanic.put("last_modified_time_formatted", DateUtil.formatEpochTime(lastModifiedTime));

			mechanics.add(mechanic);
		}

		return mechanics;
	}

	@Override
	public Map<String, Object> makeAvailable(long mechanicID)
	{
		return updateAvailability(mechanicID, MechanicAvailabilityStatus.AVAILABLE, "Mechanic marked available");
	}

	@Override
	public Map<String, Object> makeOffline(long mechanicID)
	{
		return updateAvailability(mechanicID, MechanicAvailabilityStatus.OFFLINE, "Mechanic marked offline");
	}

	private Map<String, Object> updateAvailability(long mechanicID, MechanicAvailabilityStatus status, String message)
	{
		PersistenceQuery query = new PersistenceQuery("Mechanic").where(new Criteria("MechanicID", Criteria.EQUAL, mechanicID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		Row mechanic = dataObject.getRows("Mechanic").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Mechanic not found: " + mechanicID));

		int currentStatusCode = ((Number) mechanic.get("AvailabilityStatus")).intValue();

		MechanicAvailabilityStatus currentStatus = MechanicAvailabilityStatus.fromCode(currentStatusCode);

		if (currentStatus == MechanicAvailabilityStatus.BUSY)
		{
			throw new IllegalStateException("Busy mechanic availability cannot be changed by admin");
		}

		mechanic.set("AvailabilityStatus", status.getCode());

		dataObject.updateRow(mechanic);

		PersistenceManager.update(dataObject);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("mechanic_id", mechanicID);

		response.put("availability_status", status.getNodeRepresentation());

		response.put("availability_status_formatted", status.getDisplayName());

		response.put("message", message);

		return response;
	}
}