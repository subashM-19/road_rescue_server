package com.sentinal.roadrescue.admin;

import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class MechanicAdminAPI
{
	private static final class Holder
	{
		private static final MechanicAdminOperations INSTANCE = TransactionProxy.create(MechanicAdminOperations.class, new MechanicAdminImpl());
	}

	public static MechanicAdminOperations getMechanicAdminImpl()
	{
		return Holder.INSTANCE;
	}

	public List<Map<String, Object>> getMechanics()
	{
		return getMechanicAdminImpl().getMechanics();
	}

	public Map<String, Object> makeAvailable(long mechanicID)
	{
		return getMechanicAdminImpl().makeAvailable(mechanicID);
	}

	public Map<String, Object> makeOffline(long mechanicID)
	{
		return getMechanicAdminImpl().makeOffline(mechanicID);
	}
}
