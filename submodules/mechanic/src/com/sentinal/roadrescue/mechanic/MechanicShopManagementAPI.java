package com.sentinal.roadrescue.mechanic;

import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class MechanicShopManagementAPI
{
	private static final class Holder
	{
		private static final MechanicShopManagementOperations INSTANCE = TransactionProxy.create(MechanicShopManagementOperations.class, new MechanicShopManagementImpl());
	}

	public static MechanicShopManagementOperations getMechanicShopManagementImpl()
	{
		return Holder.INSTANCE;
	}

	public Map<String, Object> getMyShop(long mechanicID)
	{
		return getMechanicShopManagementImpl().getMyShop(mechanicID);
	}

	public Map<String, Object> createShop(long mechanicID, String name, String address, double latitude, double longitude, String phone)
	{
		return getMechanicShopManagementImpl().createShop(mechanicID, name, address, latitude, longitude, phone);
	}

	public List<Map<String, Object>> getShopMechanics(long mechanicID)
	{
		return getMechanicShopManagementImpl().getShopMechanics(mechanicID);
	}

	public Map<String, Object> makeShopMechanicAvailable(long mechanicID, long targetMechanicID)
	{
		return getMechanicShopManagementImpl().makeShopMechanicAvailable(mechanicID, targetMechanicID);
	}

	public Map<String, Object> makeShopMechanicOffline(long mechanicID, long targetMechanicID)
	{
		return getMechanicShopManagementImpl().makeShopMechanicOffline(mechanicID, targetMechanicID);
	}
}
