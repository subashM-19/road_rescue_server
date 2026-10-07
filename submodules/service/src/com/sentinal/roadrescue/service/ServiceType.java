package com.sentinal.roadrescue.service;

public enum ServiceType
{
	BREAKDOWN(1, "Breakdown", "breakdown"),
	FLAT_TYRE(2, "Flat Tyre", "flat_tyre"),
	BATTERY_JUMP_START(3, "Battery Jump Start", "battery_jump_start"),
	TOWING(4, "Towing", "towing"),
	FUEL_ASSISTANCE(5, "Fuel Assistance", "fuel_assistance"),
	OTHER(6, "Other", "other");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	ServiceType(int code, String displayName, String nodeRepresentation)
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

	public static ServiceType fromCode(int code)
	{
		for (ServiceType type : values())
		{
			if (type.code == code)
			{
				return type;
			}
		}

		throw new IllegalArgumentException("Invalid ServiceType code: " + code);
	}

	public static ServiceType fromNodeRepresentation(String nodeRepresentation)
	{
		if (nodeRepresentation == null)
		{
			throw new IllegalArgumentException("ServiceType is required");
		}

		for (ServiceType type : values())
		{
			if (type.nodeRepresentation.equalsIgnoreCase(nodeRepresentation))
			{
				return type;
			}
		}

		throw new IllegalArgumentException("Invalid ServiceType: " + nodeRepresentation);
	}
}