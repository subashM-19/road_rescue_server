package com.sentinal.roadrescue.mechanic;

import com.sentinal.persistencefw.Row;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class MechanicIdentityService
{
	private static final class Holder
	{
		private static final MechanicIdentityOperations INSTANCE = TransactionProxy.create(MechanicIdentityOperations.class, new MechanicIdentityImpl());
	}

	public static MechanicIdentityOperations getMechanicIdentityImpl()
	{
		return Holder.INSTANCE;
	}

	public long getCurrentMechanicID()
	{
		return getMechanicIdentityImpl().getCurrentMechanicID();
	}

	public long getMechanicID(long userID)
	{
		return getMechanicIdentityImpl().getMechanicID(userID);
	}

	public Row getCurrentMechanic()
	{
		return getMechanicIdentityImpl().getCurrentMechanic();
	}

	public Row getMechanicByUserID(long userID)
	{
		return getMechanicIdentityImpl().getMechanicByUserID(userID);
	}

	public Row getMechanic(long mechanicID)
	{
		return getMechanicIdentityImpl().getMechanic(mechanicID);
	}
}
