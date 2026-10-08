package com.sentinal.roadrescue.mechanic;

import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class MechanicAssignmentAPI
{
	private static final class Holder
	{
		private static final MechanicAssignmentOperations INSTANCE = TransactionProxy.create(MechanicAssignmentOperations.class, new MechanicAssignmentImpl());
	}

	public static MechanicAssignmentOperations getMechanicAssignmentImpl()
	{
		return Holder.INSTANCE;
	}

	public List<Map<String, Object>> getMyAssignments()
	{
		return getMechanicAssignmentImpl().getMyAssignments();
	}

	public Map<String, Object> acceptAssignment(long mechanicAssignmentID)
	{
		return getMechanicAssignmentImpl().acceptAssignment(mechanicAssignmentID);
	}

	public Map<String, Object> rejectAssignment(long mechanicAssignmentID)
	{
		return getMechanicAssignmentImpl().rejectAssignment(mechanicAssignmentID);
	}

	public Map<String, Object> startJourney(long mechanicAssignmentID)
	{
		return getMechanicAssignmentImpl().startJourney(mechanicAssignmentID);
	}

	public Map<String, Object> startService(long mechanicAssignmentID)
	{
		return getMechanicAssignmentImpl().startService(mechanicAssignmentID);
	}

	public Map<String, Object> completeAssignment(long mechanicAssignmentID)
	{
		return getMechanicAssignmentImpl().completeAssignment(mechanicAssignmentID);
	}
}
