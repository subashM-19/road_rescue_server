package com.sentinal.roadrescue.admin;

import java.util.List;
import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface MechanicShopOperations
{
	List<Map<String, Object>> getMechanicShops();

	Map<String, Object> createMechanicShop(String name, String address, double latitude, double longitude, String phone);

	Map<String, Object> getMechanicShop(long mechanicShopID);

	Map<String, Object> openMechanicShop(long mechanicShopID);

	Map<String, Object> closeMechanicShop(long mechanicShopID);
}
