package com.sentinal.roadrescue.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinal.restfw.api.rest.APIHandler;
import com.sentinal.restfw.api.rest.RestAPIContext;
import com.sentinal.restfw.api.rest.RestAPIUtil;

public class ServiceRequestAPIHandler extends APIHandler
{
	@Override public Object handlePreProcess(RestAPIContext restAPIContext) throws Exception
	{
		String requestUri = restAPIContext.getRequestURI();
		int requestType = restAPIContext.getRequestType();

		if (requestUri.contains("/service-requests") && RestAPIUtil.CREATE == requestType)
		{
			String jsonString = restAPIContext.getPayload();
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(jsonString);

			String vehicleType = jsonNode.get("vehicle_type").asText();
			String serviceType = jsonNode.get("service_type").asText();
			String description = jsonNode.get("description").asText();
			double latitude = jsonNode.get("latitude").asDouble();
			double longitude = jsonNode.get("longitude").asDouble();

			return new ServiceRequestAPI().createServiceRequest(vehicleType, serviceType, description, latitude, longitude);
		}
		return null;
	}
}
