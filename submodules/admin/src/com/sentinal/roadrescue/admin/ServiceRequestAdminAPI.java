package com.sentinal.roadrescue.admin;

import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class ServiceRequestAdminAPI
{
	private static final class Holder
	{
		private static final ServiceRequestAdminOperations INSTANCE = TransactionProxy.create(ServiceRequestAdminOperations.class, new ServiceRequestAdminImpl());
	}

	public static ServiceRequestAdminOperations getServiceRequestAdminImpl()
	{
		return Holder.INSTANCE;
	}

	public List<Map<String, Object>> getServiceRequests()
	{
		return getServiceRequestAdminImpl().getServiceRequests();
	}
}
