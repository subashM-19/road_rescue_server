package com.sentinal.roadrescue.jobs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.Row;

public final class BackgroundJobManager
{
	private static final int THREAD_COUNT = 4;

	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(THREAD_COUNT, new BackgroundJobThreadFactory());

	private static volatile boolean shuttingDown;

	private BackgroundJobManager()
	{
	}


	public static void submit(BackgroundJob job)
	{
		validateJob(job);

		Row trackingRow = createTrackingRow(job);

		PersistenceObject persistenceObject = new PersistenceObject();

		persistenceObject.addRowForInsert(trackingRow);

		PersistenceManager.update(persistenceObject);

		submit(job, trackingRow);
	}

	/*
	 * Used when the BackgroundJob row was already
	 * inserted as part of another business transaction.
	 *
	 * Mechanic approval/rejection uses this form.
	 */
	public static void submit(BackgroundJob job, Row trackingRow)
	{
		validateJob(job);

		if (trackingRow == null)
		{
			throw new IllegalArgumentException("Background job tracking row cannot be null");
		}

		if (shuttingDown)
		{
			throw new IllegalStateException("Background job manager is shutting down");
		}

		EXECUTOR.submit(() -> executeJob(job, trackingRow));
	}

	/*
	 * Used by BackgroundJobScheduler.
	 *
	 * The execution happens on the scheduler thread.
	 * This is intentional because scheduleWithFixedDelay
	 * then guarantees that the same recurring execution
	 * cannot overlap with itself.
	 */
	static void executeScheduled(BackgroundJob job)
	{
		validateJob(job);

		if (shuttingDown)
		{
			return;
		}

		Row trackingRow = createTrackingRow(job);

		PersistenceObject persistenceObject = new PersistenceObject();

		persistenceObject.addRowForInsert(trackingRow);

		PersistenceManager.update(persistenceObject);

		executeJob(job, trackingRow);
	}

	public static Row createTrackingRow(BackgroundJob job)
	{
		validateJob(job);

		long now = System.currentTimeMillis();

		int maxAttempts = getMaxAttempts(job);

		Row row = new Row("BackgroundJob");

		row.set("JobName", job.getName());

		row.set("Status", BackgroundJobStatus.PENDING.getCode());

		row.set("AttemptCount", 0);

		row.set("CreatedTime", now);

		row.set("StartedTime", null);

		row.set("CompletedTime", null);

		row.set("ErrorMessage", null);

		return row;
	}

	private static void executeJob(BackgroundJob job, Row trackingRow)
	{
		int maxAttempts = getMaxAttempts(job);

		for (int attempt = 1; attempt <= maxAttempts; attempt++)
		{
			try
			{
				markRunning(trackingRow, attempt);

				job.execute();

				markCompleted(trackingRow);

				System.out.println("[BackgroundJob] Completed: " + job.getName());

				return;
			}
			catch (Exception exception)
			{
				boolean retry = job.isRetryEnabled() && attempt < maxAttempts;

				if (!retry)
				{
					markFailed(trackingRow, exception);

					System.err.println("[BackgroundJob] Failed: " + job.getName());

					exception.printStackTrace();

					return;
				}

				markPendingForRetry(trackingRow, exception);

				long delay = Math.max(0L, job.getRetryDelayMillis(attempt));

				try
				{
					Thread.sleep(delay);
				}
				catch (InterruptedException interrupted)
				{
					Thread.currentThread().interrupt();

					markFailed(trackingRow, interrupted);

					return;
				}
			}
		}
	}

	private static int getMaxAttempts(BackgroundJob job)
	{
		if (!job.isRetryEnabled())
		{
			return 1;
		}

		int configured = job.getMaxAttempts();

		if (configured < 2)
		{
			throw new IllegalArgumentException("Retry-enabled job must have at least 2 attempts: " + job.getName());
		}

		return configured;
	}

	private static void markRunning(Row trackingRow, int attempt)
	{
		long now = System.currentTimeMillis();

		trackingRow.set("Status", BackgroundJobStatus.PROCESSING.getCode());

		trackingRow.set("AttemptCount", attempt);

		trackingRow.set("StartedTime", now);

		trackingRow.set("CompletedTime", null);

		trackingRow.set("ErrorMessage", null);


		updateTrackingRow(trackingRow);
	}

	private static void markPendingForRetry(Row trackingRow, Exception exception)
	{
		long now = System.currentTimeMillis();

		trackingRow.set("Status", BackgroundJobStatus.PENDING.getCode());

		trackingRow.set("ErrorMessage", truncate(getErrorMessage(exception), 2000));

		updateTrackingRow(trackingRow);
	}

	private static void markCompleted(Row trackingRow)
	{
		long now = System.currentTimeMillis();

		trackingRow.set("Status", BackgroundJobStatus.COMPLETED.getCode());

		trackingRow.set("CompletedTime", now);

		trackingRow.set("ErrorMessage", null);


		updateTrackingRow(trackingRow);
	}

	private static void markFailed(Row trackingRow, Exception exception)
	{
		long now = System.currentTimeMillis();

		trackingRow.set("Status", BackgroundJobStatus.FAILED.getCode());

		trackingRow.set("CompletedTime", now);

		trackingRow.set("ErrorMessage", truncate(getErrorMessage(exception), 2000));


		updateTrackingRow(trackingRow);
	}

	private static void updateTrackingRow(Row trackingRow)
	{
		PersistenceObject persistenceObject = new PersistenceObject();

		persistenceObject.updateRow(trackingRow);

		PersistenceManager.update(persistenceObject);
	}

	private static String getErrorMessage(Exception exception)
	{
		if (exception == null)
		{
			return null;
		}

		if (exception.getMessage() != null)
		{
			return exception.getClass().getSimpleName() + ": " + exception.getMessage();
		}

		return exception.getClass().getName();
	}

	private static String truncate(String value, int maximumLength)
	{
		if (value == null)
		{
			return null;
		}

		if (value.length() <= maximumLength)
		{
			return value;
		}

		return value.substring(0, maximumLength);
	}

	private static void validateJob(BackgroundJob job)
	{
		if (job == null)
		{
			throw new IllegalArgumentException("Background job cannot be null");
		}

		String name = job.getName();

		if (name == null || name.trim().isEmpty())
		{
			throw new IllegalArgumentException("Background job name cannot be empty");
		}
	}

	public static void shutdown()
	{
		shuttingDown = true;

		EXECUTOR.shutdown();

		try
		{
			if (!EXECUTOR.awaitTermination(15, TimeUnit.SECONDS))
			{
				EXECUTOR.shutdownNow();
			}
		}
		catch (InterruptedException exception)
		{
			Thread.currentThread().interrupt();

			EXECUTOR.shutdownNow();
		}
	}

	private static final class BackgroundJobThreadFactory implements ThreadFactory
	{
		private final AtomicInteger counter = new AtomicInteger(1);

		@Override public Thread newThread(Runnable runnable)
		{
			Thread thread = new Thread(runnable, "roadrescue-background-" + counter.getAndIncrement());

			thread.setDaemon(true);

			return thread;
		}
	}
}