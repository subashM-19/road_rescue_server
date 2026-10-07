package com.sentinal.roadrescue.storage;

public final class ObjectStorageManager
{
	private static volatile ObjectStorageProvider provider;

	private ObjectStorageManager()
	{
	}

	public static void initialize(ObjectStorageProvider objectStorageProvider)
	{
		if (objectStorageProvider == null)
		{
			throw new IllegalArgumentException("ObjectStorageProvider cannot be null");
		}

		provider = objectStorageProvider;
	}

	public static ObjectStorageProvider getProvider()
	{
		ObjectStorageProvider current = provider;

		if (current == null)
		{
			throw new IllegalStateException("ObjectStorageManager is not initialized");
		}

		return current;
	}
}