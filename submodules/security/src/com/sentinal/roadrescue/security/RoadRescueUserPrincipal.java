package com.sentinal.roadrescue.security;

public class RoadRescueUserPrincipal
{
	private final long userID;
	private final Integer role;

	public RoadRescueUserPrincipal(long userID, Integer role)
	{
		this.userID = userID;
		this.role = role;
	}

	public long getUserID()
	{
		return userID;
	}

	public Integer getRole()
	{
		return role;
	}
}