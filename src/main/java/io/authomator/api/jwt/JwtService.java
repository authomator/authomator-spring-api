package io.authomator.api.jwt;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.authomator.api.domain.entity.Context;
import io.authomator.api.domain.entity.User;
import io.authomator.api.dto.TokenReply;

@Component
public class JwtService {

	private static final String REFRESH_TOKEN_SUFFIX = "#refresh";
	private static final String FORGOT_TOKEN_SUFFIX = "#forgot";
	private static final String CONFIRM_EMAIL_TOKEN_SUFFIX = "#confirm-email";

	/**
	 * Secret used to sign the jwt
	 */
	private String secret;

	/**
	 * The algorithm to use for signing tokens
	 */
	private String alg;

	/**
	 * Name of the jwt issuer
	 */
	private String issuer;

	/**
	 * List of audience
	 */
	private List<String> audience;

	/**
	 * Secret used to sign the refresh tokens
	 */
	private String internalSecret;

	/**
	 * TTL in minutes that an access token will be valid
	 */
	private int ttl = 60;
	
	/**
	 * TTL in minutes that a refreshtoken will be valid
	 */
	private int ttlRefresh = 60 * 8;
	
	/**
	 * TTL in minutes that a forgot password token will be valid
	 */
	private int ttlForgot = 60;

	/**
	 * Constructor gets autowired with properties
	 * 
	 * @param secret
	 * @param alg
	 * @param ttl
	 * @param issuer
	 * @param audience
	 * @param refreshSecret
	 */
	@Autowired
	public JwtService(
			@Value("${io.authomator.api.secret}") String secret,
			@Value("${io.authomator.api.secretinternal}") String internalSecret,
			@Value("${io.authomator.api.alg:HMAC_SHA512}") String alg, 
			@Value("${io.authomator.api.ttl:60}") int ttl,
			@Value("${io.authomator.api.ttlrefresh:120}") int ttlRefresh,
			@Value("${io.authomator.api.ttlforgot:60}") int ttlForgot,
			@Value("${io.authomator.api.issuer}") String issuer,
			@Value("${io.authomator.api.audience}") String[] audience ) {

		this.secret = secret;
		this.ttl = ttl;
		this.ttlRefresh = ttlRefresh;
		this.ttlForgot = ttlForgot;
		this.issuer = issuer;
		this.audience = Arrays.asList(audience);
		this.internalSecret = internalSecret;
		setAlgFromAlgorithmIdentifiers(alg);
	}

	// TODO: create issue at https://bitbucket.org/b_c/jose4j/
	/**
	 * Sets the algorithm to use for jwt based on AlgorithmIdentifiers which
	 * probably should have been an enum ?
	 *
	 * @param alg
	 */
	private void setAlgFromAlgorithmIdentifiers(final String alg) {
		final Field[] fields = AlgorithmIdentifiers.class.getFields();
		for (Field field : fields) {
			if (field.getName().equals(alg)) {
				try {
					this.alg = (String) field.get(null);
				} catch (IllegalArgumentException e) {
					// Dont bother
				} catch (IllegalAccessException e) {
					// Dont bother
				}
			}
		}
		if (this.alg == null) {
			throw new RuntimeException("Invalid JWT algorith specified in io.authomator.api.alg");
		}
	}

	/**
	 * Create a user claims suitable for access token (only sub/roles/context)
	 * 
	 * @param user
	 * @return JwtClaims
	 */
	private JwtClaims getUserClaims(User user, Context context) {
		JwtClaims claims = new JwtClaims();
		claims.setIssuer(issuer);
		claims.setAudience(audience);
		claims.setExpirationTimeMinutesInTheFuture(ttl);
		claims.setIssuedAtToNow();
		claims.setNotBeforeMinutesInThePast(1);
		claims.setSubject(user.getId());
		claims.setStringListClaim("roles", user.getRoles());
		claims.setStringClaim("ctx", context.getId());
		return claims;
	}

	/**
	 * Create a claims suitable for the refresh/forgot password token
	 * 
	 * @param user
	 * @return JwtClaims
	 */
	private JwtClaims getInternalClaims(User user, String suffix, int ttl) {
		JwtClaims claims = new JwtClaims();
		claims.setIssuer(issuer);
		claims.setAudience(issuer + suffix);
		claims.setExpirationTimeMinutesInTheFuture(ttl);
		claims.setIssuedAtToNow();
		claims.setNotBeforeMinutesInThePast(1);
		claims.setSubject(user.getId());
		return claims;
	}

	/**
	 * Sign the user claims
	 * 
	 * @param claims
	 * @return JsonWebSignature
	 */
	private JsonWebSignature signUserClaims(JwtClaims claims) {
		JsonWebSignature jws = new JsonWebSignature();
		jws.setPayload(claims.toJson());
		jws.setKey(new HmacKey(secret.getBytes()));
		jws.setAlgorithmHeaderValue(this.alg);
		jws.setKeyIdHeaderValue("0");
		return jws;
	}

	/**
	 * Sign the internal claims
	 * 
	 * @param claims
	 * @return JsonWebSignature
	 */
	private JsonWebSignature signInternalClaims(JwtClaims claims) {
		JsonWebSignature jws = new JsonWebSignature();
		jws.setPayload(claims.toJson());
		jws.setKey(new HmacKey(internalSecret.getBytes()));
		jws.setAlgorithmHeaderValue(this.alg);
		jws.setKeyIdHeaderValue("0");
		return jws;
	}

	/**
	 * Create the access token for the specified user
	 * 
	 * Access token content:
	 * 
	 * - sub: String userId 
	 * - roles: List<String> roles
	 * 
	 * @param user
	 * @return JsonWebSignature
	 */
	public JsonWebSignature getAccessToken(User user, Context context) {
		JwtClaims claims = getUserClaims(user, context);
		claims.setClaim("ev", user.getEmailVerified());
		return signUserClaims(claims);
	}

	/**
	 * Create the identity token for the specified user
	 * 
	 * * Access token content:
	 * 
	 * - sub: String userId 
	 * - roles: List<String> roles
	 * - email: String email
	 * 
	 * @param user
	 * @return JsonWebSignature
	 */
	public JsonWebSignature getIdentityToken(User user, Context context) {
		JwtClaims claims = getUserClaims(user, context);
		claims.setClaim("email", user.getEmail());
		claims.setClaim("emailVerified", user.getEmailVerified());
		claims.setClaim("contexts", user.getContexts().stream().map( c -> c.getId()).collect(Collectors.toList())); //TODO: should use mapper
		return signUserClaims(claims);
	}

	/**
	 * Create the refresh token for the specified user
	 * 
	 * Refresh token content:
	 * 
	 * - sub: String userId
	 * 
	 * @param user
	 * @return JsonWebSignature
	 */
	public JsonWebSignature getRefreshToken(User user, Context context) {
		JwtClaims claims = getInternalClaims(user, REFRESH_TOKEN_SUFFIX, ttlRefresh);
		claims.setStringClaim("ctx", context.getId());
		return signInternalClaims(claims);
	}

	/**
	 * Create the password forgotten token for the specified user
	 * 
	 * @param user
	 * @return
	 */
	public JsonWebSignature getForgotPasswordToken(User user) {
		return signInternalClaims(getInternalClaims(user, FORGOT_TOKEN_SUFFIX, ttlForgot));
	}
	
	/**
	 * Create the email confirmation token for the specified user
	 * 
	 * @param user
	 * @return
	 */
	public JsonWebSignature getConfirmEmailToken(User user) {
		return signInternalClaims(getInternalClaims(user, CONFIRM_EMAIL_TOKEN_SUFFIX, ttlRefresh));
	}
	
	
	/**
	 * Create the tokens for a user
	 * @param user
	 * @return
	 * @throws JoseException
	 */
	public TokenReply createTokensForUser(User user, Context context) throws JoseException{		
		TokenReply reply = new TokenReply();		
		reply.setAccessToken(getAccessToken(user, context).getCompactSerialization());
		reply.setIdentityToken(getIdentityToken(user, context).getCompactSerialization());
		reply.setRefreshToken(getRefreshToken(user, context).getCompactSerialization());
		return reply;
	}
	
	
	/**
	 * Validate and return the claims for the specified internal token
	 * 
	 * @param jwt
	 * @param suffix - [REFRESH_TOKEN_SUFFIX|FORGOT_TOKEN_SUFFIX]
	 * @return JwtClaims
	 * @throws InvalidJwtException
	 */
	private JwtClaims validateInternalToken(String jwt, String suffix) throws InvalidJwtException{
		JwtConsumer jwtConsumer = new JwtConsumerBuilder()
            .setRequireExpirationTime()
            .setAllowedClockSkewInSeconds(30)
            .setRequireSubject()
            .setExpectedIssuer(issuer)
            .setExpectedAudience(issuer + suffix)
            .setJwsAlgorithmConstraints(AlgorithmConstraints.DISALLOW_NONE)
            .setVerificationKey(new HmacKey(internalSecret.getBytes()))
            .build();		
		return jwtConsumer.process(jwt).getJwtClaims();		
	}
	
	/**
	 * Validate a refresh token (in compact serialization format)
	 * 
	 * @param jwt
	 * @return JwtClaims
	 * @throws InvalidJwtException
	 */
	public JwtClaims validateRefreshToken(String jwt) throws InvalidJwtException {
		return validateInternalToken(jwt, REFRESH_TOKEN_SUFFIX);
	}
	
	
	/**
	 * Validate a forgot password token (in compact serialization format)
	 * 
	 * @param jwt
	 * @return
	 * @throws InvalidJwtException
	 */
	public JwtClaims validateForgotToken(String jwt) throws InvalidJwtException {
		return validateInternalToken(jwt, FORGOT_TOKEN_SUFFIX);
	}
	
	
	/**
	 * Validate a confirm email token (in compact serialization format)
	 * 
	 * @param jwt
	 * @return
	 * @throws InvalidJwtException
	 */
	public JwtClaims validateConfirmEmailToken(String jwt) throws InvalidJwtException {
		return validateInternalToken(jwt, CONFIRM_EMAIL_TOKEN_SUFFIX);
	}
	
	/**
	 * Validate an access token (in compact serialization format)
	 * NOTICE:: This does NOT perform any ExpectedAudience check !!
	 * 
	 * @param jwt
	 * @return
	 * @throws InvalidJwtException
	 */
	public JwtClaims validateAccessToken(String jwt) throws InvalidJwtException {
		JwtConsumer jwtConsumer = new JwtConsumerBuilder()
			.setRequireExpirationTime()
            .setAllowedClockSkewInSeconds(30)
            .setRequireSubject()
            .setExpectedIssuer(issuer)
            .setSkipDefaultAudienceValidation()
            .setJwsAlgorithmConstraints(AlgorithmConstraints.DISALLOW_NONE)
            .setVerificationKey(new HmacKey(secret.getBytes()))
            .build();
		return jwtConsumer.process(jwt).getJwtClaims();
	}
}