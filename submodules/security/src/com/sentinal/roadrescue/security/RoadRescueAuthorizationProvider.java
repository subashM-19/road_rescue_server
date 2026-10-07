package com.sentinal.roadrescue.security;

import com.sentinal.roadrescue.roles.UserRole;

import com.sentinal.securityfw.AuthorizationException;
import com.sentinal.securityfw.AuthorizationProvider;
import com.sentinal.securityfw.SecurityContext;

import javax.servlet.http.HttpServletRequest;

public class RoadRescueAuthorizationProvider implements AuthorizationProvider
{
	@Override public boolean isPublic(HttpServletRequest request)
	{
		String path = apiPath(request);

		return "/health".equals(path);
	}

	@Override public void authorize(SecurityContext context, HttpServletRequest request)
	{
		String path = apiPath(request);

		if ("/health".equals(path))
		{
			return;
		}

		if (path.startsWith("/admin/"))
		{
			requireRole(context, UserRole.PLATFORM_ADMIN.getCode(), "Platform admin permission required");

			return;
		}

		if ("/me".equals(path) || "/ws-ticket".equals(path) || path.startsWith("/notifications") || path.startsWith("/mechanic/application"))
		{
			return;
		}

		if (path.startsWith("/mechanic/"))
		{
			requireRole(context, UserRole.MECHANIC.getCode(), "Mechanic permission required");

			return;
		}

		if (path.startsWith("/service-requests"))
		{
			if (!context.hasRole(UserRole.CUSTOMER.getCode()) && !context.hasRole(UserRole.MECHANIC.getCode()) && !context.hasRole(UserRole.PLATFORM_ADMIN.getCode()))
			{
				throw new AuthorizationException("RoadRescue user permission required");
			}

			return;
		}

		throw new AuthorizationException("API access denied");
	}

	private void requireRole(SecurityContext context, int role, String message)
	{
		if (!context.hasRole(role))
		{
			throw new AuthorizationException(message);
		}
	}

	private String apiPath(HttpServletRequest request)
	{
		String uri = request.getRequestURI();

		int index = uri.indexOf("/api/");

		if (index >= 0)
		{
			return uri.substring(index + 4);
		}

		if (uri.endsWith("/api"))
		{
			return "/";
		}

		return uri;
	}
}