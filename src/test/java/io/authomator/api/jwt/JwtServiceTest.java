package io.authomator.api.jwt;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import io.authomator.api.AuthomatorApiApplication;
import io.authomator.api.domain.entity.Context;
import io.authomator.api.domain.entity.User;
import io.authomator.api.dto.TokenReply;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class JwtServiceTest {

	@Autowired
	JwtService jwtService;
	
	@Value("${io.authomator.api.alg:HMAC_SHA512}")
	private String defaultAlg;
	
	@Value("${io.authomator.api.issuer}")
	private String defaultIssuer;
	
	@Value("${io.authomator.api.audience}")
	private String[] defaultAudience;
	
	@Value("${io.authomator.api.ttl:60}")
	private int defaultTtl;
	
	@Value("${io.authomator.api.secret}")
	private String defaultSecret;
	
	@Value("${io.authomator.api.secretinternal}")
	private String defaultInternalSecret;
	
	@Value("${io.authomator.api.ttlrefresh:120}")
	private int defaultTtlRefresh;
	
	@Value("${io.authomator.api.ttlforgot:60}")
	private int defaultTtlForgot;
	
		
	@After
	public void cleanup(){
		ReflectionTestUtils.invokeMethod(jwtService, "setAlgFromAlgorithmIdentifiers", defaultAlg);
	}
	
	//--------------------------------------------------------------------------
	//  Config tests
	//--------------------------------------------------------------------------
	
	@Test
	public void userRegistrationConfig(){
		assertNotNull("Should have secret", 
				ReflectionTestUtils.getField(jwtService, "secret"));
		assertNotNull("Should have alg", 
				ReflectionTestUtils.getField(jwtService, "alg"));
		assertNotNull("Should have ttl", 
				ReflectionTestUtils.getField(jwtService, "ttl"));
		assertNotNull("Should have issuer", 
				ReflectionTestUtils.getField(jwtService, "issuer"));
		assertNotNull("Should have audience", 
				ReflectionTestUtils.getField(jwtService, "audience"));
		assertNotNull("Should have internalSecret", 
				ReflectionTestUtils.getField(jwtService, "internalSecret"));
		assertNotNull("Should have ttlRefresh", 
				ReflectionTestUtils.getField(jwtService, "ttlRefresh"));
		assertNotNull("Should have ttlForgot", 
				ReflectionTestUtils.getField(jwtService, "ttlForgot"));
	}
	
	@Test
	public void setAlgFromAlgorithmIdentifiers(){
		ReflectionTestUtils.invokeMethod(jwtService, "setAlgFromAlgorithmIdentifiers", "HMAC_SHA512");
		assertEquals("HS512", 
			(String)ReflectionTestUtils.getField(jwtService, "alg")
		);
		ReflectionTestUtils.invokeMethod(jwtService, "setAlgFromAlgorithmIdentifiers", "RSA_PSS_USING_SHA256");
		assertEquals("PS256", 
			(String)ReflectionTestUtils.getField(jwtService, "alg")
		);
	}
	
	
	//--------------------------------------------------------------------------
	//  JwtClaims
	//--------------------------------------------------------------------------
	
	private User createTestUser() {
		
		
		User user = new User();
		user.setEmail("testuser@mydomain.tld");
		user.setId("someid");
		user.setRoles("USER");
		user.setRoles("ADMIN");
		return user;
	}
	
	private Context createTestContext(User user){
		Context ctx = new Context();
		ctx.setId("somectxid");
		ctx.setName(user.getEmail());
		ctx.setOwner(user);
		return ctx;
	}
	
	
	private void checkClaimExpires(JwtClaims claims, int ttl) throws MalformedClaimException{
		NumericDate expire = NumericDate.now();
		expire.addSeconds((ttl*60) + 1);
		assertTrue(
			claims.getExpirationTime().isBefore(expire)
		);
		assertNull(claims.getClaimValue("email"));
	}
	
	
	@Test
	public void getUserClaims() throws MalformedClaimException {
		User user = createTestUser();
		Context ctx = createTestContext(user);
		
		Object claimsmaybe = ReflectionTestUtils.invokeMethod(jwtService, "getUserClaims", user, ctx);
		Assert.isInstanceOf(JwtClaims.class, claimsmaybe);
		JwtClaims claims = (JwtClaims) claimsmaybe;
		
		assertEquals("someid", claims.getSubject());
		assertTrue(
			claims.getStringListClaimValue("roles").contains("USER")
		);
		assertTrue(
			claims.getStringListClaimValue("roles").contains("ADMIN")
		);
		assertEquals(defaultIssuer, claims.getIssuer());
		assertEquals(Arrays.asList(defaultAudience), claims.getAudience());
		
		checkClaimExpires(claims, defaultTtl);
		assertNull(claims.getClaimValue("email"));
	}
		
	
	private JwtClaims testUserToken(String jwt) throws JoseException, InvalidJwtException, MalformedClaimException{
		
		JwtConsumerBuilder cb = new JwtConsumerBuilder();
		cb.setVerificationKey(new HmacKey(defaultSecret.getBytes()));
		cb.setJwsAlgorithmConstraints(AlgorithmConstraints.DISALLOW_NONE);
		cb.setExpectedAudience(defaultAudience[0]);
		cb.setExpectedIssuer(defaultIssuer);
		JwtConsumer cons = cb.build(); 
		
		JwtClaims claims = cons.process(jwt).getJwtClaims();
		assertEquals("someid",claims.getSubject());
		assertEquals("somectxid",claims.getStringClaimValue("ctx"));
		
		return claims;
	}
	
	
	@Test
	public void getAccessToken() throws JoseException, InvalidJwtException, MalformedClaimException {
		User user = createTestUser();
		Context ctx = createTestContext(user);
		user.setEmailVerified(true);
		String jwt = jwtService.getAccessToken(user, ctx).getCompactSerialization();
		testUserToken(jwt);
		JwtClaims accessClaims = testUserToken(jwt);
		assertNull(accessClaims.getClaimValue("email"));
		assertTrue((boolean)accessClaims.getClaimValue("ev"));
	}
	
	
	@Test
	public void getIdentityToken() throws JoseException, InvalidJwtException, MalformedClaimException {
		User user = createTestUser();
		Context ctx = createTestContext(user);
		String jwt = jwtService.getIdentityToken(user, ctx).getCompactSerialization();		
		JwtClaims idClaims = testUserToken(jwt);
		assertEquals(user.getEmail(), idClaims.getClaimValue("email"));
		assertFalse((boolean)idClaims.getClaimValue("emailVerified"));
	}
	
	
	@Test
	public void getRefreshToken() throws JoseException, InvalidJwtException, MalformedClaimException {
		User user = createTestUser();
		Context ctx = createTestContext(user);
		String jwt = jwtService.getRefreshToken(user, ctx).getCompactSerialization();		
		JwtConsumerBuilder cb = new JwtConsumerBuilder();
		cb.setVerificationKey(new HmacKey(defaultInternalSecret.getBytes()));
		cb.setJwsAlgorithmConstraints(AlgorithmConstraints.DISALLOW_NONE);
		cb.setExpectedAudience(defaultIssuer + "#refresh");
		cb.setExpectedIssuer(defaultIssuer);
		JwtConsumer cons = cb.build(); 		
		JwtClaims claims = cons.process(jwt).getJwtClaims();

		assertEquals(user.getId(),claims.getSubject());	
		assertNull(claims.getClaimValue("email"));
		assertNull(claims.getStringListClaimValue("role"));
		checkClaimExpires(claims, defaultTtlRefresh);
	}
	
	
	@Test
	public void getConfirmEmailToken() throws JoseException, InvalidJwtException, MalformedClaimException {
		User user = createTestUser();
		String jwt = jwtService.getConfirmEmailToken(user).getCompactSerialization();		
		JwtConsumerBuilder cb = new JwtConsumerBuilder();
		cb.setVerificationKey(new HmacKey(defaultInternalSecret.getBytes()));
		cb.setJwsAlgorithmConstraints(AlgorithmConstraints.DISALLOW_NONE);
		cb.setExpectedAudience(defaultIssuer + "#confirm-email");
		cb.setExpectedIssuer(defaultIssuer);
		JwtConsumer cons = cb.build(); 		
		JwtClaims claims = cons.process(jwt).getJwtClaims();

		assertEquals(user.getId(),claims.getSubject());	
		assertNull(claims.getClaimValue("email"));
		assertNull(claims.getStringListClaimValue("role"));
		checkClaimExpires(claims, defaultTtlRefresh);
	}
	
	
	@Test
	public void getForgotPasswordToken() throws JoseException, InvalidJwtException, MalformedClaimException {
		User user = createTestUser();
		String jwt = jwtService.getForgotPasswordToken(user).getCompactSerialization();		
		JwtConsumerBuilder cb = new JwtConsumerBuilder();
		cb.setVerificationKey(new HmacKey(defaultInternalSecret.getBytes()));
		cb.setJwsAlgorithmConstraints(AlgorithmConstraints.DISALLOW_NONE);
		cb.setExpectedAudience(defaultIssuer + "#forgot");
		cb.setExpectedIssuer(defaultIssuer);
		JwtConsumer cons = cb.build();
		JwtClaims claims = cons.process(jwt).getJwtClaims();
		
		assertEquals(user.getId(),claims.getSubject());	
		assertNull(claims.getClaimValue("email"));
		assertNull(claims.getStringListClaimValue("role"));
		checkClaimExpires(claims, defaultTtlForgot);
	}
	
	@Test
	public void createTokensForUser() throws JoseException, InvalidJwtException {
		User user = createTestUser();
		Context ctx = createTestContext(user);
		TokenReply tr = jwtService.createTokensForUser(user, ctx);
		assertNotNull(tr);
		assertNotNull(tr.getAccessToken());
		assertNotNull(tr.getIdentityToken());
		assertNotNull(tr.getRefreshToken());
		assertFalse(tr.getAccessToken().equals(tr.getIdentityToken()));
		assertFalse(tr.getAccessToken().equals(tr.getRefreshToken()));
		assertFalse(tr.getRefreshToken().equals(tr.getIdentityToken()));
		jwtService.validateRefreshToken(tr.getRefreshToken());
		jwtService.validateAccessToken(tr.getAccessToken());
	}
	
	
	
	private JsonWebSignature signClaims(JwtClaims claims, String secret){		
		JsonWebSignature jws = new JsonWebSignature();
		jws.setPayload(claims.toJson());
		jws.setKey(new HmacKey(secret.getBytes()));
		jws.setAlgorithmHeaderValue((String)ReflectionTestUtils.getField(jwtService, "alg"));
		jws.setKeyIdHeaderValue("0");
		return jws;
	}
	
	private JwtClaims createValidRefreshClaims(){
		JwtClaims refresh = new JwtClaims();
		refresh.setIssuer(defaultIssuer);
		refresh.setAudience(defaultIssuer + "#refresh");
		refresh.setExpirationTimeMinutesInTheFuture(10);
		refresh.setIssuedAtToNow();
		refresh.setNotBeforeMinutesInThePast(1);
		refresh.setSubject("useridRefres12");
		refresh.setStringClaim("ctx", "userCtxRefres12");
		return refresh;
	}
	
	private JwtClaims createValidForgotClaims(){
		JwtClaims forgot = new JwtClaims();
		forgot.setIssuer(defaultIssuer);
		forgot.setAudience(defaultIssuer + "#forgot");
		forgot.setExpirationTimeMinutesInTheFuture(10);
		forgot.setIssuedAtToNow();
		forgot.setNotBeforeMinutesInThePast(1);
		forgot.setSubject("useridForgot12");
		return forgot;
	}
	
	private JwtClaims createValidConfirmEmailClaims(){
		JwtClaims confirm = new JwtClaims();
		confirm.setIssuer(defaultIssuer);
		confirm.setAudience(defaultIssuer + "#confirm-email");
		confirm.setExpirationTimeMinutesInTheFuture(10);
		confirm.setIssuedAtToNow();
		confirm.setNotBeforeMinutesInThePast(1);
		confirm.setSubject("useridConfirmEmail");
		return confirm;
	}
	
	private JwtClaims createValidAccessClaims(){
		JwtClaims access = new JwtClaims();
		access.setIssuer(defaultIssuer);
		access.setAudience(defaultAudience);
		access.setExpirationTimeMinutesInTheFuture(10);
		access.setIssuedAtToNow();
		access.setNotBeforeMinutesInThePast(1);
		access.setSubject("somevaliduserid");
		access.setStringClaim("ctx", "somevalidCtxid");
		return access;
	}
	
	@Test
	public void validateRefreshToken() throws InvalidJwtException, MalformedClaimException, JoseException{
		
		JwtClaims refresh = createValidRefreshClaims();		
		JsonWebSignature refreshJwt = signClaims(refresh, defaultInternalSecret);
		
		Object refreshParsed = jwtService.validateRefreshToken(refreshJwt.getCompactSerialization());
		assertNotNull(refreshParsed);
		Assert.isInstanceOf(JwtClaims.class, refreshParsed);
		assertEquals("useridRefres12", ((JwtClaims)refreshParsed).getSubject());
	}
	
	@Test(expected=InvalidJwtException.class)
	public void validateRefreshTokenShouldCheckIfCtxClaimIsPresent() throws InvalidJwtException, JoseException {
		
		JwtClaims refresh = createValidRefreshClaims();
		refresh.unsetClaim("ctx");
		JsonWebSignature refreshJwt = signClaims(refresh, defaultInternalSecret);
		
		try {
			jwtService.validateRefreshToken(refreshJwt.getCompactSerialization());
		}
		catch(InvalidJwtException ex) {
			assertEquals("Refresh token is missing ctx claim", ex.getMessage());
			throw ex;
		}
	}
	
	@Test(expected=InvalidJwtException.class)
	public void validateRefreshTokenShouldNotAcceptForgotToken() throws InvalidJwtException, JoseException {
		
		JwtClaims refresh = createValidForgotClaims();		
		JsonWebSignature refreshJwt = signClaims(refresh, defaultInternalSecret);
		
		jwtService.validateRefreshToken(refreshJwt.getCompactSerialization());
	}
	
	@Test
	public void validateForgotToken() throws InvalidJwtException, MalformedClaimException, JoseException{
		
		JwtClaims forgot = createValidForgotClaims();		
		JsonWebSignature forgotJwt = signClaims(forgot, defaultInternalSecret);
		
		Object forgotParsed = jwtService.validateForgotToken(forgotJwt.getCompactSerialization());
		assertNotNull(forgotParsed);
		Assert.isInstanceOf(JwtClaims.class, forgotParsed);
		assertEquals("useridForgot12", ((JwtClaims)forgotParsed).getSubject());
	}
	
	
	@Test
	public void validateConfirmEmailToken() throws InvalidJwtException, MalformedClaimException, JoseException{
		
		JwtClaims confirm = createValidConfirmEmailClaims();		
		JsonWebSignature forgotJwt = signClaims(confirm, defaultInternalSecret);
		
		Object confirmParsed = jwtService.validateConfirmEmailToken(forgotJwt.getCompactSerialization());
		assertNotNull(confirmParsed);
		Assert.isInstanceOf(JwtClaims.class, confirmParsed);
		assertEquals("useridConfirmEmail", ((JwtClaims)confirmParsed).getSubject());
	}
	
	
	@Test(expected=InvalidJwtException.class)
	public void validateForgotTokenShouldNotAcceptRefreshToken() throws InvalidJwtException, JoseException{
		
		JwtClaims forgot = createValidRefreshClaims();		
		JsonWebSignature forgotJwt = signClaims(forgot, defaultInternalSecret);
				
		jwtService.validateForgotToken(forgotJwt.getCompactSerialization());		
	}
	
	
	@Test
	public void validateAccessToken () throws Throwable {
		JwtClaims access = createValidAccessClaims();
		JsonWebSignature accessJwt = signClaims(access, defaultSecret);
		
		JwtClaims claims = jwtService.validateAccessToken(accessJwt.getCompactSerialization());
		assertNotNull(claims);
		assertEquals(claims.getSubject(), "somevaliduserid");
		
	}
	
	@Test(expected=InvalidJwtException.class)
	public void validateAccessTokenWithInvalidToken() throws Throwable {
		JwtClaims access = createValidRefreshClaims();
		JsonWebSignature accessJwt = signClaims(access, defaultInternalSecret);
		
		jwtService.validateAccessToken(accessJwt.getCompactSerialization());		
	}
}
