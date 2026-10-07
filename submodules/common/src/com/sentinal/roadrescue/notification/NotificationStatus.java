package com.sentinal.roadrescue.notification;

public enum NotificationStatus
{
	UNREAD(1, "Unread", "unread"),
	READ(2, "Read", "read");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	NotificationStatus(int code, String displayName, String nodeRepresentation)
	{
		this.code = code;
		this.displayName = displayName;
		this.nodeRepresentation = nodeRepresentation;
	}

	public int getCode()
	{
		return code;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getNodeRepresentation()
	{
		return nodeRepresentation;
	}
}