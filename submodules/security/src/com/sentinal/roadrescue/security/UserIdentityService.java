package com.sentinal.roadrescue.security;

import com.auth0.jwt.interfaces.DecodedJWT;

import com.sentinal.persistencefw.transaction.TransactionProxy;

/** REST facade delegating business work to an interface transaction proxy. */
public class UserIdentityService
{
	private static final class Holder
	{
		private static final UserIdentityOperations INSTANCE = TransactionProxy.create(UserIdentityOperations.class, new UserIdentityImpl());
	}

	public static UserIdentityOperations getUserIdentityImpl()
	{
		return Holder.INSTANCE;
	}

	public RoadRescueUserPrincipal resolveUser(String externalSubject, DecodedJWT jwt)
	{
		return getUserIdentityImpl().resolveUser(externalSubject, jwt);
	}
}
