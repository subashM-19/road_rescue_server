package com.sentinal.roadrescue.mechanic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinal.restfw.api.rest.APIHandler;
import com.sentinal.restfw.api.rest.RestAPIContext;
import com.sentinal.restfw.api.rest.RestAPIUtil;

public class MechanicAPIHandler extends APIHandler
{
	@Override public Object handlePreProcess(RestAPIContext restAPIContext) throws Exception
	{
		String requestUri = restAPIContext.getRequestURI();
		int requestType = restAPIContext.getRequestType();

		if (requestUri.contains("mechanic/application/create") && RestAPIUtil.CREATE == requestType)
		{
			String jsonString = restAPIContext.getPayload();
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(jsonString);

			int years = jsonNode.get("experience_years").asInt();
			String address = jsonNode.get("address").toString();
			String city = jsonNode.get("city").toString();
			String state = jsonNode.get("state").toString();
			String postalCode = jsonNode.get("postal_code").toString();

			return MechanicApplicationAPI.createApplication(years, address, city, state, postalCode);
		}
		else if (requestUri.contains("/mechanic/availability"))
		{
			String jsonString = restAPIContext.getPayload();
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(jsonString);

			String status = jsonNode.get("status").asText();

			Double latitude = null;
			Double longitude = null;

			if (jsonNode.has("latitude") && !jsonNode.get("latitude").isNull())
			{
				latitude = jsonNode.get("latitude").asDouble();
			}

			if (jsonNode.has("longitude") && !jsonNode.get("longitude").isNull())
			{
				longitude = jsonNode.get("longitude").asDouble();
			}

			return new MechanicAvailabilityAPI().updateAvailability(status, latitude, longitude);
		}
		else if (requestUri.contains("/admin/mechanic-applications") && requestUri.contains("reject"))
		{
			String jsonString = restAPIContext.getPayload();
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(jsonString);

			String reason = jsonNode.get("rejection_reason").toString();
			long applicationID = (long) restAPIContext.getArguments()[0];
			return new MechanicApplicationAPI().rejectApplication(applicationID, reason);
		}
		return null;
	}
}
