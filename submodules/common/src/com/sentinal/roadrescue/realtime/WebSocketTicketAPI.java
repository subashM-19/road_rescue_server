package com.sentinal.roadrescue.realtime;

import com.sentinal.roadrescue.security.RoadRescueSecurityContext;
import com.sentinal.securityfw.AuthenticatedPrincipal;

import java.util.LinkedHashMap;
import java.util.Map;

public class WebSocketTicketAPI
{
	public Map<String, Object> createTicket()
	{
		AuthenticatedPrincipal principal = RoadRescueSecurityContext.getPrincipal();

		String ticket = WebSocketTicketService.issue(principal);

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("ticket", ticket);

		response.put("expires_in_ms", 60_000);

		return response;
	}
}