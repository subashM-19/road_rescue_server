package com.sentinal.roadrescue.mechanic;

public enum MechanicApplicationStatus
{
	DRAFT(1, "Draft", "draft"),

	PENDING(2, "Pending", "pending"),

	APPROVED(3, "Approved", "approved"),

	REJECTED(4, "Rejected", "rejected");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	MechanicApplicationStatus(int code, String displayName, String nodeRepresentation)
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

	public static MechanicApplicationStatus findByCode(int code)
	{
		for (MechanicApplicationStatus status : values())
		{
			if (status.code == code)
			{
				return status;
			}
		}

		throw new IllegalArgumentException("Unknown mechanic application status: " + code);
	}
}