package com.sentinal.roadrescue.security;

import com.sentinal.securityfw.AuthenticatedPrincipal;
import com.sentinal.securityfw.SecurityContext;
import com.sentinal.securityfw.SecurityContextHolder;

public final class RoadRescueSecurityContext
{
	private RoadRescueSecurityContext()
	{
	}

	public static AuthenticatedPrincipal getPrincipal()
	{
		SecurityContext context = SecurityContextHolder.get();

		if (context == null)
		{
			throw new IllegalStateException("Security context not available");
		}

		AuthenticatedPrincipal principal = context.getPrincipal();

		if (principal == null)
		{
			throw new IllegalStateException("Authenticated principal not available");
		}

		return principal;
	}

	public static long getUserID()
	{
		return getPrincipal().getPrincipalID();
	}
}