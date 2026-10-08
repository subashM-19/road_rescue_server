package com.sentinal.roadrescue.mechanic;

import java.util.List;
import java.util.Map;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.Row;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface MechanicApplicationOperations
{
	Map<String, Object> getMyApplication();

	Map<String, Object> createApplication(int experienceYears, String address, String city, String state, String postalCode);

	Map<String, Object> submitApplication();

	List<Map<String, Object>> getApplications();

	Map<String, Object> getApplication(long application_id);

	Map<String, Object> approveApplication(long applicationID);

	Map<String, Object> rejectApplication(long applicationID, String reason);

	void approveApplication(long applicationID, long adminUserID);

	void rejectApplication(long applicationID, long adminUserID, String rejectionReason);

	PersistenceObject getApplicationObjectByUserID(long userID);

	PersistenceObject getApplicationPersistenceObject(long applicationID);

	void validateBeforeSubmit(Row application);
}
