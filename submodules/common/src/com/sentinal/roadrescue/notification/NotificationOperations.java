package com.sentinal.roadrescue.notification;

import java.util.List;
import java.util.Map;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface NotificationOperations
{
	List<Map<String, Object>> getNotifications();

	Map<String, Object> markAsRead(long notification_id);
}
