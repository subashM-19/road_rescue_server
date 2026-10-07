package com.sentinal.roadrescue.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LocalObjectStorageProvider implements ObjectStorageProvider
{
	private final Path rootDirectory;

	public LocalObjectStorageProvider(String rootDirectory)
	{
		this.rootDirectory = Path.of(rootDirectory).toAbsolutePath().normalize();
	}

	@Override public void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) throws IOException
	{
		Path target = resolve(objectKey);

		Path parent = target.getParent();

		if (parent != null)
		{
			Files.createDirectories(parent);
		}

		Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
	}

	@Override public InputStream download(String objectKey) throws IOException
	{
		return Files.newInputStream(resolve(objectKey));
	}

	@Override public void delete(String objectKey) throws IOException
	{
		Files.deleteIfExists(resolve(objectKey));
	}

	@Override public boolean exists(String objectKey)
	{
		return Files.exists(resolve(objectKey));
	}

	private Path resolve(String objectKey)
	{
		Path result = rootDirectory.resolve(objectKey).normalize();

		if (!result.startsWith(rootDirectory))
		{
			throw new IllegalArgumentException("Invalid object key");
		}

		return result;
	}
}