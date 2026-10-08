package com.sentinal.roadrescue.mechanic;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.security.RoadRescueSecurityContext;

public class MechanicIdentityImpl implements MechanicIdentityOperations
{
	@Override
	public long getCurrentMechanicID()
	{
		return getMechanicID(RoadRescueSecurityContext.getUserID());
	}

	@Override
	public long getMechanicID(long userID)
	{
		Row mechanic = getMechanicByUserID(userID);

		return ((Number) mechanic.get("MechanicID")).longValue();
	}

	@Override
	public Row getCurrentMechanic()
	{
		return getMechanicByUserID(RoadRescueSecurityContext.getUserID());
	}

	@Override
	public Row getMechanicByUserID(long userID)
	{
		PersistenceQuery query = new PersistenceQuery("Mechanic").where(new Criteria("Mechanic.UserID", Criteria.EQUAL, userID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("Mechanic").stream().findFirst().orElseThrow(() -> new IllegalStateException("Mechanic account not found"));
	}

	@Override
	public Row getMechanic(long mechanicID)
	{
		PersistenceQuery query = new PersistenceQuery("Mechanic").where(new Criteria("Mechanic.MechanicID", Criteria.EQUAL, mechanicID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("Mechanic").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Mechanic not found: " + mechanicID));
	}
}