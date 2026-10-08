package com.sentinal.roadrescue.admin;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.Join;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;
import com.sentinal.roadrescue.common.DateUtil;
import com.sentinal.roadrescue.service.ServiceRequestStatus;
import com.sentinal.roadrescue.service.ServiceType;
import com.sentinal.roadrescue.service.VehicleType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceRequestAdminImpl implements ServiceRequestAdminOperations
{
	@Override
	public List<Map<String, Object>> getServiceRequests()
	{
		PersistenceQuery query = new PersistenceQuery("ServiceRequest").join(Join.INNER, "Users", "ServiceRequest", "UserID", "Users", "UserID");

		PersistenceObject dataObject = PersistenceManager.get(query);

		List<Map<String, Object>> requests = new ArrayList<>();

		for (Row requestRow : dataObject.getRows("ServiceRequest"))
		{
			long userID = ((Number) requestRow.get("UserID")).longValue();

			Row userRow = dataObject.getRow("Users", new Criteria("UserID", Criteria.EQUAL, userID));

			int statusCode = ((Number) requestRow.get("Status")).intValue();

			int vehicleTypeCode = ((Number) requestRow.get("VehicleType")).intValue();

			int serviceTypeCode = ((Number) requestRow.get("ServiceType")).intValue();

			ServiceRequestStatus status = ServiceRequestStatus.fromCode(statusCode);

			VehicleType vehicleType = VehicleType.fromCode(vehicleTypeCode);

			ServiceType serviceType = ServiceType.fromCode(serviceTypeCode);

			Map<String, Object> request = new LinkedHashMap<>();

			request.put("service_request_id", requestRow.get("ServiceRequestID"));

			request.put("user_id", userID);

			request.put("customer_name", userRow.get("Name"));

			request.put("vehicle_type", vehicleType.getNodeRepresentation());

			request.put("vehicle_type_formatted", vehicleType.getDisplayName());

			request.put("service_type", serviceType.getNodeRepresentation());

			request.put("service_type_formatted", serviceType.getDisplayName());

			request.put("status", status.getNodeRepresentation());

			request.put("status_formatted", status.getDisplayName());

			request.put("description", requestRow.get("Description"));

			request.put("latitude", requestRow.get("Latitude"));

			request.put("longitude", requestRow.get("Longitude"));

			Object createdTime = requestRow.get("CreatedTime");

			request.put("created_time", createdTime);

			request.put("created_time_formatted", DateUtil.formatEpochTime(createdTime));

			Object lastModifiedTime = requestRow.get("LastModifiedTime");

			request.put("last_modified_time", lastModifiedTime);

			request.put("last_modified_time_formatted", DateUtil.formatEpochTime(lastModifiedTime));

			requests.add(request);
		}

		return requests;
	}
}