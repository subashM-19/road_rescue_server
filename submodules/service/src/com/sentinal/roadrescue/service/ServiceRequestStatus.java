package com.sentinal.roadrescue.service;

public enum ServiceRequestStatus
{
	PENDING(1, "Pending", "pending"),
	ASSIGNED(2, "Assigned", "assigned"),
	ACCEPTED(3, "Accepted", "accepted"),
	ON_THE_WAY(4, "On The Way", "on_the_way"),
	IN_PROGRESS(5, "In Progress", "in_progress"),
	COMPLETED(6, "Completed", "completed"),
	CANCELLED(7, "Cancelled", "cancelled");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	ServiceRequestStatus(int code, String displayName, String nodeRepresentation)
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

	public static ServiceRequestStatus fromCode(int code)
	{
		for (ServiceRequestStatus status : values())
		{
			if (status.code == code)
			{
				return status;
			}
		}

		throw new IllegalArgumentException("Invalid ServiceRequestStatus code: " + code);
	}

	public static ServiceRequestStatus fromNodeRepresentation(String nodeRepresentation)
	{
		if (nodeRepresentation == null)
		{
			throw new IllegalArgumentException("ServiceRequestStatus is required");
		}

		for (ServiceRequestStatus status : values())
		{
			if (status.nodeRepresentation.equalsIgnoreCase(nodeRepresentation))
			{
				return status;
			}
		}

		throw new IllegalArgumentException("Invalid ServiceRequestStatus: " + nodeRepresentation);
	}
}