package com.sentinal.roadrescue.security;

import com.sentinal.securityfw.RateLimitException;
import com.sentinal.securityfw.RateLimiter;
import com.sentinal.securityfw.SecurityContext;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoadRescueRateLimiter implements RateLimiter
{
	private static final long WINDOW_MS = 60_000L;

	private static final long CLEANUP_INTERVAL_MS = 300_000L;

	private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

	private volatile long lastCleanupTime = System.currentTimeMillis();

	@Override public void check(SecurityContext context, HttpServletRequest request)
	{
		cleanupIfRequired();

		String path = request.getRequestURI();

		String method = request.getMethod();

		int limit = determineLimit(path, method);

		String subject;

		if (context != null)
		{
			subject = "user:" + context.getPrincipalID();
		}
		else
		{
			subject = "ip:" + request.getRemoteAddr();
		}

		String category = determineCategory(path, method);

		String key = subject + ":" + category;

		long now = System.currentTimeMillis();

		Bucket bucket = buckets.compute(key, (ignored, current) -> {
			if (current == null || now - current.windowStart >= WINDOW_MS)
			{
				return new Bucket(now);
			}

			return current;
		});

		int count = bucket.count.incrementAndGet();

		if (count > limit)
		{
			throw new RateLimitException("Too many requests. Please retry shortly.");
		}
	}

	private int determineLimit(String path, String method)
	{
		if (path.contains("/api/ws-ticket"))
		{
			return 20;
		}

		if ("POST".equalsIgnoreCase(method) && path.endsWith("/api/service-requests"))
		{
			return 10;
		}

		if (path.contains("/api/admin/"))
		{
			return 120;
		}

		if (path.endsWith("/health"))
		{
			return 120;
		}

		return 180;
	}

	private String determineCategory(String path, String method)
	{
		if (path.contains("/api/ws-ticket"))
		{
			return "ws-ticket";
		}

		if ("POST".equalsIgnoreCase(method) && path.endsWith("/api/service-requests"))
		{
			return "create-request";
		}

		if (path.contains("/api/admin/"))
		{
			return "admin";
		}

		return "general";
	}

	private void cleanupIfRequired()
	{
		long now = System.currentTimeMillis();

		if (now - lastCleanupTime < CLEANUP_INTERVAL_MS)
		{
			return;
		}

		synchronized (this)
		{
			if (now - lastCleanupTime < CLEANUP_INTERVAL_MS)
			{
				return;
			}

			for (Map.Entry<String, Bucket> entry : buckets.entrySet())
			{
				if (now - entry.getValue().windowStart > WINDOW_MS * 2)
				{
					buckets.remove(entry.getKey(), entry.getValue());
				}
			}

			lastCleanupTime = now;
		}
	}

	private static final class Bucket
	{
		private final long windowStart;

		private final AtomicInteger count = new AtomicInteger();

		private Bucket(long windowStart)
		{
			this.windowStart = windowStart;
		}
	}
}