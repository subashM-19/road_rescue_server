package com.sentinal.roadrescue.notification;

import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.Row;

public class NotificationService
{
	public void create(long userID, NotificationType type, String title, String message)
	{
		long now = System.currentTimeMillis();

		Row notification = new Row("Notification");

		notification.set("UserID", userID);

		notification.set("NotificationType", type.getCode());

		notification.set("Title", title);

		notification.set("Message", message);

		notification.set("Status", NotificationStatus.UNREAD.getCode());

		notification.set("CreatedTime", now);

		notification.set("LastModifiedTime", now);

		PersistenceObject dataObject = new PersistenceObject();

		dataObject.addRowForInsert(notification);

		PersistenceManager.update(dataObject);
	}
}