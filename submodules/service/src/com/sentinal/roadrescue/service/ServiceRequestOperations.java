package com.sentinal.roadrescue.service;

import java.util.List;
import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface ServiceRequestOperations
{
	Map<String, Object> createServiceRequest(String vehicleType, String serviceType, String description, double latitude, double longitude);

	Map<String, Object> getServiceRequest(long serviceRequestID);

	Map<String, Object> getTracking(long serviceRequestID);

	List<Map<String, Object>> getMyServiceRequests();
}
