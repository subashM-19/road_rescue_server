package com.sentinal.roadrescue.service;

import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class ServiceRequestAPI
{
	private static final class Holder
	{
		private static final ServiceRequestOperations INSTANCE = TransactionProxy.create(ServiceRequestOperations.class, new ServiceRequestImpl());
	}

	public static ServiceRequestOperations getServiceRequestImpl()
	{
		return Holder.INSTANCE;
	}

	public Map<String, Object> createServiceRequest(String vehicleType, String serviceType, String description, double latitude, double longitude)
	{
		return getServiceRequestImpl().createServiceRequest(vehicleType, serviceType, description, latitude, longitude);
	}

	public Map<String, Object> getServiceRequest(long serviceRequestID)
	{
		return getServiceRequestImpl().getServiceRequest(serviceRequestID);
	}

	public Map<String, Object> getTracking(long serviceRequestID)
	{
		return getServiceRequestImpl().getTracking(serviceRequestID);
	}

	public List<Map<String, Object>> getMyServiceRequests()
	{
		return getServiceRequestImpl().getMyServiceRequests();
	}
}
