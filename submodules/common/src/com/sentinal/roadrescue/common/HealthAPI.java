package com.sentinal.roadrescue.common;

import java.util.LinkedHashMap;
import java.util.Map;

public class HealthAPI
{
	public Map<String, Object> health()
	{
		Map<String, Object> response = new LinkedHashMap<>();

		response.put("status", "ok");

		response.put("service", "road_rescue_server");

		return response;
	}
}