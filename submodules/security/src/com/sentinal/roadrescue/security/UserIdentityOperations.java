package com.sentinal.roadrescue.security;

import com.auth0.jwt.interfaces.DecodedJWT;

/** Business contract; calls through the proxy use the shared transaction timeout. */
public interface UserIdentityOperations
{
	RoadRescueUserPrincipal resolveUser(String externalSubject, DecodedJWT jwt);
}
