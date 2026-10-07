package com.sentinal.roadrescue.notification;

public interface EmailProvider
{
	void send(String email, String subject, String body) throws Exception;
}