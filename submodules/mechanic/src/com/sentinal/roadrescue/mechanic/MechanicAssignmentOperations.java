package com.sentinal.roadrescue.mechanic;

import java.util.List;
import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface MechanicAssignmentOperations
{
	List<Map<String, Object>> getMyAssignments();

	Map<String, Object> acceptAssignment(long mechanicAssignmentID);

	Map<String, Object> rejectAssignment(long mechanicAssignmentID);

	Map<String, Object> startJourney(long mechanicAssignmentID);

	Map<String, Object> startService(long mechanicAssignmentID);

	Map<String, Object> completeAssignment(long mechanicAssignmentID);
}
