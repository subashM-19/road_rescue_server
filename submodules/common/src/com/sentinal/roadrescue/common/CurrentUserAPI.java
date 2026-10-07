package com.sentinal.roadrescue.common;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.mechanic.MechanicApplicationStatus;
import com.sentinal.roadrescue.mechanic.MechanicAvailabilityStatus;
import com.sentinal.roadrescue.roles.UserRole;
import com.sentinal.roadrescue.security.RoadRescueSecurityContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class CurrentUserAPI
{
	public Map<String, Object> getCurrentUser()
	{
		long userID = RoadRescueSecurityContext.getUserID();

		Row user = getUser(userID);

		int roleCode = ((Number) user.get("UserType")).intValue();

		UserRole role = UserRole.findByCode(roleCode);

		Row mechanic = getMechanic(userID);

		Row application = getMechanicApplication(userID);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("user_id", userID);

		response.put("name", user.get("Name"));

		response.put("email", user.get("Email"));

		response.put("role", role.getNodeRepresentation());

		response.put("role_formatted", role.getDisplayName());

		if (mechanic != null)
		{
			MechanicAvailabilityStatus mechanicAvailabilityStatus = MechanicAvailabilityStatus.fromCode(((Number) mechanic.get("AvailabilityStatus")).intValue());
			response.put("mechanic_id", ((Number) mechanic.get("MechanicID")).longValue());
			response.put("availability_status", mechanicAvailabilityStatus.getNodeRepresentation());
			response.put("availability_status_formatted", mechanicAvailabilityStatus.getDisplayName());
		}
		else
		{
			response.put("mechanic_id", null);
		}

		if (application != null)
		{
			int applicationStatusCode = ((Number) application.get("Status")).intValue();

			MechanicApplicationStatus applicationStatus = MechanicApplicationStatus.findByCode(applicationStatusCode);

			response.put("mechanic_application_status", applicationStatus.getNodeRepresentation());

			response.put("mechanic_application_status_formatted", applicationStatus.getDisplayName());
		}
		else
		{
			response.put("mechanic_application_status", null);

			response.put("mechanic_application_status_formatted", null);
		}

		return response;
	}

	private Row getUser(long userID)
	{
		PersistenceQuery query = new PersistenceQuery("Users").where(new Criteria("Users.UserID", Criteria.EQUAL, userID));

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		return persistenceObject.getRows("Users").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("User not found: " + userID));
	}

	private Row getMechanic(long userID)
	{
		PersistenceQuery query = new PersistenceQuery("Mechanic").where(new Criteria("Mechanic.UserID", Criteria.EQUAL, userID));

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		return persistenceObject.getRows("Mechanic").stream().findFirst().orElse(null);
	}

	private Row getMechanicApplication(long userID)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicApplication").where(new Criteria("MechanicApplication.UserID", Criteria.EQUAL, userID));

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		return persistenceObject.getRows("MechanicApplication").stream().findFirst().orElse(null);
	}
}