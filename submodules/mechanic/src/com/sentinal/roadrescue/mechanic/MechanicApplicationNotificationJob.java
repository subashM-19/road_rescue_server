package com.sentinal.roadrescue.mechanic;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;
import com.sentinal.roadrescue.jobs.BackgroundJob;
import com.sentinal.roadrescue.notification.EmailManager;
import com.sentinal.roadrescue.notification.NotificationService;
import com.sentinal.roadrescue.notification.NotificationWriterOperations;
import com.sentinal.roadrescue.notification.NotificationType;

public class MechanicApplicationNotificationJob implements BackgroundJob
{

	private final long userID;
	private final MechanicApplicationStatus action;
	private final String rejectionReason;

	public MechanicApplicationNotificationJob(long userID, MechanicApplicationStatus action)
	{
		this(userID, action, null);
	}

	public MechanicApplicationNotificationJob(long userID, MechanicApplicationStatus action, String rejectionReason)
	{
		if (action == null)
		{
			throw new IllegalArgumentException("Action cannot be null");
		}

		this.userID = userID;

		this.action = action;

		this.rejectionReason = rejectionReason;
	}

	@Override public String getName()
	{
		return "MechanicApplicationNotificationJob." + action.name();
	}

	@Override public void execute() throws Exception
	{
		PersistenceQuery query = new PersistenceQuery("Users").select("Users", "UserID").select("Users", "Name").select("Users", "Email").where(new Criteria("Users.UserID", Criteria.EQUAL, userID));

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		Row user = persistenceObject.getRow("Users", null);

		if (user == null)
		{
			throw new IllegalStateException("User not found: " + userID);
		}

		String name = (String) user.get("Name");

		String email = (String) user.get("Email");

		if (action == MechanicApplicationStatus.APPROVED)
		{
			processApproval(name, email);

			return;
		}

		processRejection(name, email);
	}

	private void processApproval(String name, String email) throws Exception
	{
		NotificationWriterOperations notificationService = NotificationService.getNotificationWriterImpl();

		notificationService.create(userID, NotificationType.MECHANIC_APPLICATION_APPROVED, "Mechanic application approved", "Your RoadRescue mechanic application has been approved.");

		if (email == null || email.trim().isEmpty())
		{
			return;
		}

		String body = "Hi " + getDisplayName(name) + ",\n\n" + "Your RoadRescue mechanic application " + "has been approved.\n\n" + "You can now open the RoadRescue " + "Mechanic app and start accepting " + "service requests.\n\n" + "Regards,\n" + "RoadRescue";

		EmailManager.getProvider().send(email, "RoadRescue mechanic application approved", body);
	}

	private void processRejection(String name, String email) throws Exception
	{
		String message = "Your RoadRescue mechanic application " + "was not approved.";

		if (rejectionReason != null && !rejectionReason.trim().isEmpty())
		{
			message += " Reason: " + rejectionReason.trim();
		}

		NotificationWriterOperations notificationService = NotificationService.getNotificationWriterImpl();

		notificationService.create(userID, NotificationType.MECHANIC_APPLICATION_REJECTED, "Mechanic application update", message);

		if (email == null || email.trim().isEmpty())
		{
			return;
		}

		String body = "Hi " + getDisplayName(name) + ",\n\n" + message + "\n\n" + "You can update your application " + "and submit it again.\n\n" + "Regards,\n" + "RoadRescue";

		EmailManager.getProvider().send(email, "RoadRescue mechanic application update", body);
	}

	private String getDisplayName(String name)
	{
		if (name == null || name.trim().isEmpty())
		{
			return "there";
		}

		return name.trim();
	}
}