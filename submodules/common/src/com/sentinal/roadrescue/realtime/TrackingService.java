package com.sentinal.roadrescue.realtime;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.mechanic.MechanicAssignmentStatus;
import com.sentinal.roadrescue.mechanic.MechanicIdentityService;
import com.sentinal.roadrescue.roles.UserRole;
import com.sentinal.roadrescue.security.RoadRescueSecurityContext;
import com.sentinal.roadrescue.service.ServiceRequestStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TrackingService
{
	public Map<String, Object> getAuthorizedSnapshot(long serviceRequestID)
	{
		long userID = RoadRescueSecurityContext.getUserID();

		Set<Integer> roles = RoadRescueSecurityContext.getPrincipal().getRoles();

		assertAccess(userID, roles, serviceRequestID);

		return getSnapshot(serviceRequestID);
	}

	public void assertAccess(long userID, Set<Integer> roles, long serviceRequestID)
	{
		if (roles.contains(UserRole.PLATFORM_ADMIN.getCode()))
		{
			return;
		}

		Row request = getServiceRequest(serviceRequestID);

		long ownerUserID = ((Number) request.get("UserID")).longValue();

		if (ownerUserID == userID)
		{
			return;
		}

		if (roles.contains(UserRole.MECHANIC.getCode()) && mechanicOwnsRequest(userID, serviceRequestID))
		{
			return;
		}

		throw new SecurityException("Access denied for service request");
	}

	public boolean mechanicOwnsRequest(long mechanicUserID, long serviceRequestID)
	{
		Row mechanic = new MechanicIdentityService().getMechanicByUserID(mechanicUserID);

		long mechanicID = ((Number) mechanic.get("MechanicID")).longValue();

		PersistenceQuery query = new PersistenceQuery("MechanicAssignment").where(Criteria.and(new Criteria("MechanicAssignment.ServiceRequestID", Criteria.EQUAL, serviceRequestID), new Criteria("MechanicAssignment.MechanicID", Criteria.EQUAL, mechanicID)));

		PersistenceObject dataObject = PersistenceManager.get(query);

		for (Row row : dataObject.getRows("MechanicAssignment"))
		{
			int status = ((Number) row.get("Status")).intValue();

			if (status == MechanicAssignmentStatus.PENDING.getCode() || status == MechanicAssignmentStatus.ACCEPTED.getCode() || status == MechanicAssignmentStatus.COMPLETED.getCode())
			{
				return true;
			}
		}

		return false;
	}

	public Map<String, Object> getSnapshot(long serviceRequestID)
	{
		Row request = getServiceRequest(serviceRequestID);

		ServiceRequestStatus status = ServiceRequestStatus.fromCode(((Number) request.get("Status")).intValue());

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("type", "tracking_snapshot");

		response.put("service_request_id", serviceRequestID);

		response.put("status", status.getNodeRepresentation());

		response.put("customer_latitude", request.get("Latitude"));

		response.put("customer_longitude", request.get("Longitude"));

		Row assignment = getLatestActiveAssignment(serviceRequestID);

		if (assignment == null)
		{
			response.put("mechanic_assignment_id", null);

			response.put("mechanic_id", null);

			response.put("mechanic_latitude", null);

			response.put("mechanic_longitude", null);

			return response;
		}

		long mechanicID = ((Number) assignment.get("MechanicID")).longValue();

		Row mechanic = new MechanicIdentityService().getMechanic(mechanicID);

		response.put("mechanic_assignment_id", assignment.get("MechanicAssignmentID"));

		response.put("mechanic_id", mechanicID);

		response.put("mechanic_latitude", mechanic.get("Latitude"));

		response.put("mechanic_longitude", mechanic.get("Longitude"));

		response.put("mechanic_location_updated_time", mechanic.get("LocationUpdatedTime"));

		long mechanicUserID = ((Number) mechanic.get("UserID")).longValue();

		Row mechanicUser = getUser(mechanicUserID);

		response.put("mechanic_name", mechanicUser.get("Name"));

		response.put("mechanic_phone", mechanicUser.get("Phone"));

		return response;
	}

	private Row getUser(long userID)
	{
		PersistenceQuery query = new PersistenceQuery("Users").where(new Criteria("Users.UserID", Criteria.EQUAL, userID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("Users").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	private Row getLatestActiveAssignment(long serviceRequestID)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicAssignment").where(new Criteria("MechanicAssignment.ServiceRequestID", Criteria.EQUAL, serviceRequestID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		Row latest = null;

		for (Row row : dataObject.getRows("MechanicAssignment"))
		{
			int status = ((Number) row.get("Status")).intValue();

			if (status != MechanicAssignmentStatus.PENDING.getCode() && status != MechanicAssignmentStatus.ACCEPTED.getCode() && status != MechanicAssignmentStatus.COMPLETED.getCode())
			{
				continue;
			}

			if (latest == null || ((Number) row.get("MechanicAssignmentID")).longValue() > ((Number) latest.get("MechanicAssignmentID")).longValue())
			{
				latest = row;
			}
		}

		return latest;
	}

	private Row getServiceRequest(long serviceRequestID)
	{
		PersistenceQuery query = new PersistenceQuery("ServiceRequest").where(new Criteria("ServiceRequest.ServiceRequestID", Criteria.EQUAL, serviceRequestID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("ServiceRequest").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Service request not found"));
	}
}