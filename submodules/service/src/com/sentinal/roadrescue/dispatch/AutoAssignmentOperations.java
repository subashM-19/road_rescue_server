package com.sentinal.roadrescue.dispatch;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface AutoAssignmentOperations
{
	Long autoAssign(long serviceRequestID);

	void expirePendingAssignments();

	void assignPendingRequests();

	void cancelStaleServiceRequests();
}
