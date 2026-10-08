package com.sentinal.roadrescue.mechanic;

import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface MechanicAvailabilityOperations
{
	Map<String, Object> updateAvailability(String status, Double latitude, Double longitude);
}
