package com.sentinal.roadrescue.jobs;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class BackgroundJobScheduler
{
	private static final int THREAD_COUNT = 2;

	private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(THREAD_COUNT, new ScheduledJobThreadFactory());

	private static volatile boolean shuttingDown;

	private BackgroundJobScheduler()
	{
	}

	public static void scheduleWithFixedDelay(BackgroundJob job, long initialDelay, long delay, TimeUnit unit)
	{
		if (job == null)
		{
			throw new IllegalArgumentException("Background job cannot be null");
		}

		if (initialDelay < 0)
		{
			throw new IllegalArgumentException("Initial delay cannot be negative");
		}

		if (delay <= 0)
		{
			throw new IllegalArgumentException("Delay must be greater than zero");
		}

		if (unit == null)
		{
			throw new IllegalArgumentException("TimeUnit cannot be null");
		}

		SCHEDULER.scheduleWithFixedDelay(() -> {
			if (shuttingDown)
			{
				return;
			}

			try
			{
				BackgroundJobManager.executeScheduled(job);
			}
			catch (Exception exception)
			{
				System.err.println("[BackgroundJobScheduler] " + "Scheduled execution failed: " + job.getName());

				exception.printStackTrace();
			}
		}, initialDelay, delay, unit);
	}

	public static void shutdown()
	{
		shuttingDown = true;

		SCHEDULER.shutdown();

		try
		{
			if (!SCHEDULER.awaitTermination(15, TimeUnit.SECONDS))
			{
				SCHEDULER.shutdownNow();
			}
		}
		catch (InterruptedException exception)
		{
			Thread.currentThread().interrupt();

			SCHEDULER.shutdownNow();
		}
	}

	private static final class ScheduledJobThreadFactory implements ThreadFactory
	{
		private final AtomicInteger counter = new AtomicInteger(1);

		@Override public Thread newThread(Runnable runnable)
		{
			Thread thread = new Thread(runnable, "roadrescue-scheduler-" + counter.getAndIncrement());

			thread.setDaemon(true);

			return thread;
		}
	}
}