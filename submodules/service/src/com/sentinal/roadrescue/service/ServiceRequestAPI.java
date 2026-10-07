package com.sentinal.roadrescue.service;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.dispatch.AutoAssignmentService;
import com.sentinal.roadrescue.realtime.TrackingService;
import com.sentinal.roadrescue.security.RoadRescueSecurityContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceRequestAPI
{
	private final AutoAssignmentService autoAssignmentService = new AutoAssignmentService();

	private final TrackingService trackingService = new TrackingService();

	public Map<String, Object> createServiceRequest(String vehicleType, String serviceType, String description, double latitude, double longitude)
	{
		long userID = RoadRescueSecurityContext.getUserID();

		VehicleType vehicle = VehicleType.fromNodeRepresentation(vehicleType);

		ServiceType service = ServiceType.fromNodeRepresentation(serviceType);

		validateLocation(latitude, longitude);

		Row request = new Row("ServiceRequest");

		request.set("UserID", userID);

		request.set("VehicleType", vehicle.getCode());

		request.set("ServiceType", service.getCode());

		request.set("Description", description);

		request.set("Latitude", latitude);

		request.set("Longitude", longitude);

		request.set("Status", ServiceRequestStatus.PENDING.getCode());

		request.set("CreatedTime", System.currentTimeMillis());

		PersistenceObject dataObject = new PersistenceObject();

		dataObject.addRowForInsert(request);

		PersistenceManager.update(dataObject);

		long serviceRequestID = ((Number) request.get("ServiceRequestID")).longValue();

		Long assignmentID = autoAssignmentService.autoAssign(serviceRequestID);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("service_request_id", serviceRequestID);

		if (assignmentID != null)
		{
			response.put("status", ServiceRequestStatus.ASSIGNED.getNodeRepresentation());

			response.put("status_formatted", ServiceRequestStatus.ASSIGNED.getDisplayName());

			response.put("mechanic_assignment_id", assignmentID);
		}
		else
		{
			response.put("status", ServiceRequestStatus.PENDING.getNodeRepresentation());

			response.put("status_formatted", ServiceRequestStatus.PENDING.getDisplayName());

			response.put("mechanic_assignment_id", null);
		}

		return response;
	}

	public Map<String, Object> getServiceRequest(long serviceRequestID)
	{
		Row request = getOwnedServiceRequest(serviceRequestID);

		int statusCode = ((Number) request.get("Status")).intValue();

		int vehicleCode = ((Number) request.get("VehicleType")).intValue();

		int serviceCode = ((Number) request.get("ServiceType")).intValue();

		VehicleType vehicleType = VehicleType.fromCode(vehicleCode);

		ServiceType serviceType = ServiceType.fromCode(serviceCode);

		ServiceRequestStatus status = ServiceRequestStatus.fromCode(statusCode);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("service_request_id", serviceRequestID);

		response.put("status", status.getNodeRepresentation());

		response.put("status_formatted", status.getDisplayName());

		response.put("vehicle_type", vehicleType.getNodeRepresentation());

		response.put("vehicle_type_formatted", vehicleType.getDisplayName());

		response.put("service_type", serviceType.getNodeRepresentation());

		response.put("service_type_formatted", serviceType.getDisplayName());

		response.put("description", request.get("Description"));

		response.put("latitude", request.get("Latitude"));

		response.put("longitude", request.get("Longitude"));

		return response;
	}

	public Map<String, Object> getTracking(long serviceRequestID)
	{
		return trackingService.getAuthorizedSnapshot(serviceRequestID);
	}

	private Row getOwnedServiceRequest(long serviceRequestID)
	{
		long userID = RoadRescueSecurityContext.getUserID();

		PersistenceQuery query = new PersistenceQuery("ServiceRequest").where(Criteria.and(new Criteria("ServiceRequest.ServiceRequestID", Criteria.EQUAL, serviceRequestID), new Criteria("ServiceRequest.UserID", Criteria.EQUAL, userID)));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("ServiceRequest").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Service request not found"));
	}

	private void validateLocation(double latitude, double longitude)
	{
		if (latitude < -90 || latitude > 90)
		{
			throw new IllegalArgumentException("Invalid latitude");
		}

		if (longitude < -180 || longitude > 180)
		{
			throw new IllegalArgumentException("Invalid longitude");
		}
	}

	public List<Map<String, Object>> getMyServiceRequests()
	{
		long userID = RoadRescueSecurityContext.getUserID();

		PersistenceQuery query = new PersistenceQuery("ServiceRequest").where(new Criteria("ServiceRequest.UserID", Criteria.EQUAL, userID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		List<Map<String, Object>> result = new ArrayList<>();

		for (Row request : dataObject.getRows("ServiceRequest"))
		{
			int statusCode = ((Number) request.get("Status")).intValue();

			int vehicleCode = ((Number) request.get("VehicleType")).intValue();

			int serviceCode = ((Number) request.get("ServiceType")).intValue();

			ServiceRequestStatus status = ServiceRequestStatus.fromCode(statusCode);

			VehicleType vehicleType = VehicleType.fromCode(vehicleCode);

			ServiceType serviceType = ServiceType.fromCode(serviceCode);

			Map<String, Object> item = new LinkedHashMap<>();

			item.put("service_request_id", request.get("ServiceRequestID"));

			item.put("status", status.getNodeRepresentation());

			item.put("status_formatted", status.getDisplayName());

			item.put("vehicle_type", vehicleType.getNodeRepresentation());

			item.put("vehicle_type_formatted", vehicleType.getDisplayName());

			item.put("service_type", serviceType.getNodeRepresentation());

			item.put("service_type_formatted", serviceType.getDisplayName());

			item.put("description", request.get("Description"));

			item.put("latitude", request.get("Latitude"));

			item.put("longitude", request.get("Longitude"));

			item.put("created_time", request.get("CreatedTime"));

			result.add(item);
		}

		result.sort((left, right) -> Long.compare(((Number) right.get("service_request_id")).longValue(),

			((Number) left.get("service_request_id")).longValue()));

		return result;
	}
}