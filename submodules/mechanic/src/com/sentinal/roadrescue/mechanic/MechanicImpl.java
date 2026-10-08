package com.sentinal.roadrescue.mechanic;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.Join;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;
import com.sentinal.roadrescue.service.ServiceType;
import com.sentinal.roadrescue.service.VehicleType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MechanicImpl implements MechanicOperations
{
	private static final double SEARCH_RADIUS_KM = 20.0;

	@Override
	public List<Map<String, Object>> getNearbyMechanics(double latitude, double longitude, String vehicleType, String serviceType)
	{
		validateLocation(latitude, longitude);

		VehicleType vehicle = VehicleType.fromNodeRepresentation(vehicleType);
		ServiceType service = ServiceType.fromNodeRepresentation(serviceType);

		PersistenceQuery query = new PersistenceQuery("Mechanic").join(Join.INNER, "MechanicService", "Mechanic", "MechanicID", "MechanicService", "MechanicID").select("Mechanic", "MechanicID").select("Mechanic", "UserID").select("Mechanic", "Latitude").select("Mechanic", "Longitude").select("Mechanic", "AvailabilityStatus").where(Criteria.and(new Criteria("Mechanic.AvailabilityStatus", Criteria.EQUAL, MechanicAvailabilityStatus.AVAILABLE.getCode()), Criteria.and(new Criteria("MechanicService.VehicleType", Criteria.EQUAL, vehicle.getCode()), Criteria.and(new Criteria("MechanicService.ServiceType", Criteria.EQUAL, service.getCode()), new Criteria("MechanicService.Status", Criteria.EQUAL, 1)))));

		PersistenceObject dataObject = PersistenceManager.get(query);
		List<Map<String, Object>> mechanics = new ArrayList<>();

		for (Row row : dataObject.getRows("Mechanic"))
		{
			if (row.get("Latitude") == null || row.get("Longitude") == null)
			{
				continue;
			}

			double mechanicLatitude = ((Number) row.get("Latitude")).doubleValue();
			double mechanicLongitude = ((Number) row.get("Longitude")).doubleValue();

			double distance = calculateDistance(latitude, longitude, mechanicLatitude, mechanicLongitude);

			if (distance > SEARCH_RADIUS_KM)
			{
				continue;
			}

			Map<String, Object> mechanic = new LinkedHashMap<>();
			mechanic.put("mechanic_id", row.get("MechanicID"));
			mechanic.put("user_id", row.get("UserID"));
			mechanic.put("latitude", mechanicLatitude);
			mechanic.put("longitude", mechanicLongitude);
			mechanic.put("distance_km", distance);
			mechanic.put("availability", MechanicAvailabilityStatus.AVAILABLE.getNodeRepresentation());

			mechanics.add(mechanic);
		}

		mechanics.sort(Comparator.comparingDouble(mechanic -> ((Number) mechanic.get("distance_km")).doubleValue()));

		return mechanics;
	}

	private double calculateDistance(double latitude1, double longitude1, double latitude2, double longitude2)
	{
		double earthRadius = 6371.0;

		double latitudeDistance = Math.toRadians(latitude2 - latitude1);
		double longitudeDistance = Math.toRadians(longitude2 - longitude1);

		double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2) + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return earthRadius * c;
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