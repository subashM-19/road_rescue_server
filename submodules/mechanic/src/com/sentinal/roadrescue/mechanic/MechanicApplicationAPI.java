package com.sentinal.roadrescue.mechanic;

import java.util.List;
import java.util.Map;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.Row;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class MechanicApplicationAPI
{
	private static final class Holder
	{
		private static final MechanicApplicationOperations INSTANCE = TransactionProxy.create(MechanicApplicationOperations.class, new MechanicApplicationImpl());
	}

	public static MechanicApplicationOperations getMechanicImpl()
	{
		return Holder.INSTANCE;
	}

	public Map<String, Object> getMyApplication()
	{
		return getMechanicImpl().getMyApplication();
	}

	public static Map<String, Object> createApplication(int experienceYears, String address, String city, String state, String postalCode)
	{
		return getMechanicImpl().createApplication(experienceYears, address, city, state, postalCode);
	}

	public Map<String, Object> submitApplication()
	{
		return getMechanicImpl().submitApplication();
	}

	public List<Map<String, Object>> getApplications()
	{
		return getMechanicImpl().getApplications();
	}

	public Map<String, Object> getApplication(long application_id)
	{
		return getMechanicImpl().getApplication(application_id);
	}

	public Map<String, Object> approveApplication(long applicationID)
	{
		return getMechanicImpl().approveApplication(applicationID);
	}

	public Map<String, Object> rejectApplication(long applicationID, String reason)
	{
		return getMechanicImpl().rejectApplication(applicationID, reason);
	}

	public void approveApplication(long applicationID, long adminUserID)
	{
		getMechanicImpl().approveApplication(applicationID, adminUserID);
	}

	public void rejectApplication(long applicationID, long adminUserID, String rejectionReason)
	{
		getMechanicImpl().rejectApplication(applicationID, adminUserID, rejectionReason);
	}

	public static PersistenceObject getApplicationObjectByUserID(long userID)
	{
		return getMechanicImpl().getApplicationObjectByUserID(userID);
	}

	public PersistenceObject getApplicationPersistenceObject(long applicationID)
	{
		return getMechanicImpl().getApplicationPersistenceObject(applicationID);
	}

	public static void validateBeforeSubmit(Row application)
	{
		getMechanicImpl().validateBeforeSubmit(application);
	}
}
