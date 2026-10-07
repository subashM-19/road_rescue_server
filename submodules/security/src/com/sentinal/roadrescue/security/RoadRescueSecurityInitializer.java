package com.sentinal.roadrescue.security;

import com.sentinal.roadrescue.dispatch.DispatchScheduler;

import com.sentinal.securityfw.SecurityManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class RoadRescueSecurityInitializer implements ServletContextListener
{
	@Override public void contextInitialized(ServletContextEvent event)
	{
		String auth0Domain = System.getenv("AUTH0_DOMAIN");

		String auth0Audience = System.getenv("AUTH0_AUDIENCE");

		SecurityManager.initialize(new Auth0AuthenticationProvider(auth0Domain, auth0Audience), new RoadRescueAuthorizationProvider(), new RoadRescueRateLimiter());

		DispatchScheduler.start();

		System.out.println("RoadRescue security initialized with Auth0");

		System.out.println("RoadRescue dispatch scheduler started");
	}

	@Override public void contextDestroyed(ServletContextEvent event)
	{
		DispatchScheduler.stop();
	}
}