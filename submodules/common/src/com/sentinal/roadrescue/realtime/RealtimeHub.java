package com.sentinal.roadrescue.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.Session;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RealtimeHub
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final ConcurrentHashMap<Long, Set<Session>> REQUEST_SESSIONS = new ConcurrentHashMap<>();

	private static final ConcurrentHashMap<Long, Set<Session>> USER_SESSIONS = new ConcurrentHashMap<>();

	private RealtimeHub()
	{
	}

	public static void registerUser(long userID, Session session)
	{
		USER_SESSIONS.computeIfAbsent(userID, ignored -> ConcurrentHashMap.newKeySet()).add(session);
	}

	public static void subscribeRequest(long serviceRequestID, Session session)
	{
		REQUEST_SESSIONS.computeIfAbsent(serviceRequestID, ignored -> ConcurrentHashMap.newKeySet()).add(session);
	}

	public static void unregister(Session session)
	{
		for (Set<Session> sessions : REQUEST_SESSIONS.values())
		{
			sessions.remove(session);
		}

		for (Set<Session> sessions : USER_SESSIONS.values())
		{
			sessions.remove(session);
		}
	}

	public static void broadcastRequest(long serviceRequestID, Map<String, Object> message)
	{
		Set<Session> sessions = REQUEST_SESSIONS.get(serviceRequestID);

		broadcast(sessions, message);
	}

	public static void broadcastUser(long userID, Map<String, Object> message)
	{
		Set<Session> sessions = USER_SESSIONS.get(userID);

		broadcast(sessions, message);
	}

	public static void send(Session session, Map<String, Object> message)
	{
		if (session == null || !session.isOpen())
		{
			return;
		}

		try
		{
			session.getAsyncRemote().sendText(MAPPER.writeValueAsString(message));
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
	}

	private static void broadcast(Set<Session> sessions, Map<String, Object> message)
	{
		if (sessions == null || sessions.isEmpty())
		{
			return;
		}

		for (Session session : sessions)
		{
			send(session, message);
		}
	}

	public static Map<String, Object> message(Object... values)
	{
		if (values.length == 0 || values.length % 2 != 0)
		{
			throw new IllegalArgumentException("Message requires key/value pairs");
		}

		Map<String, Object> result = new LinkedHashMap<>();

		for (int index = 0; index < values.length; index += 2)
		{
			result.put(String.valueOf(values[index]), values[index + 1]);
		}

		return result;
	}
}