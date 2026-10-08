package com.sentinal.roadrescue.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import com.sentinal.securityfw.AuthenticatedPrincipal;
import com.sentinal.securityfw.AuthenticationException;
import com.sentinal.securityfw.AuthenticationProvider;
import com.sentinal.securityfw.SecurityContext;

import javax.servlet.http.HttpServletRequest;

import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Auth0AuthenticationProvider implements AuthenticationProvider
{
	private final String issuer;
	private final String audience;

	private final JwkProvider jwkProvider;

	private final UserIdentityService userIdentityService;

	public Auth0AuthenticationProvider(String domain, String audience)
	{
		if (domain == null || domain.trim().isEmpty())
		{
			throw new IllegalArgumentException("AUTH0_DOMAIN is required");
		}

		if (audience == null || audience.trim().isEmpty())
		{
			throw new IllegalArgumentException("AUTH0_AUDIENCE is required");
		}

		String normalizedDomain = domain.replace("https://", "").replace("http://", "");

		this.issuer = "https://" + normalizedDomain + "/";

		this.audience = audience;

		this.jwkProvider = new JwkProviderBuilder(normalizedDomain).cached(10, 24, TimeUnit.HOURS).rateLimited(10, 1, TimeUnit.MINUTES).build();

		this.userIdentityService = new UserIdentityService();
	}

	@Override public SecurityContext authenticate(HttpServletRequest request)
	{
		String authorization = request.getHeader("Authorization");

		String token = extractBearerToken(authorization);

		try
		{
			DecodedJWT decoded = JWT.decode(token);

			String algorithmName = decoded.getAlgorithm();

			if (!"RS256".equals(algorithmName))
			{
				throw new AuthenticationException("Unsupported token algorithm");
			}

			String keyID = decoded.getKeyId();

			if (keyID == null || keyID.trim().isEmpty())
			{
				throw new AuthenticationException("Token signing key ID is missing");
			}

			Jwk jwk = jwkProvider.get(keyID);

			RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

			Algorithm algorithm = Algorithm.RSA256(publicKey, null);

			JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build();

			DecodedJWT verifiedJWT = verifier.verify(token);

			return buildSecurityContext(verifiedJWT);
		}
		catch (Exception e)
		{
			System.err.println("JWT verification failed: " + e.getClass().getName() + ": " + e.getMessage());

			e.printStackTrace();
			throw new AuthenticationException("Invalid access token", e);
		}
	}

	private SecurityContext buildSecurityContext(DecodedJWT jwt)
	{
		String subject = jwt.getSubject();

		if (subject == null || subject.trim().isEmpty())
		{
			throw new AuthenticationException("Token subject is missing");
		}

		RoadRescueUserPrincipal user = userIdentityService.resolveUser(subject, jwt);

		Set<Integer> roles = Collections.singleton(user.getRole());

		AuthenticatedPrincipal principal = new AuthenticatedPrincipal(user.getUserID(), "user", roles);

		return new SecurityContext(principal);
	}

	private String extractBearerToken(String authorization)
	{
		if (authorization == null || authorization.trim().isEmpty())
		{
			throw new AuthenticationException("Authorization header is required");
		}

		if (!authorization.startsWith("Bearer "))
		{
			throw new AuthenticationException("Bearer token is required");
		}

		String token = authorization.substring(7).trim();

		if (token.isEmpty())
		{
			throw new AuthenticationException("Bearer token is required");
		}

		return token;
	}
}