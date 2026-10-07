package com.sentinal.roadrescue.notification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.security.RoadRescueSecurityContext;

public class NotificationAPI
{
	public List<Map<String, Object>> getNotifications()
	{
		long userID = RoadRescueSecurityContext.getUserID();

		PersistenceQuery query = new PersistenceQuery("Notification").select("Notification", "NotificationID").select("Notification", "UserID").select("Notification", "NotificationType").select("Notification", "Title").select("Notification", "Message").select("Notification", "Status").select("Notification", "CreatedTime").select("Notification", "ReadTime").select("Notification", "LastModifiedTime").where(new Criteria("Notification.UserID", Criteria.EQUAL, userID));

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		List<Map<String, Object>> result = new ArrayList<>();

		for (Row notification : persistenceObject.getRows("Notification"))
		{
			result.add(buildResponse(notification));
		}

		return result;
	}

	public Map<String, Object> markAsRead(long notification_id)
	{
		long userID = RoadRescueSecurityContext.getUserID();

		PersistenceQuery query = new PersistenceQuery("Notification").select("Notification", "NotificationID").select("Notification", "UserID").select("Notification", "Status").select("Notification", "ReadTime").select("Notification", "LastModifiedTime").where(Criteria.and(new Criteria("Notification.NotificationID", Criteria.EQUAL, notification_id), new Criteria("Notification.UserID", Criteria.EQUAL, userID)));

		PersistenceObject persistenceObject = PersistenceManager.get(query);

		Row notification = persistenceObject.getRow("Notification", null);

		if (notification == null)
		{
			throw new IllegalStateException("Notification not found");
		}

		long now = System.currentTimeMillis();

		notification.set("Status", NotificationStatus.READ.getCode());

		notification.set("ReadTime", now);


		persistenceObject.updateRow(notification);

		PersistenceManager.update(persistenceObject);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("success", true);

		response.put("notification_id", notification_id);

		response.put("status", NotificationStatus.READ.getNodeRepresentation());

		response.put("status_formatted", NotificationStatus.READ.getDisplayName());

		return response;
	}

	private Map<String, Object> buildResponse(Row notification)
	{
		Map<String, Object> response = new LinkedHashMap<>();

		response.put("notification_id", notification.get("NotificationID"));

		int typeCode = ((Number) notification.get("NotificationType")).intValue();

		NotificationType type = findNotificationType(typeCode);

		response.put("type", type.getNodeRepresentation());

		response.put("type_formatted", type.getDisplayName());

		response.put("title", notification.get("Title"));

		response.put("message", notification.get("Message"));

		int statusCode = ((Number) notification.get("Status")).intValue();

		NotificationStatus status = statusCode == NotificationStatus.READ.getCode() ? NotificationStatus.READ : NotificationStatus.UNREAD;

		response.put("status", status.getNodeRepresentation());

		response.put("status_formatted", status.getDisplayName());

		response.put("created_time", notification.get("CreatedTime"));

		response.put("read_time", notification.get("ReadTime"));

		return response;
	}

	private NotificationType findNotificationType(int code)
	{
		for (NotificationType type : NotificationType.values())
		{
			if (type.getCode() == code)
			{
				return type;
			}
		}

		throw new IllegalStateException("Unknown notification type: " + code);
	}
}