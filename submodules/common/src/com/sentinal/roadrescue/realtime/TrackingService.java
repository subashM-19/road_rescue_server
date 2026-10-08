package com.sentinal.roadrescue.realtime;

import java.util.Map;
import java.util.Set;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class TrackingService
{
	private static final class Holder
	{
		private static final TrackingOperations INSTANCE = TransactionProxy.create(TrackingOperations.class, new TrackingImpl());
	}

	public static TrackingOperations getTrackingImpl()
	{
		return Holder.INSTANCE;
	}

	public Map<String, Object> getAuthorizedSnapshot(long serviceRequestID)
	{
		return getTrackingImpl().getAuthorizedSnapshot(serviceRequestID);
	}

	public void assertAccess(long userID, Set<Integer> roles, long serviceRequestID)
	{
		getTrackingImpl().assertAccess(userID, roles, serviceRequestID);
	}

	public boolean mechanicOwnsRequest(long mechanicUserID, long serviceRequestID)
	{
		return getTrackingImpl().mechanicOwnsRequest(mechanicUserID, serviceRequestID);
	}

	public Map<String, Object> getSnapshot(long serviceRequestID)
	{
		return getTrackingImpl().getSnapshot(serviceRequestID);
	}
}
