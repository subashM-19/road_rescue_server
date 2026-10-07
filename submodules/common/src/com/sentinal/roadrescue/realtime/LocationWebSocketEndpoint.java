package com.sentinal.roadrescue.realtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.mechanic.MechanicIdentityService;
import com.sentinal.roadrescue.roles.UserRole;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ServerEndpoint("/ws/location") public class LocationWebSocketEndpoint
{
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final long MIN_LOCATION_INTERVAL_MS = 1_000L;

	private final TrackingService trackingService = new TrackingService();

	@OnOpen public void onOpen(Session session) throws Exception
	{
		List<String> tickets = session.getRequestParameterMap().get("ticket");

		String ticketValue = tickets == null || tickets.isEmpty() ? null : tickets.get(0);

		WebSocketTicketService.Ticket ticket = WebSocketTicketService.consume(ticketValue);

		if (ticket == null)
		{
			session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid WebSocket ticket"));

			return;
		}

		session.getUserProperties().put("user_id", ticket.getUserID());

		session.getUserProperties().put("roles", ticket.getRoles());

		session.getUserProperties().put("last_location_time", 0L);

		RealtimeHub.registerUser(ticket.getUserID(), session);

		RealtimeHub.send(session, RealtimeHub.message("type", "connected"));
	}

	@OnMessage public void onMessage(String text, Session session)
	{
		try
		{
			Map<String, Object> message = MAPPER.readValue(text, new TypeReference<Map<String, Object>>()
			{
			});

			String type = String.valueOf(message.get("type"));

			if ("subscribe_request".equals(type))
			{
				handleSubscribe(session, message);

				return;
			}

			if ("mechanic_location".equals(type))
			{
				handleMechanicLocation(session, message);

				return;
			}

			RealtimeHub.send(session, RealtimeHub.message("type", "error", "message", "Unknown WebSocket message type"));
		}
		catch (Exception exception)
		{
			exception.printStackTrace();

			RealtimeHub.send(session, RealtimeHub.message("type", "error", "message", exception.getMessage()));
		}
	}

	private void handleSubscribe(Session session, Map<String, Object> message)
	{
		long serviceRequestID = number(message.get("service_request_id")).longValue();

		long userID = ((Number) session.getUserProperties().get("user_id")).longValue();

		Set<Integer> roles = getRoles(session);

		trackingService.assertAccess(userID, roles, serviceRequestID);

		RealtimeHub.subscribeRequest(serviceRequestID, session);

		RealtimeHub.send(session, trackingService.getSnapshot(serviceRequestID));
	}

	private void handleMechanicLocation(Session session, Map<String, Object> message)
	{
		long userID = ((Number) session.getUserProperties().get("user_id")).longValue();

		Set<Integer> roles = getRoles(session);

		if (!roles.contains(UserRole.MECHANIC.getCode()))
		{
			throw new SecurityException("Mechanic permission required");
		}

		long serviceRequestID = number(message.get("service_request_id")).longValue();

		trackingService.assertAccess(userID, roles, serviceRequestID);

		long now = System.currentTimeMillis();

		long lastLocationTime = ((Number) session.getUserProperties().get("last_location_time")).longValue();

		if (now - lastLocationTime < MIN_LOCATION_INTERVAL_MS)
		{
			return;
		}

		double latitude = number(message.get("latitude")).doubleValue();

		double longitude = number(message.get("longitude")).doubleValue();

		validateLocation(latitude, longitude);

		Row mechanic = new MechanicIdentityService().getMechanicByUserID(userID);

		mechanic.set("Latitude", latitude);

		mechanic.set("Longitude", longitude);

		mechanic.set("LocationUpdatedTime", now);

		PersistenceObject update = new PersistenceObject();

		update.updateRow(mechanic);

		PersistenceManager.update(update);

		session.getUserProperties().put("last_location_time", now);

		RealtimeHub.broadcastRequest(serviceRequestID, RealtimeHub.message("type", "mechanic_location", "service_request_id", serviceRequestID, "latitude", latitude, "longitude", longitude, "time", now));
	}

	@OnClose public void onClose(Session session)
	{
		RealtimeHub.unregister(session);
	}

	@OnError public void onError(Session session, Throwable throwable)
	{
		throwable.printStackTrace();

		RealtimeHub.unregister(session);
	}

	@SuppressWarnings("unchecked") private Set<Integer> getRoles(Session session)
	{
		return (Set<Integer>) session.getUserProperties().get("roles");
	}

	private Number number(Object value)
	{
		if (value instanceof Number)
		{
			return (Number) value;
		}

		return Double.parseDouble(String.valueOf(value));
	}

	private void validateLocation(double latitude, double longitude)
	{
		if (latitude < -90 || latitude > 90)
		{
			throw new IllegalArgumentException("Invalid latitude");
		}

		if (longitude < -180 || longitude > 180)
		{
			throw new IllegalArgumentException("Invalid longitude");
		}
	}
}