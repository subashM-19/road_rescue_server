package com.sentinal.roadrescue.mechanic;

import java.util.List;
import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface MechanicOperations
{
	List<Map<String, Object>> getNearbyMechanics(double latitude, double longitude, String vehicleType, String serviceType);
}
