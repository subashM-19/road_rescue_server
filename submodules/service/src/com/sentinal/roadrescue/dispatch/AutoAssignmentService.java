package com.sentinal.roadrescue.dispatch;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.mechanic.MechanicAssignmentStatus;
import com.sentinal.roadrescue.mechanic.MechanicAvailabilityStatus;
import com.sentinal.roadrescue.realtime.RealtimeHub;
import com.sentinal.roadrescue.service.ServiceRequestStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoAssignmentService
{
	private static final Object ASSIGNMENT_LOCK = new Object();

	private static final long OFFER_TIMEOUT_MS = readLongEnvironment("ROAD_RESCUE_ASSIGNMENT_TIMEOUT_MS", 120_000L);

	private static final double MAX_RADIUS_KM = readDoubleEnvironment("ROAD_RESCUE_ASSIGNMENT_RADIUS_KM", 25.0);

	public Long autoAssign(long serviceRequestID)
	{
		synchronized (ASSIGNMENT_LOCK)
		{
			Row request = getServiceRequest(serviceRequestID);

			int requestStatus = ((Number) request.get("Status")).intValue();

			if (requestStatus != ServiceRequestStatus.PENDING.getCode())
			{
				return null;
			}

			int vehicleType = ((Number) request.get("VehicleType")).intValue();

			int serviceType = ((Number) request.get("ServiceType")).intValue();

			double requestLatitude = ((Number) request.get("Latitude")).doubleValue();

			double requestLongitude = ((Number) request.get("Longitude")).doubleValue();

			Set<Long> excludedMechanics = getPreviouslyOfferedMechanics(serviceRequestID);

			List<Long> capableMechanics = findCapableMechanics(vehicleType, serviceType);

			Row nearestMechanic = null;

			double nearestDistance = Double.MAX_VALUE;

			for (Long mechanicID : capableMechanics)
			{
				if (excludedMechanics.contains(mechanicID))
				{
					continue;
				}

				Row mechanic = getMechanicOrNull(mechanicID);

				if (mechanic == null)
				{
					continue;
				}

				int availability = ((Number) mechanic.get("AvailabilityStatus")).intValue();

				if (availability != MechanicAvailabilityStatus.AVAILABLE.getCode())
				{
					continue;
				}

				Object latitudeValue = mechanic.get("Latitude");

				Object longitudeValue = mechanic.get("Longitude");

				if (latitudeValue == null || longitudeValue == null)
				{
					continue;
				}

				double latitude = ((Number) latitudeValue).doubleValue();

				double longitude = ((Number) longitudeValue).doubleValue();

				double distance = calculateDistanceKM(requestLatitude, requestLongitude, latitude, longitude);

				if (distance > MAX_RADIUS_KM)
				{
					continue;
				}

				if (distance < nearestDistance)
				{
					nearestDistance = distance;

					nearestMechanic = mechanic;
				}
			}

			if (nearestMechanic == null)
			{
				return null;
			}

			long mechanicID = ((Number) nearestMechanic.get("MechanicID")).longValue();

			long mechanicUserID = ((Number) nearestMechanic.get("UserID")).longValue();

			long now = System.currentTimeMillis();

			Row assignment = new Row("MechanicAssignment");

			assignment.set("ServiceRequestID", serviceRequestID);

			assignment.set("MechanicID", mechanicID);

			assignment.set("Status", MechanicAssignmentStatus.PENDING.getCode());

			assignment.set("AssignedTime", now);

			assignment.set("ExpiresTime", now + OFFER_TIMEOUT_MS);

			request.set("Status", ServiceRequestStatus.ASSIGNED.getCode());

			nearestMechanic.set("AvailabilityStatus", MechanicAvailabilityStatus.RESERVED.getCode());

			PersistenceObject dataObject = new PersistenceObject();

			dataObject.updateRow(request);

			dataObject.updateRow(nearestMechanic);

			dataObject.addRowForInsert(assignment);

			PersistenceManager.update(dataObject);

			long assignmentID = ((Number) assignment.get("MechanicAssignmentID")).longValue();

			RealtimeHub.broadcastUser(mechanicUserID, RealtimeHub.message("type", "assignment_offer", "mechanic_assignment_id", assignmentID, "service_request_id", serviceRequestID, "expires_time", now + OFFER_TIMEOUT_MS));

			return assignmentID;
		}
	}

	public void expirePendingAssignments()
	{
		PersistenceQuery query = new PersistenceQuery("MechanicAssignment").where(new Criteria("MechanicAssignment.Status", Criteria.EQUAL, MechanicAssignmentStatus.PENDING.getCode()));

		PersistenceObject dataObject = PersistenceManager.get(query);

		long now = System.currentTimeMillis();

		List<Long> requestsToReassign = new ArrayList<>();

		for (Row assignment : dataObject.getRows("MechanicAssignment"))
		{
			Object expiresValue = assignment.get("ExpiresTime");

			if (expiresValue == null)
			{
				continue;
			}

			long expiresTime = ((Number) expiresValue).longValue();

			if (expiresTime > now)
			{
				continue;
			}

			long serviceRequestID = ((Number) assignment.get("ServiceRequestID")).longValue();

			long mechanicID = ((Number) assignment.get("MechanicID")).longValue();

			Row request = getServiceRequest(serviceRequestID);

			Row mechanic = getMechanicOrNull(mechanicID);

			int currentRequestStatus = ((Number) request.get("Status")).intValue();

			if (currentRequestStatus != ServiceRequestStatus.ASSIGNED.getCode())
			{
				continue;
			}

			assignment.set("Status", MechanicAssignmentStatus.CANCELLED.getCode());

			request.set("Status", ServiceRequestStatus.PENDING.getCode());

			PersistenceObject update = new PersistenceObject();

			update.updateRow(assignment);

			update.updateRow(request);

			if (mechanic != null)
			{
				int availability = ((Number) mechanic.get("AvailabilityStatus")).intValue();

				if (availability == MechanicAvailabilityStatus.RESERVED.getCode())
				{
					mechanic.set("AvailabilityStatus", MechanicAvailabilityStatus.AVAILABLE.getCode());

					update.updateRow(mechanic);
				}
			}

			PersistenceManager.update(update);

			requestsToReassign.add(serviceRequestID);
		}

		for (Long requestID : requestsToReassign)
		{
			autoAssign(requestID);
		}
	}

	private List<Long> findCapableMechanics(int vehicleType, int serviceType)
	{
		PersistenceQuery query = new PersistenceQuery("Mechanic").where(new Criteria("Mechanic.AvailabilityStatus", Criteria.EQUAL, MechanicAvailabilityStatus.AVAILABLE.getCode()));

		PersistenceObject dataObject = PersistenceManager.get(query);

		List<Long> mechanics = new ArrayList<>();

		for (Row row : dataObject.getRows("Mechanic"))
		{
			mechanics.add(((Number) row.get("MechanicID")).longValue());
		}

		return mechanics;
	}

	private Set<Long> getPreviouslyOfferedMechanics(long serviceRequestID)
	{
		PersistenceQuery query = new PersistenceQuery("MechanicAssignment").where(new Criteria("MechanicAssignment.ServiceRequestID", Criteria.EQUAL, serviceRequestID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		Set<Long> result = new HashSet<>();

		for (Row row : dataObject.getRows("MechanicAssignment"))
		{
			result.add(((Number) row.get("MechanicID")).longValue());
		}

		return result;
	}

	private Row getServiceRequest(long serviceRequestID)
	{
		PersistenceQuery query = new PersistenceQuery("ServiceRequest").where(new Criteria("ServiceRequest.ServiceRequestID", Criteria.EQUAL, serviceRequestID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("ServiceRequest").stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Service request not found: " + serviceRequestID));
	}

	private Row getMechanicOrNull(long mechanicID)
	{
		PersistenceQuery query = new PersistenceQuery("Mechanic").where(new Criteria("Mechanic.MechanicID", Criteria.EQUAL, mechanicID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("Mechanic").stream().findFirst().orElse(null);
	}

	private double calculateDistanceKM(double latitude1, double longitude1, double latitude2, double longitude2)
	{
		final double earthRadiusKM = 6371.0088;

		double latitudeDistance = Math.toRadians(latitude2 - latitude1);

		double longitudeDistance = Math.toRadians(longitude2 - longitude1);

		double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2) + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return earthRadiusKM * c;
	}

	private static long readLongEnvironment(String name, long defaultValue)
	{
		try
		{
			String value = System.getenv(name);

			if (value == null || value.trim().isEmpty())
			{
				return defaultValue;
			}

			return Long.parseLong(value.trim());
		}
		catch (Exception ignored)
		{
			return defaultValue;
		}
	}

	private static double readDoubleEnvironment(String name, double defaultValue)
	{
		try
		{
			String value = System.getenv(name);

			if (value == null || value.trim().isEmpty())
			{
				return defaultValue;
			}

			return Double.parseDouble(value.trim());
		}
		catch (Exception ignored)
		{
			return defaultValue;
		}
	}

	public void assignPendingRequests()
	{
		PersistenceQuery query = new PersistenceQuery("ServiceRequest").where(new Criteria("ServiceRequest.Status", Criteria.EQUAL, ServiceRequestStatus.PENDING.getCode()));

		PersistenceObject dataObject = PersistenceManager.get(query);

		for (Row request : dataObject.getRows("ServiceRequest"))
		{
			long serviceRequestID = ((Number) request.get("ServiceRequestID")).longValue();

			try
			{
				autoAssign(serviceRequestID);
			}
			catch (Exception exception)
			{
				System.err.println("[DISPATCH] Failed to assign request " + serviceRequestID + ": " + exception.getMessage());
			}
		}
	}
}