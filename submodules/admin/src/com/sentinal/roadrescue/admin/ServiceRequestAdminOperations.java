package com.sentinal.roadrescue.admin;

import java.util.List;
import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface ServiceRequestAdminOperations
{
	List<Map<String, Object>> getServiceRequests();
}
