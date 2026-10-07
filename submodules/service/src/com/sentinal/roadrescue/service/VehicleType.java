package com.sentinal.roadrescue.service;

public enum VehicleType
{
	CAR(1, "Car", "car"),
	BIKE(2, "Bike", "bike");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	VehicleType(int code, String displayName, String nodeRepresentation)
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

	public static VehicleType fromCode(int code)
	{
		for (VehicleType type : values())
		{
			if (type.code == code)
			{
				return type;
			}
		}

		throw new IllegalArgumentException("Invalid VehicleType code: " + code);
	}

	public static VehicleType fromNodeRepresentation(String nodeRepresentation)
	{
		if (nodeRepresentation == null)
		{
			throw new IllegalArgumentException("VehicleType is required");
		}

		for (VehicleType type : values())
		{
			if (type.nodeRepresentation.equalsIgnoreCase(nodeRepresentation))
			{
				return type;
			}
		}

		throw new IllegalArgumentException("Invalid VehicleType: " + nodeRepresentation);
	}
}