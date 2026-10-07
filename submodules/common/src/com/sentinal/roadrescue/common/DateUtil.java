package com.sentinal.roadrescue.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtil
{
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.systemDefault());

	public static String formatEpochTime(Object value)
	{
		if (value == null)
		{
			return null;
		}

		long epochTime = ((Number) value).longValue();

		return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(epochTime));
	}
}
