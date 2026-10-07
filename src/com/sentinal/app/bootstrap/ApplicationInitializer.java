package com.sentinal.app.bootstrap;

import com.sentinal.app.persistence.PersistenceInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ApplicationInitializer
	implements ServletContextListener
{
	@Override
	public void contextInitialized(ServletContextEvent event)
	{
		ServletContext servletContext =
			event.getServletContext();

		String applicationRoot =
			servletContext.getRealPath("/");

		if (applicationRoot == null)
		{
			throw new IllegalStateException(
				"Unable to determine application deployment path"
			);
		}

		PersistenceInitializer.initialize(
			applicationRoot
		);
	}

	@Override
	public void contextDestroyed(ServletContextEvent event)
	{
	}
}