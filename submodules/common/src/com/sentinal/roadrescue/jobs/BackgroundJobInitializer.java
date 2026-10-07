package com.sentinal.roadrescue.jobs;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.sentinal.roadrescue.notification.ConsoleEmailProvider;
import com.sentinal.roadrescue.notification.EmailManager;
import com.sentinal.roadrescue.storage.LocalObjectStorageProvider;
import com.sentinal.roadrescue.storage.ObjectStorageManager;

public class BackgroundJobInitializer implements ServletContextListener
{
	@Override public void contextInitialized(ServletContextEvent event)
	{
		initializeEmail();
		registerScheduledJobs();
		initializeObjectStorage();

		System.out.println("RoadRescue background services initialized");
	}

	private void registerScheduledJobs()
	{
		/*
		 * Add recurring Phase-1 jobs here when their
		 * concrete classes are implemented.
		 *
		 * Example:
		 *
		 * BackgroundJobScheduler.scheduleWithFixedDelay(
		 *     new MechanicOfflineJob(),
		 *     1,
		 *     1,
		 *     TimeUnit.MINUTES
		 * );
		 *
		 * Because this uses fixed-delay execution,
		 * one invocation finishes before the delay
		 * for the next invocation begins.
		 */
	}


	@Override public void contextDestroyed(ServletContextEvent event)
	{
		BackgroundJobScheduler.shutdown();
		BackgroundJobManager.shutdown();

		System.out.println("RoadRescue background services stopped");
	}

	private void initializeEmail()
	{
		EmailManager.initialize(new ConsoleEmailProvider());
	}

	private void initializeObjectStorage()
	{
		String storageDirectory = System.getenv("ROAD_RESCUE_OBJECT_STORAGE_DIR");

		if (storageDirectory == null || storageDirectory.trim().isEmpty())
		{
			storageDirectory = System.getProperty("java.io.tmpdir") + "/roadrescue-storage";
		}

		ObjectStorageManager.initialize(new LocalObjectStorageProvider(storageDirectory));
	}
}