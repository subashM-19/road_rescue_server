package com.sentinal.roadrescue.notification;

public final class EmailManager
{
	private static volatile EmailProvider provider;

	private EmailManager()
	{
	}

	public static void initialize(EmailProvider emailProvider)
	{
		if (emailProvider == null)
		{
			throw new IllegalArgumentException("EmailProvider cannot be null");
		}

		provider = emailProvider;
	}

	public static EmailProvider getProvider()
	{
		EmailProvider current = provider;

		if (current == null)
		{
			throw new IllegalStateException("EmailManager is not initialized");
		}

		return current;
	}
}