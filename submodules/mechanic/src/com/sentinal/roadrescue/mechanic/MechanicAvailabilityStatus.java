package com.sentinal.roadrescue.mechanic;

public enum MechanicAvailabilityStatus
{
	AVAILABLE(1, "Available", "available"),
	BUSY(2, "Busy", "busy"),
	OFFLINE(3, "Offline", "offline"),
	RESERVED(4, "Reserved", "reserved");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	MechanicAvailabilityStatus(int code, String displayName, String nodeRepresentation)
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

	public static MechanicAvailabilityStatus fromCode(int code)
	{
		for (MechanicAvailabilityStatus status : values())
		{
			if (status.code == code)
			{
				return status;
			}
		}

		throw new IllegalArgumentException("Invalid MechanicAvailabilityStatus code: " + code);
	}

	public static MechanicAvailabilityStatus fromNodeRepresentation(String nodeRepresentation)
	{
		if (nodeRepresentation == null)
		{
			throw new IllegalArgumentException("MechanicAvailabilityStatus is required");
		}

		for (MechanicAvailabilityStatus status : values())
		{
			if (status.nodeRepresentation.equalsIgnoreCase(nodeRepresentation))
			{
				return status;
			}
		}

		throw new IllegalArgumentException("Invalid MechanicAvailabilityStatus: " + nodeRepresentation);
	}
}