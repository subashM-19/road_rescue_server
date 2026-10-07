package com.sentinal.roadrescue.mechanic;

public enum MechanicShopStatus
{
	OPENED(1, "Opened", "open"),
	CLOSED(2, "Closed", "close");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	MechanicShopStatus(int code, String displayName, String nodeRepresentation)
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

	public static MechanicShopStatus fromCode(int code)
	{
		for (MechanicShopStatus status : values())
		{
			if (status.code == code)
			{
				return status;
			}
		}

		throw new IllegalArgumentException("Invalid MechanicShopStatus code: " + code);
	}

	public static MechanicShopStatus fromNodeRepresentation(String nodeRepresentation)
	{
		if (nodeRepresentation == null)
		{
			throw new IllegalArgumentException("MechanicShopStatus is required");
		}

		for (MechanicShopStatus status : values())
		{
			if (status.nodeRepresentation.equalsIgnoreCase(nodeRepresentation))
			{
				return status;
			}
		}

		throw new IllegalArgumentException("Invalid MechanicShopStatus: " + nodeRepresentation);
	}
}