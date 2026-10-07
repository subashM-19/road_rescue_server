package com.sentinal.roadrescue.roles;

public enum UserRole
{
	CUSTOMER(1, "Customer", "customer"),

	MECHANIC_ADMIN(2, "Mechanic Admin", "mechanic_admin"),

	MECHANIC(3, "Mechanic", "mechanic"),

	PLATFORM_ADMIN(4, "Platform Admin", "platform_admin");

	private final int code;
	private final String displayName;
	private final String nodeRepresentation;

	UserRole(int code, String displayName, String nodeRepresentation)
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

	public static UserRole findByCode(int code)
	{
		for (UserRole role : values())
		{
			if (role.getCode() == code)
			{
				return role;
			}
		}

		throw new IllegalArgumentException("Unknown user role code: " + code);
	}

	public static UserRole findByNodeRepresentation(String value)
	{
		for (UserRole role : values())
		{
			if (role.getNodeRepresentation().equalsIgnoreCase(value))
			{
				return role;
			}
		}

		throw new IllegalArgumentException("Unknown user role: " + value);
	}
}