package com.sentinal.roadrescue.mechanic;

import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class MechanicAvailabilityAPI
{
	private static final class Holder
	{
		private static final MechanicAvailabilityOperations INSTANCE = TransactionProxy.create(MechanicAvailabilityOperations.class, new MechanicAvailabilityImpl());
	}

	public static MechanicAvailabilityOperations getMechanicAvailabilityImpl()
	{
		return Holder.INSTANCE;
	}

	public Map<String, Object> updateAvailability(String status, Double latitude, Double longitude)
	{
		return getMechanicAvailabilityImpl().updateAvailability(status, latitude, longitude);
	}
}
