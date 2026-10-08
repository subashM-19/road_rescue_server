package com.sentinal.roadrescue.dispatch;

import com.sentinal.persistencefw.transaction.TransactionProxy;

public class AutoAssignmentService
{
	private static final class Holder
	{
		private static final AutoAssignmentOperations INSTANCE = TransactionProxy.create(AutoAssignmentOperations.class, new AutoAssignmentImpl());
	}

	public static AutoAssignmentOperations getAutoAssignmentImpl()
	{
		return Holder.INSTANCE;
	}

	public Long autoAssign(long serviceRequestID)
	{
		return getAutoAssignmentImpl().autoAssign(serviceRequestID);
	}

	public void expirePendingAssignments()
	{
		getAutoAssignmentImpl().expirePendingAssignments();
	}

	public void assignPendingRequests()
	{
		getAutoAssignmentImpl().assignPendingRequests();
	}

	public void cancelStaleServiceRequests()
	{
		getAutoAssignmentImpl().cancelStaleServiceRequests();
	}
}
