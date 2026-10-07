package com.sentinal.roadrescue.notification;

public enum NotificationType
{
	MECHANIC_APPLICATION_APPROVED(1, "Mechanic Application Approved", "mechanic_application_approved"),

	MECHANIC_APPLICATION_REJECTED(2, "Mechanic Application Rejected", "mechanic_application_rejected"),

	NEW_ASSIGNMENT(3, "New Assignment", "new_assignment"),

	REQUEST_ACCEPTED(4, "Request Accepted", "request_accepted"),

	REQUEST_REJECTED(5, "Request Rejected", "request_rejected"),

	MECHANIC_ON_THE_WAY(6, "Mechanic On The Way", "mechanic_on_the_way"),

	SERVICE_STARTED(7, "Service Started", "service_started"),

	SERVICE_COMPLETED(8, "Service Completed", "service_completed");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	NotificationType(int code, String displayName, String nodeRepresentation)
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