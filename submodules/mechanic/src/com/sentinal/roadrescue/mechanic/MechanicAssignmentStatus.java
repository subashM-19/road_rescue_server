package com.sentinal.roadrescue.mechanic;

public enum MechanicAssignmentStatus
{
	PENDING(1, "Pending", "pending"),
	ACCEPTED(2, "Accepted", "accepted"),
	REJECTED(3, "Rejected", "rejected"),
	COMPLETED(4, "Completed", "completed"),
	CANCELLED(5, "Cancelled", "cancelled");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	MechanicAssignmentStatus(int code, String displayName, String nodeRepresentation)
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

	public static MechanicAssignmentStatus findByCode(int code)
	{
		for (MechanicAssignmentStatus status : MechanicAssignmentStatus.values())
		{
			if (status.getCode() == code)
			{
				return status;
			}
		}

		throw new IllegalArgumentException("Invalid mechanic assignment status: " + code);
	}
}