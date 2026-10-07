package com.sentinal.roadrescue.realtime;

import com.sentinal.securityfw.AuthenticatedPrincipal;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WebSocketTicketService
{
	private static final long TICKET_LIFETIME_MS = 60_000L;

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final ConcurrentHashMap<String, Ticket> TICKETS = new ConcurrentHashMap<>();

	private WebSocketTicketService()
	{
	}

	public static String issue(AuthenticatedPrincipal principal)
	{
		cleanup();

		byte[] bytes = new byte[32];

		RANDOM.nextBytes(bytes);

		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		Ticket ticket = new Ticket(principal.getPrincipalID(), principal.getPrincipalType(), principal.getRoles(), System.currentTimeMillis() + TICKET_LIFETIME_MS);

		TICKETS.put(value, ticket);

		return value;
	}

	public static Ticket consume(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return null;
		}

		Ticket ticket = TICKETS.remove(value);

		if (ticket == null)
		{
			return null;
		}

		if (ticket.expiresTime < System.currentTimeMillis())
		{
			return null;
		}

		return ticket;
	}

	private static void cleanup()
	{
		long now = System.currentTimeMillis();

		TICKETS.entrySet().removeIf(entry -> entry.getValue().expiresTime < now);
	}

	public static final class Ticket
	{
		private final long userID;

		private final String principalType;

		private final Set<Integer> roles;

		private final long expiresTime;

		private Ticket(long userID, String principalType, Set<Integer> roles, long expiresTime)
		{
			this.userID = userID;

			this.principalType = principalType;

			this.roles = new HashSet<>(roles);

			this.expiresTime = expiresTime;
		}

		public long getUserID()
		{
			return userID;
		}

		public String getPrincipalType()
		{
			return principalType;
		}

		public Set<Integer> getRoles()
		{
			return new HashSet<>(roles);
		}

		public boolean hasRole(int role)
		{
			return roles.contains(role);
		}
	}
}