package com.sentinal.roadrescue.notification;

public interface NotificationWriterOperations
{
    void create(long userID, NotificationType type, String title, String message);
}
