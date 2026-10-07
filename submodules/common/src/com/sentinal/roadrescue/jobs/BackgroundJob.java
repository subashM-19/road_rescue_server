package com.sentinal.roadrescue.jobs;

public interface BackgroundJob extends Runnable
{
	String getName();

	void execute() throws Exception;

	default boolean isRetryEnabled()
	{
		return false;
	}

	default int getMaxAttempts()
	{
		return 1;
	}

	default long getRetryDelayMillis(int attemptNumber)
	{
		return 5000L;
	}

	@Override default void run()
	{
		try
		{
			execute();
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Background job failed: " + getName(), exception);
		}
	}
}