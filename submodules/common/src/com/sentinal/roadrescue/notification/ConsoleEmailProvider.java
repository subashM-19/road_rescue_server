package com.sentinal.roadrescue.notification;

public class ConsoleEmailProvider implements EmailProvider
{
	@Override public void send(String email, String subject, String body)
	{
		System.out.println("========== ROADRESCUE EMAIL ==========");

		System.out.println("To: " + email);

		System.out.println("Subject: " + subject);

		System.out.println();

		System.out.println(body);

		System.out.println("======================================");
	}
}