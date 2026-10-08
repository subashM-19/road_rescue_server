package com.sentinal.roadrescue.mechanic;

import java.util.List;
import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface MechanicShopManagementOperations
{
	Map<String, Object> getMyShop(long mechanicID);

	Map<String, Object> createShop(long mechanicID, String name, String address, double latitude, double longitude, String phone);

	List<Map<String, Object>> getShopMechanics(long mechanicID);

	Map<String, Object> makeShopMechanicAvailable(long mechanicID, long targetMechanicID);

	Map<String, Object> makeShopMechanicOffline(long mechanicID, long targetMechanicID);
}
