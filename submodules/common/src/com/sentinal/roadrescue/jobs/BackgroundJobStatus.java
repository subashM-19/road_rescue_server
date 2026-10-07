package com.sentinal.roadrescue.jobs;

public enum BackgroundJobStatus
{
	PENDING(1),
	PROCESSING(2),
	COMPLETED(3),
	FAILED(4);

	private final int code;

	BackgroundJobStatus(int code)
	{
		this.code = code;
	}

	public int getCode()
	{
		return code;
	}
}