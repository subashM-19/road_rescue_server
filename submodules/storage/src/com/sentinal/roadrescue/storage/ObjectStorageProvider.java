package com.sentinal.roadrescue.storage;

import java.io.InputStream;

public interface ObjectStorageProvider
{
	void upload(String objectKey, InputStream inputStream, long contentLength, String contentType) throws Exception;

	InputStream download(String objectKey) throws Exception;

	void delete(String objectKey) throws Exception;

	boolean exists(String objectKey) throws Exception;
}