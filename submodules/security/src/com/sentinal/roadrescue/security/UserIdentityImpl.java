package com.sentinal.roadrescue.security;

import com.auth0.jwt.interfaces.DecodedJWT;

import com.sentinal.persistencefw.Criteria;
import com.sentinal.persistencefw.PersistenceManager;
import com.sentinal.persistencefw.PersistenceObject;
import com.sentinal.persistencefw.PersistenceQuery;
import com.sentinal.persistencefw.Row;

import com.sentinal.roadrescue.roles.UserRole;
import com.sentinal.securityfw.AuthenticationException;

public class UserIdentityImpl implements UserIdentityOperations
{
	@Override
	public RoadRescueUserPrincipal resolveUser(String externalSubject, DecodedJWT jwt)
	{
		Row identity = findIdentity(externalSubject);

		if (identity != null)
		{
			long userID = ((Number) identity.get("UserID")).longValue();

			return loadUserPrincipal(userID);
		}

		String email = jwt.getClaim("https://roadrescue.app/email").asString();
		if (email == null || email.trim().isEmpty())
		{
			throw new AuthenticationException("Email claim is required for first login");
		}

		Boolean emailVerified = jwt.getClaim("https://roadrescue.app/email_verified").asBoolean();

		if (!Boolean.TRUE.equals(emailVerified))
		{
			throw new AuthenticationException("Verified email is required");
		}

		Row user = findUserByEmail(email);

		if (user == null)
		{
			user = createUser(email, jwt);
		}

		long userID = ((Number) user.get("UserID")).longValue();

		createIdentity(userID, externalSubject);

		return buildPrincipal(user);
	}

	private Row createUser(String email, DecodedJWT jwt)
	{
		String name = jwt.getClaim("name").asString();

		if (name == null || name.trim().isEmpty())
		{
			name = deriveNameFromEmail(email);
		}
		else
		{
			name = name.trim();
		}

		Row user = new Row("Users");

		user.set("Name", name);

		user.set("Email", email);

		user.set("Phone", "");

		user.set("UserType", UserRole.CUSTOMER.getCode());

		user.set("Status", 1);

		PersistenceObject dataObject = new PersistenceObject();

		dataObject.addRowForInsert(user);

		PersistenceManager.update(dataObject);
		Object userID = user.get("UserID");

		if (userID == null)
		{
			throw new AuthenticationException("Unable to create RoadRescue user");
		}

		return user;
	}

	private String deriveNameFromEmail(String email)
	{
		int index = email.indexOf('@');

		if (index > 0)
		{
			return email.substring(0, index);
		}

		return email;
	}

	private Row findIdentity(String externalSubject)
	{
		PersistenceQuery query = new PersistenceQuery("UserIdentity").where(new Criteria("ExternalSubject", Criteria.EQUAL, externalSubject));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("UserIdentity").stream().findFirst().orElse(null);
	}

	private Row findUserByEmail(String email)
	{
		PersistenceQuery query = new PersistenceQuery("Users").where(new Criteria("Email", Criteria.EQUAL, email));

		PersistenceObject dataObject = PersistenceManager.get(query);

		return dataObject.getRows("Users").stream().findFirst().orElse(null);
	}

	private RoadRescueUserPrincipal loadUserPrincipal(long userID)
	{
		PersistenceQuery query = new PersistenceQuery("Users").where(new Criteria("UserID", Criteria.EQUAL, userID));

		PersistenceObject dataObject = PersistenceManager.get(query);

		Row user = dataObject.getRows("Users").stream().findFirst().orElseThrow(() -> new AuthenticationException("RoadRescue user not found"));

		return buildPrincipal(user);
	}

	private RoadRescueUserPrincipal buildPrincipal(Row user)
	{
		int status = ((Number) user.get("Status")).intValue();

		if (status != 1)
		{
			throw new AuthenticationException("RoadRescue account is disabled");
		}

		int role = ((Number) user.get("UserType")).intValue();

		long userID = ((Number) user.get("UserID")).longValue();

		return new RoadRescueUserPrincipal(userID, role);
	}

	private void createIdentity(long userID, String externalSubject)
	{
		Row identity = new Row("UserIdentity");

		identity.set("UserID", userID);

		identity.set("Provider", "auth0");

		identity.set("ExternalSubject", externalSubject);

		PersistenceObject dataObject = new PersistenceObject();

		dataObject.addRowForInsert(identity);

		PersistenceManager.update(dataObject);
	}
}