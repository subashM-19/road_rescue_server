package com.sentinal.roadrescue.notification;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class NotificationService
{
	private static final class Holder
	{
		private static final NotificationWriterOperations INSTANCE = TransactionProxy.create(NotificationWriterOperations.class, new NotificationWriterImpl());
	}

	public static NotificationWriterOperations getNotificationWriterImpl()
	{
		return Holder.INSTANCE;
	}

	public void create(long userID, NotificationType type, String title, String message)
	{
		getNotificationWriterImpl().create(userID, type, title, message);
	}
}
