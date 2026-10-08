package com.sentinal.roadrescue.mechanic;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.Join;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.common.DateUtil;
import com.sentinal.roadrescue.dispatch.AutoAssignmentService;
import com.sentinal.roadrescue.dispatch.AutoAssignmentOperations;
import com.sentinal.roadrescue.realtime.RealtimeHub;
import com.sentinal.roadrescue.service.ServiceRequestStatus;
import com.sentinal.roadrescue.service.ServiceType;
import com.sentinal.roadrescue.service.VehicleType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MechanicAssignmentImpl implements MechanicAssignmentOperations
{
	private final MechanicIdentityOperations mechanicIdentityService = MechanicIdentityService.getMechanicIdentityImpl();

	private final AutoAssignmentOperations autoAssignmentService = AutoAssignmentService.getAutoAssignmentImpl();

	@Override
	public List<Map<String, Object>> getMyAssignments()
	{
		long mechanicID = mechanicIdentityService.getCurrentMechanicID();

		PersistenceQuery query = new PersistenceQuery("MechanicAssignment").join(Join.INNER, "ServiceRequest", "MechanicAssignment", "ServiceRequestID", "ServiceRequest", "ServiceRequestID").join(Join.INNER, "Users", "ServiceRequest", "UserID", "Users", "UserID")

			.select("MechanicAssignment", "MechanicAssignmentID").select("MechanicAssignment", "ServiceRequestID").select("MechanicAssignment", "MechanicID").select("MechanicAssignment", "Status").select("MechanicAssignment", "AssignedTime").select("MechanicAssignment", "ExpiresTime").select("MechanicAssignment", "AcceptedTime").select("MechanicAssignment", "CompletedTime")

			.select("ServiceRequest", "ServiceRequestID").select("ServiceRequest", "UserID").select("ServiceRequest", "VehicleType").select("ServiceRequest", "ServiceType").select("ServiceRequest", "Description").select("ServiceRequest", "Latitude").select("ServiceRequest", "Longitude").select("ServiceRequest", "Status")

			.select("Users", "UserID").select("Users", "Name").select("Users", "Phone")

			.where(new Criteria("MechanicAssignment.MechanicID", Criteria.EQUAL, mechanicID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		List<Map<String, Object>> result = new ArrayList<>();

		for (Row assignmentRow : dataObject.getRows("MechanicAssignment"))
		{
			long serviceRequestID = ((Number) assignmentRow.get("ServiceRequestID")).longValue();

			Row serviceRequestRow = dataObject.getRow("ServiceRequest", new Criteria("ServiceRequestID", Criteria.EQUAL, serviceRequestID));

			long customerUserID = ((Number) serviceRequestRow.get("UserID")).longValue();

			Row userRow = dataObject.getRow("Users", new Criteria("UserID", Criteria.EQUAL, customerUserID));

			result.add(createAssignmentMap(assignmentRow, serviceRequestRow, userRow));
		}

		return result;
	}

	@Override
	public Map<String, Object> acceptAssignment(long mechanicAssignmentID)
	{
		Row assignment = getOwnedAssignment(mechanicAssignmentID);

		int assignmentStatus = ((Number) assignment.get("Status")).intValue();

		if (assignmentStatus != MechanicAssignmentStatus.PENDING.getCode())
		{
			throw new IllegalStateException("Assignment is not pending");
		}

		Object expiresValue = assignment.get("ExpiresTime");

		if (expiresValue != null && ((Number) expiresValue).longValue() < System.currentTimeMillis())
		{
			throw new IllegalStateException("Assignment offer has expired");
		}

		long serviceRequestID = ((Number) assignment.get("ServiceRequestID")).longValue();

		long mechanicID = ((Number) assignment.get("MechanicID")).longValue();

		Row request = getServiceRequest(serviceRequestID);

		Row mechanic = mechanicIdentityService.getMechanic(mechanicID);

		assignment.set("Status", MechanicAssignmentStatus.ACCEPTED.getCode());

		assignment.set("AcceptedTime", System.currentTimeMillis());

		request.set("Status", ServiceRequestStatus.ACCEPTED.getCode());

		mechanic.set("AvailabilityStatus", MechanicAvailabilityStatus.BUSY.getCode());

		PersistenceObject update = new PersistenceObject();

		update.updateRow(assignment);

		update.updateRow(request);

		update.updateRow(mechanic);

		PersistenceManager.update(update);

		broadcastStatus(serviceRequestID, "accepted");

		return createStatusResponse(mechanicAssignmentID, serviceRequestID, "Mechanic accepted the request", MechanicAssignmentStatus.ACCEPTED, ServiceRequestStatus.ACCEPTED);
	}

	@Override
	public Map<String, Object> rejectAssignment(long mechanicAssignmentID)
	{
		Row assignment = getOwnedAssignment(mechanicAssignmentID);

		int status = ((Number) assignment.get("Status")).intValue();

		if (status != MechanicAssignmentStatus.PENDING.getCode())
		{
			throw new IllegalStateException("Assignment is not pending");
		}

		long serviceRequestID = ((Number) assignment.get("ServiceRequestID")).longValue();

		long mechanicID = ((Number) assignment.get("MechanicID")).longValue();

		Row request = getServiceRequest(serviceRequestID);

		Row mechanic = mechanicIdentityService.getMechanic(mechanicID);

		assignment.set("Status", MechanicAssignmentStatus.REJECTED.getCode());

		request.set("Status", ServiceRequestStatus.PENDING.getCode());

		mechanic.set("AvailabilityStatus", MechanicAvailabilityStatus.AVAILABLE.getCode());

		PersistenceObject update = new PersistenceObject();

		update.updateRow(assignment);

		update.updateRow(request);

		update.updateRow(mechanic);

		PersistenceManager.update(update);

		broadcastStatus(serviceRequestID, "searching");

		autoAssignmentService.autoAssign(serviceRequestID);

		return createStatusResponse(mechanicAssignmentID, serviceRequestID, "Mechanic rejected the request", MechanicAssignmentStatus.REJECTED, ServiceRequestStatus.PENDING);
	}

	@Override
	public Map<String, Object> startJourney(long mechanicAssignmentID)
	{
		Row assignment = getOwnedAssignment(mechanicAssignmentID);

		requireAccepted(assignment);

		long serviceRequestID = ((Number) assignment.get("ServiceRequestID")).longValue();

		Row request = getServiceRequest(serviceRequestID);

		int requestStatus = ((Number) request.get("Status")).intValue();

		if (requestStatus != ServiceRequestStatus.ACCEPTED.getCode())
		{
			throw new IllegalStateException("Service request must be accepted first");
		}

		request.set("Status", ServiceRequestStatus.ON_THE_WAY.getCode());

		PersistenceObject update = new PersistenceObject();

		update.updateRow(request);

		PersistenceManager.update(update);

		broadcastStatus(serviceRequestID, "on_the_way");

		return createStatusResponse(mechanicAssignmentID, serviceRequestID, "Mechanic is on the way", MechanicAssignmentStatus.ACCEPTED, ServiceRequestStatus.ON_THE_WAY);
	}

	@Override
	public Map<String, Object> startService(long mechanicAssignmentID)
	{
		Row assignment = getOwnedAssignment(mechanicAssignmentID);

		requireAccepted(assignment);

		long serviceRequestID = ((Number) assignment.get("ServiceRequestID")).longValue();

		Row request = getServiceRequest(serviceRequestID);

		int requestStatus = ((Number) request.get("Status")).intValue();

		if (requestStatus != ServiceRequestStatus.ON_THE_WAY.getCode())
		{
			throw new IllegalStateException("Mechanic must be on the way before starting service");
		}

		request.set("Status", ServiceRequestStatus.IN_PROGRESS.getCode());

		PersistenceObject update = new PersistenceObject();

		update.updateRow(request);

		PersistenceManager.update(update);

		broadcastStatus(serviceRequestID, "in_progress");

		return createStatusResponse(mechanicAssignmentID, serviceRequestID, "Service started", MechanicAssignmentStatus.ACCEPTED, ServiceRequestStatus.IN_PROGRESS);
	}

	@Override
	public Map<String, Object> completeAssignment(long mechanicAssignmentID)
	{
		Row assignment = getOwnedAssignment(mechanicAssignmentID);

		requireAccepted(assignment);

		long serviceRequestID = ((Number) assignment.get("ServiceRequestID")).longValue();

		long mechanicID = ((Number) assignment.get("MechanicID")).longValue();

		Row request = getServiceRequest(serviceRequestID);

		int requestStatus = ((Number) request.get("Status")).intValue();

		if (requestStatus != ServiceRequestStatus.IN_PROGRESS.getCode())
		{
			throw new IllegalStateException("Service must be in progress before completion");
		}

		Row mechanic = mechanicIdentityService.getMechanic(mechanicID);

		assignment.set("Status", MechanicAssignmentStatus.COMPLETED.getCode());

		assignment.set("CompletedTime", System.currentTimeMillis());

		request.set("Status", ServiceRequestStatus.COMPLETED.getCode());

		mechanic.set("AvailabilityStatus", MechanicAvailabilityStatus.AVAILABLE.getCode());

		PersistenceObject update = new PersistenceObject();

		update.updateRow(assignment);

		update.updateRow(request);

		update.updateRow(mechanic);

		PersistenceManager.update(update);

		broadcastStatus(serviceRequestID, "completed");

		return createStatusResponse(mechanicAssignmentID, serviceRequestID, "Service completed", MechanicAssignmentStatus.COMPLETED, ServiceRequestStatus.COMPLETED);
	}

	private Row getOwnedAssignment(long mechanicAssignmentID)
	{
		long mechanicID = mechanicIdentityService.getCurrentMechanicID();

		PersistenceQuery query = new PersistenceQuery("MechanicAssignment").where(Criteria.and(new Criteria("MechanicAssignment.MechanicAssignmentID", Criteria.EQUAL, mechanicAssignmentID), new Criteria("MechanicAssignment.MechanicID", Criteria.EQUAL, mechanicID)));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("MechanicAssignment").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Mechanic assignment not found"));
	}

	private Row getServiceRequest(long serviceRequestID)
	{
		PersistenceQuery query = new PersistenceQuery("ServiceRequest").where(new Criteria("ServiceRequest.ServiceRequestID", Criteria.EQUAL, serviceRequestID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("ServiceRequest").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Service request not found"));
	}

	private void requireAccepted(Row assignment)
	{
		int status = ((Number) assignment.get("Status")).intValue();

		if (status != MechanicAssignmentStatus.ACCEPTED.getCode())
		{
			throw new IllegalStateException("Assignment is not active");
		}
	}

	private Map<String, Object> createAssignmentMap(Row assignment, Row request, Row user)
	{
		int assignmentStatusCode = ((Number) assignment.get("Status")).intValue();

		int requestStatusCode = ((Number) request.get("Status")).intValue();

		int vehicleCode = ((Number) request.get("VehicleType")).intValue();

		int serviceCode = ((Number) request.get("ServiceType")).intValue();

		MechanicAssignmentStatus assignmentStatus = MechanicAssignmentStatus.findByCode(assignmentStatusCode);

		ServiceRequestStatus requestStatus = ServiceRequestStatus.fromCode(requestStatusCode);

		VehicleType vehicleType = VehicleType.fromCode(vehicleCode);

		ServiceType serviceType = ServiceType.fromCode(serviceCode);

		Map<String, Object> result = new LinkedHashMap<>();

		result.put("mechanic_assignment_id", assignment.get("MechanicAssignmentID"));

		result.put("service_request_id", assignment.get("ServiceRequestID"));

		result.put("status", assignmentStatus.getNodeRepresentation());

		result.put("status_formatted", assignmentStatus.getDisplayName());

		result.put("service_request_status", requestStatus.getNodeRepresentation());

		result.put("service_request_status_formatted", requestStatus.getDisplayName());

		result.put("vehicle_type", vehicleType.getNodeRepresentation());

		result.put("vehicle_type_formatted", vehicleType.getDisplayName());

		result.put("service_type", serviceType.getNodeRepresentation());

		result.put("service_type_formatted", serviceType.getDisplayName());

		result.put("description", request.get("Description"));

		result.put("latitude", request.get("Latitude"));

		result.put("longitude", request.get("Longitude"));

		result.put("customer_name", user.get("Name"));

		result.put("customer_phone", user.get("Phone"));

		result.put("assigned_time", assignment.get("AssignedTime"));

		result.put("expires_time", assignment.get("ExpiresTime"));

		result.put("accepted_time", assignment.get("AcceptedTime"));

		result.put("completed_time", assignment.get("CompletedTime"));

		result.put("assigned_time_formatted", DateUtil.formatEpochTime(assignment.get("AssignedTime")));

		return result;
	}

	private Map<String, Object> createStatusResponse(long assignmentID, long requestID, String message, MechanicAssignmentStatus assignmentStatus, ServiceRequestStatus requestStatus)
	{
		Map<String, Object> response = new LinkedHashMap<>();

		response.put("mechanic_assignment_id", assignmentID);

		response.put("service_request_id", requestID);

		response.put("assignment_status", assignmentStatus.getNodeRepresentation());

		response.put("assignment_status_formatted", assignmentStatus.getDisplayName());

		response.put("service_request_status", requestStatus.getNodeRepresentation());

		response.put("service_request_status_formatted", requestStatus.getDisplayName());

		response.put("message", message);

		return response;
	}

	private void broadcastStatus(long requestID, String status)
	{
		RealtimeHub.broadcastRequest(requestID, RealtimeHub.message("type", "request_status", "service_request_id", requestID, "status", status));
	}
}