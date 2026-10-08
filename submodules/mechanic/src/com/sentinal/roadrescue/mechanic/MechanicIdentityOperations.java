package com.sentinal.roadrescue.mechanic;

import com.sentinal.persistencefw.Row;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface MechanicIdentityOperations
{
	long getCurrentMechanicID();

	long getMechanicID(long userID);

	Row getCurrentMechanic();

	Row getMechanicByUserID(long userID);

	Row getMechanic(long mechanicID);
}
