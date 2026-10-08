package com.sentinal.roadrescue.admin;

import java.util.List;
import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface MechanicAdminOperations
{
	List<Map<String, Object>> getMechanics();

	Map<String, Object> makeAvailable(long mechanicID);

	Map<String, Object> makeOffline(long mechanicID);
}
