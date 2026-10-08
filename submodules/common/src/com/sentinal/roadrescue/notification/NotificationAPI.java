package com.sentinal.roadrescue.notification;

import java.util.List;
import java.util.Map;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class NotificationAPI
{
	private static final class Holder
	{
		private static final NotificationOperations INSTANCE = TransactionProxy.create(NotificationOperations.class, new NotificationImpl());
	}

	public static NotificationOperations getNotificationImpl()
	{
		return Holder.INSTANCE;
	}

	public List<Map<String, Object>> getNotifications()
	{
		return getNotificationImpl().getNotifications();
	}

	public Map<String, Object> markAsRead(long notification_id)
	{
		return getNotificationImpl().markAsRead(notification_id);
	}
}
