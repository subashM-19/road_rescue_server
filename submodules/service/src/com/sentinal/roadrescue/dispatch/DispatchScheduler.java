package com.sentinal.roadrescue.dispatch;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DispatchScheduler
{
	private static ScheduledExecutorService scheduler;

	private DispatchScheduler()
	{
	}

	public static synchronized void start()
	{
		if (scheduler != null)
		{
			return;
		}

		scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "roadrescue-dispatch");

			thread.setDaemon(true);

			return thread;
		});

		AutoAssignmentService service = new AutoAssignmentService();

		scheduler.scheduleWithFixedDelay(() -> {
			try
			{
				service.expirePendingAssignments();
				service.assignPendingRequests();
			}
			catch (Throwable throwable)
			{
				throwable.printStackTrace();
			}
		}, 0, 5, TimeUnit.HOURS);
	}

	public static synchronized void stop()
	{
		if (scheduler == null)
		{
			return;
		}

		scheduler.shutdownNow();

		scheduler = null;
	}
}