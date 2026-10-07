package com.sentinal.roadrescue.mechanic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.Join;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.jobs.BackgroundJobManager;
import com.sentinal.roadrescue.roles.UserRole;
import com.sentinal.roadrescue.security.RoadRescueSecurityContext;
import com.sentinal.roadrescue.security.RoadRescueUserPrincipal;

import com.sentinal.securityfw.AuthenticatedPrincipal;
import com.sentinal.securityfw.SecurityContext;
import com.sentinal.securityfw.SecurityContextHolder;

public class MechanicApplicationAPI
{

	public Map<String, Object> getMyApplication()
	{
		long userID = RoadRescueSecurityContext.getUserID();

		PersistenceQuery query = new PersistenceQuery("MechanicApplication").select("MechanicApplication", "MechanicApplicationID").select("MechanicApplication", "UserID").select("MechanicApplication", "Status").select("MechanicApplication", "ExperienceYears").select("MechanicApplication", "Address").select("MechanicApplication", "City").select("MechanicApplication", "State").select("MechanicApplication", "PostalCode").select("MechanicApplication", "SubmittedTime").select("MechanicApplication", "ReviewedTime").select("MechanicApplication", "ReviewedBy").select("MechanicApplication", "RejectionReason").select("MechanicApplication", "LastModifiedTime").where(new Criteria("MechanicApplication.UserID", Criteria.EQUAL, userID));

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		Row application = persistenceObject.getRow("MechanicApplication", null);

		Map<String, Object> response = new LinkedHashMap<>();

		if (application == null)
		{
			response.put("has_application", false);

			response.put("application", null);

			return response;
		}

		response.put("has_application", true);

		response.put("application", buildApplicationResponse(application));

		return response;
	}

	public static Map<String, Object> createApplication(int experienceYears, String address, String city, String state, String postalCode)
	{
		long userID = RoadRescueSecurityContext.getUserID();

		PersistenceObject applicationObject = getApplicationObjectByUserID(userID);

		if (applicationObject != null && !applicationObject.isEmpty())
		{
			throw new IllegalStateException("Mechanic application already exists");
		}

		Row applicationRow = new Row("MechanicApplication");
		applicationRow.set("UserID", userID);
		applicationRow.set("Status", MechanicApplicationStatus.DRAFT.getCode());
		applicationRow.set("ExperienceYears", experienceYears);
		applicationRow.set("Address", address);
		applicationRow.set("City", city);
		applicationRow.set("State", state);
		applicationRow.set("PostalCode", postalCode);

		applicationObject.addRowForInsert(applicationRow);
		PersistenceManager.update(applicationObject);
		Map<String, Object> response = new LinkedHashMap<>();

		response.put("success", true);

		response.put("application", buildApplicationResponse(applicationRow));

		return response;
	}

	public Map<String, Object> submitApplication()
	{
		long userID = RoadRescueSecurityContext.getUserID();

		PersistenceObject applicationObject = getApplicationObjectByUserID(userID);
		Row applicationRow = applicationObject.getRow("MechanicApplication", null);

		if (applicationRow == null)
		{
			throw new IllegalStateException("Mechanic application not exists");
		}

		int currentStatus = ((Number) applicationRow.get("Status")).intValue();

		if (currentStatus != MechanicApplicationStatus.DRAFT.getCode() && currentStatus != MechanicApplicationStatus.REJECTED.getCode())
		{
			throw new IllegalStateException("Application cannot be submitted");
		}

		validateBeforeSubmit(applicationRow);

		long now = System.currentTimeMillis();

		applicationRow.set("Status", MechanicApplicationStatus.PENDING.getCode());
		applicationRow.set("SubmittedTime", now);
		applicationRow.set("ReviewedTime", null);
		applicationRow.set("ReviewedBy", null);
		applicationRow.set("RejectionReason", null);
		applicationObject.updateRow(applicationRow);

		PersistenceManager.update(applicationObject);
		Map<String, Object> response = new LinkedHashMap<>();

		response.put("success", true);

		response.put("status", MechanicApplicationStatus.PENDING.getNodeRepresentation());

		response.put("status_formatted", MechanicApplicationStatus.PENDING.getDisplayName());

		return response;
	}


	public List<Map<String, Object>> getApplications()
	{
		PersistenceQuery query = new PersistenceQuery("MechanicApplication").join(Join.INNER, "Users", "MechanicApplication", "UserID", "Users", "UserID")

			.select("MechanicApplication", "MechanicApplicationID").select("MechanicApplication", "UserID").select("MechanicApplication", "Status").select("MechanicApplication", "ExperienceYears").select("MechanicApplication", "City").select("MechanicApplication", "State").select("MechanicApplication", "SubmittedTime").select("MechanicApplication", "ReviewedTime").select("MechanicApplication", "RejectionReason")

			.select("Users", "Name").select("Users", "Email").select("Users", "Phone");

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		List<Map<String, Object>> result = new ArrayList<>();

		for (Row application : persistenceObject.getRows("MechanicApplication"))
		{
			Map<String, Object> item = buildApplicationResponse(application);

			long userID = ((Number) application.get("UserID")).longValue();

			Row user = persistenceObject.getRow("Users", new Criteria("UserID", Criteria.EQUAL, userID));

			if (user != null)
			{
				item.put("name", user.get("Name"));

				item.put("email", user.get("Email"));

				item.put("phone", user.get("Phone"));
			}

			result.add(item);
		}

		return result;
	}

	public Map<String, Object> getApplication(long application_id)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicApplication").join(Join.INNER, "Users", "MechanicApplication", "UserID", "Users", "UserID").join(Join.LEFT, "MechanicDocument", "MechanicApplication", "MechanicApplicationID", "MechanicDocument", "MechanicApplicationID")

			.select("MechanicApplication", "MechanicApplicationID").select("MechanicApplication", "UserID").select("MechanicApplication", "Status").select("MechanicApplication", "ExperienceYears").select("MechanicApplication", "Address").select("MechanicApplication", "City").select("MechanicApplication", "State").select("MechanicApplication", "PostalCode").select("MechanicApplication", "SubmittedTime").select("MechanicApplication", "ReviewedTime").select("MechanicApplication", "ReviewedBy").select("MechanicApplication", "RejectionReason").select("MechanicApplication", "LastModifiedTime")

			.select("Users", "Name").select("Users", "Email").select("Users", "Phone")

			.select("MechanicDocument", "MechanicDocumentID").select("MechanicDocument", "ObjectKey").select("MechanicDocument", "OriginalFileName").select("MechanicDocument", "ContentType").select("MechanicDocument", "SizeBytes").select("MechanicDocument", "LastModifiedTime")

			.where(new Criteria("MechanicApplication.MechanicApplicationID", Criteria.EQUAL, application_id));

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		Row application = persistenceObject.getRow("MechanicApplication", null);

		if (application == null)
		{
			throw new IllegalStateException("Mechanic application not found");
		}

		Map<String, Object> response = buildApplicationResponse(application);

		Row user = persistenceObject.getRow("Users", null);

		if (user != null)
		{
			response.put("name", user.get("Name"));

			response.put("email", user.get("Email"));

			response.put("phone", user.get("Phone"));
		}

		List<Map<String, Object>> documents = new ArrayList<>();

		for (Row document : persistenceObject.getRows("MechanicDocument"))
		{
			Map<String, Object> documentData = new LinkedHashMap<>();

			documentData.put("mechanic_document_id", document.get("MechanicDocumentID"));

			documentData.put("original_file_name", document.get("OriginalFileName"));

			documentData.put("content_type", document.get("ContentType"));

			documentData.put("size_bytes", document.get("SizeBytes"));

			documentData.put("last_modified_time", document.get("LastModifiedTime"));

			documents.add(documentData);
		}

		response.put("documents", documents);

		return response;
	}

	public Map<String, Object> approveApplication(long applicationID)
	{
		long adminUserID = RoadRescueSecurityContext.getUserID();

		approveApplication(applicationID, adminUserID);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("success", true);

		response.put("status", MechanicApplicationStatus.APPROVED.getNodeRepresentation());

		response.put("status_formatted", MechanicApplicationStatus.APPROVED.getDisplayName());

		return response;
	}

	public Map<String, Object> rejectApplication(long applicationID, String reason)
	{
		long adminUserID = RoadRescueSecurityContext.getUserID();

		rejectApplication(applicationID, adminUserID, reason);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("success", true);

		response.put("status", MechanicApplicationStatus.REJECTED.getNodeRepresentation());

		response.put("status_formatted", MechanicApplicationStatus.REJECTED.getDisplayName());

		return response;
	}

	/*
	 * =====================================================
	 * RESPONSE BUILDING
	 * =====================================================
	 */

	private static Map<String, Object> buildApplicationResponse(Row application)
	{
		Map<String, Object> response = new LinkedHashMap<>();

		response.put("mechanic_application_id", application.get("MechanicApplicationID"));

		response.put("user_id", application.get("UserID"));

		int statusCode = ((Number) application.get("Status")).intValue();

		MechanicApplicationStatus status = MechanicApplicationStatus.findByCode(statusCode);

		response.put("status", status.getNodeRepresentation());

		response.put("status_formatted", status.getDisplayName());

		response.put("experience_years", application.get("ExperienceYears"));

		response.put("address", application.get("Address"));

		response.put("city", application.get("City"));

		response.put("state", application.get("State"));

		response.put("postal_code", application.get("PostalCode"));

		response.put("submitted_time", application.get("SubmittedTime"));

		response.put("reviewed_time", application.get("ReviewedTime"));

		response.put("reviewed_by", application.get("ReviewedBy"));

		response.put("rejection_reason", application.get("RejectionReason"));

		response.put("last_modified_time", application.get("LastModifiedTime"));

		return response;
	}

	public void approveApplication(long applicationID, long adminUserID)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicApplication")
			.join(Join.INNER, "Users", "MechanicApplication", "UserID", "Users", "UserID")
			.join(Join.LEFT, "Mechanic", "Users", "UserID", "Mechanic", "UserID")
			.where(new Criteria("MechanicApplication.MechanicApplicationID", Criteria.EQUAL, applicationID));

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		Row applicationRow = persistenceObject.getRow("MechanicApplication", null);

		if (applicationRow == null)
		{
			throw new IllegalStateException("Mechanic application not found");
		}

		int status = ((Number) applicationRow.get("Status")).intValue();

		if (status != MechanicApplicationStatus.PENDING.getCode())
		{
			throw new IllegalStateException("Only pending applications can be approved");
		}

		Row userRow = persistenceObject.getRow("Users", null);

		if (userRow == null)
		{
			throw new IllegalStateException("User not found");
		}

		long userID = ((Number) userRow.get("UserID")).longValue();
		Row mechanic = persistenceObject.getRow("Mechanic", null);



		/*
		 * 1. Convert userRow to MECHANIC.
		 */
		userRow.set("UserType", UserRole.MECHANIC.getCode());


		persistenceObject.updateRow(userRow);

		/*
		 * 2. Create Mechanic row only if
		 *    one doesn't already exist.
		 */
		if (mechanic == null)
		{
			mechanic = new Row("Mechanic");
			mechanic.set("UserID", userID);
			mechanic.set("AvailabilityStatus", MechanicAvailabilityStatus.OFFLINE.getCode());

			persistenceObject.addRowForInsert(mechanic);
		}

		/*
		 * 3. Approve application.
		 */
		applicationRow.set("Status", MechanicApplicationStatus.APPROVED.getCode());
		applicationRow.set("ReviewedBy", adminUserID);
		applicationRow.set("ReviewedTime", System.currentTimeMillis());
		applicationRow.set("RejectionReason", null);

		persistenceObject.updateRow(applicationRow);

		BackgroundJobManager.submit(new MechanicApplicationNotificationJob(userID, MechanicApplicationStatus.APPROVED));

		PersistenceManager.update(persistenceObject);

	}

	public void rejectApplication(long applicationID, long adminUserID, String rejectionReason)
	{
		if (rejectionReason == null || rejectionReason.trim().isEmpty())
		{
			throw new IllegalArgumentException("Rejection reason is required");
		}

		PersistenceObject applicationObject = getApplicationPersistenceObject(applicationID);
		Row applicationRow = applicationObject.getRow("MechanicApplication", null);

		if (applicationRow == null)
		{
			throw new IllegalStateException("Mechanic application not found");
		}

		long userID = ((Number) applicationRow.get("UserID")).longValue();
		int status = ((Number) applicationRow.get("Status")).intValue();

		if (status != MechanicApplicationStatus.PENDING.getCode())
		{
			throw new IllegalStateException("Only pending applications can be rejected");
		}

		long now = System.currentTimeMillis();

		applicationRow.set("Status", MechanicApplicationStatus.REJECTED.getCode());

		applicationRow.set("ReviewedBy", adminUserID);

		applicationRow.set("ReviewedTime", now);

		applicationRow.set("RejectionReason", rejectionReason.trim());

		applicationObject.updateRow(applicationRow);
		BackgroundJobManager.submit(new MechanicApplicationNotificationJob(userID, MechanicApplicationStatus.APPROVED));
		PersistenceManager.update(applicationObject);
	}

	public static PersistenceObject getApplicationObjectByUserID(long userID)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicApplication").where(new Criteria("UserID", Criteria.EQUAL, userID));

		return PersistenceManager.get(query);
	}

	public PersistenceObject getApplicationPersistenceObject(long applicationID)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicApplication").where(new Criteria("MechanicApplicationID", Criteria.EQUAL, applicationID));

		return PersistenceManager.get(query);
	}

	public static void validateBeforeSubmit(Row application)
	{
		Object experienceYears = application.get("ExperienceYears");

		if (experienceYears == null)
		{
			throw new IllegalStateException("Experience is required");
		}

		int years = ((Number) experienceYears).intValue();

		if (years < 0)
		{
			throw new IllegalStateException("Experience years cannot be negative");
		}

		String address = (String) application.get("Address");

		if (address == null || address.trim().isEmpty())
		{
			throw new IllegalStateException("Address is required");
		}

		/*
		 * Later we will also validate:
		 * - required services
		 * - required documents
		 */
	}
}