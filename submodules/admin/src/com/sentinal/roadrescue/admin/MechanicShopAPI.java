package com.sentinal.roadrescue.admin;

import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class MechanicShopAPI
{
	private static final class Holder
	{
		private static final MechanicShopOperations INSTANCE = TransactionProxy.create(MechanicShopOperations.class, new MechanicShopImpl());
	}

	public static MechanicShopOperations getMechanicShopImpl()
	{
		return Holder.INSTANCE;
	}

	public List<Map<String, Object>> getMechanicShops()
	{
		return getMechanicShopImpl().getMechanicShops();
	}

	public Map<String, Object> createMechanicShop(String name, String address, double latitude, double longitude, String phone)
	{
		return getMechanicShopImpl().createMechanicShop(name, address, latitude, longitude, phone);
	}

	public Map<String, Object> getMechanicShop(long mechanicShopID)
	{
		return getMechanicShopImpl().getMechanicShop(mechanicShopID);
	}

	public Map<String, Object> openMechanicShop(long mechanicShopID)
	{
		return getMechanicShopImpl().openMechanicShop(mechanicShopID);
	}

	public Map<String, Object> closeMechanicShop(long mechanicShopID)
	{
		return getMechanicShopImpl().closeMechanicShop(mechanicShopID);
	}
}
