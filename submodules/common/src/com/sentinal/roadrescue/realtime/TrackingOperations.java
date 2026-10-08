package com.sentinal.roadrescue.realtime;

import java.util.Map;
import java.util.Set;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface TrackingOperations
{
	Map<String, Object> getAuthorizedSnapshot(long serviceRequestID);

	void assertAccess(long userID, Set<Integer> roles, long serviceRequestID);

	boolean mechanicOwnsRequest(long mechanicUserID, long serviceRequestID);

	Map<String, Object> getSnapshot(long serviceRequestID);
}
