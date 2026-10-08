package com.sentinal.roadrescue.mechanic;

import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class MechanicAPI
{
	private static final class Holder
	{
		private static final MechanicOperations INSTANCE = TransactionProxy.create(MechanicOperations.class, new MechanicImpl());
	}

	public static MechanicOperations getMechanicImpl()
	{
		return Holder.INSTANCE;
	}

	public List<Map<String, Object>> getNearbyMechanics(double latitude, double longitude, String vehicleType, String serviceType)
	{
		return getMechanicImpl().getNearbyMechanics(latitude, longitude, vehicleType, serviceType);
	}
}
