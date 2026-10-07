package com.sentinal.roadrescue.mechanic;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import java.util.LinkedHashMap;
import java.util.Map;

public class MechanicAvailabilityAPI
{
	private final MechanicIdentityService mechanicIdentityService = new MechanicIdentityService();

	public Map<String, Object> updateAvailability(String status)
	{
		MechanicAvailabilityStatus requested = MechanicAvailabilityStatus.fromNodeRepresentation(status);

		if (requested != MechanicAvailabilityStatus.AVAILABLE && requested != MechanicAvailabilityStatus.OFFLINE)
		{
			throw new IllegalArgumentException("Availability can only be available or offline");
		}

		Row mechanic = mechanicIdentityService.getCurrentMechanic();

		long mechanicID = ((Number) mechanic.get("MechanicID")).longValue();

		if (requested == MechanicAvailabilityStatus.OFFLINE && hasActiveAssignment(mechanicID))
		{
			throw new IllegalStateException("Cannot go offline while an assignment is active");
		}

		mechanic.set("AvailabilityStatus", requested.getCode());

		PersistenceObject update = new PersistenceObject();

		update.updateRow(mechanic);

		PersistenceManager.update(update);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("availability_status", requested.getNodeRepresentation());

		response.put("availability_status_formatted", requested.getDisplayName());

		return response;
	}

	private boolean hasActiveAssignment(long mechanicID)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicAssignment").where(new Criteria("MechanicAssignment.MechanicID", Criteria.EQUAL, mechanicID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		for (Row row : dataObject.getRows("MechanicAssignment"))
		{
			int status = ((Number) row.get("Status")).intValue();

			if (status == MechanicAssignmentStatus.PENDING.getCode() || status == MechanicAssignmentStatus.ACCEPTED.getCode())
			{
				return true;
			}
		}

		return false;
	}
}