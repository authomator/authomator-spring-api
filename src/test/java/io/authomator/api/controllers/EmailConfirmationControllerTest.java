package io.authomator.api.controllers;

import static io.authomator.api.TestUtil.APPLICATION_JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.authomator.api.AuthomatorApiApplication;
import io.authomator.api.builders.UserBuilder;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.domain.service.UserService;
import io.authomator.api.jwt.JwtService;
import io.authomator.api.mail.MailService;
import io.authomator.api.mail.MailTransport;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class EmailConfirmationControllerTest {

	private static String USER_EMAIL = "some@user.tld";
	private static String USER_PASSWORD = "somepass";
	
	@Autowired
	private FilterChainProxy filterChainProxy;
	
	@Autowired
    private WebApplicationContext webApplicationContext;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private MailService mailService;
	
	@Value("${io.authomator.api.verification.email.enabled:true}")
	private boolean registrationStatus;
	
    private MockMvc mockMvc;

    private MailTransport mockTransport;
    
    private User user;
    
    @Before
    public void setup() {
    	mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilters(filterChainProxy)
                .build();
    	
    	user = new UserBuilder()
        		.withEmail(USER_EMAIL)
        		.withPassword(USER_PASSWORD)
        		.build();
        userRepository.save(user);
        
        mockTransport = Mockito.mock(MailTransport.class);
        ReflectionTestUtils.setField(mailService, "transport", mockTransport);
    }
    
    @After
	public void cleanup(){
    	userRepository.deleteAll();
		ReflectionTestUtils.setField(userService, "verificationEmailEnabled", registrationStatus);
    }
    
    
    /*
	  ___              _    ___           __ _                _   _            ___            _ _ 
	 / __| ___ _ _  __| |  / __|___ _ _  / _(_)_ _ _ __  __ _| |_(_)___ _ _   | __|_ __  __ _(_) |
	 \__ \/ -_) ' \/ _` | | (__/ _ \ ' \|  _| | '_| '  \/ _` |  _| / _ \ ' \  | _|| '  \/ _` | | |
	 |___/\___|_||_\__,_|  \___\___/_||_|_| |_|_| |_|_|_\__,_|\__|_\___/_||_| |___|_|_|_\__,_|_|_|
                                                                                              
     */
    
    private Map<String,String> createSendConfirmRequest() throws JoseException{
    	JsonWebSignature accessToken = jwtService.getAccessToken(user);
    	Map<String, String> req = new HashMap<>();
    	req.put("url", "https://authomator.io/confirm/email");
    	req.put("accessToken", accessToken.getCompactSerialization());
    	return req;
    }
    
    @Test
    public void sendConfirmEmail() throws JsonProcessingException, Exception{
    	
    	Map<String, String> req = createSendConfirmRequest();
    	    	
    	mockMvc
			.perform(
				post("/send-confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isNoContent());
    	
    	verify(mockTransport, times(1)).sendConfirmEmailEmail(eq(USER_EMAIL), startsWith("https://authomator.io/confirm/email"));
    }
    
    @Test
    public void sendConfirmEmail_when_email_confirm_disabled() throws JsonProcessingException, Exception{
    	
    	ReflectionTestUtils.setField(userService, "verificationEmailEnabled", false);
    	Map<String, String> req = createSendConfirmRequest();
    	    	
    	mockMvc
			.perform(
				post("/send-confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isForbidden())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Email confirmation is not enabled"))
	        .andExpect(jsonPath("$.code").value("EmailConfirmationNotEnabled"));
    	
    	verify(mockTransport, times(0)).sendConfirmEmailEmail(null, null);
    }
    
    @Test
    public void sendConfirmEmail_when_email_already_confirmed() throws JsonProcessingException, Exception{
    	
    	Map<String, String> req = createSendConfirmRequest();
    	user.setEmailVerified(true);
    	userRepository.save(user);
    	
    	mockMvc
			.perform(
				post("/send-confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isForbidden())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Email confirmation was already performed"))
	        .andExpect(jsonPath("$.code").value("UserEmailConfirmedAlready"));
    	
    	verify(mockTransport, times(0)).sendConfirmEmailEmail(null, null);
    }
    
    @Test
    public void sendConfirmEmail_when_user_is_nonexisting() throws JsonProcessingException, Exception{
    	
    	Map<String, String> req = createSendConfirmRequest();
    	userRepository.delete(user);
    	
    	mockMvc
			.perform(
				post("/send-confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isUnprocessableEntity())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
	        .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
	        .andExpect(jsonPath("$.fieldErrors[0].field").value("accessToken"))
	        .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid JWT access token"))
	        .andExpect(jsonPath("$.fieldErrors[0].code").value("CredentialsError"));
    	
    	verify(mockTransport, times(0)).sendConfirmEmailEmail(null, null);
    }
    
    @Test
    public void sendConfirmEmail_when_unauthorized_domain() throws JsonProcessingException, Exception{
    	
    	Map<String, String> req = createSendConfirmRequest();
    	req.put("url", "https://evil.cracker.local/confirm");
    	
    	mockMvc
			.perform(
				post("/send-confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isForbidden())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("The requested url is not allowed"))
	        .andExpect(jsonPath("$.code").value("UnauthorizedDomain"));
    	
    	verify(mockTransport, times(0)).sendConfirmEmailEmail(null, null);
    }
    
    
    @Test
    public void sendConfirmEmail_when_non_secure_url() throws JsonProcessingException, Exception{
    	
    	Map<String, String> req = createSendConfirmRequest();
    	req.put("url", "http://authomator.io/confirm/email");
    	
    	mockMvc
			.perform(
				post("/send-confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isForbidden())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("The requested url is not secure"))
	        .andExpect(jsonPath("$.code").value("NonSecureUrl"));
    	
    	verify(mockTransport, times(0)).sendConfirmEmailEmail(null, null);
    }
    
    
    @Test
    public void sendConfirmEmail_empty_body() throws JsonProcessingException, Exception{
    	
    	Map<String, String> req = createSendConfirmRequest();
    	req.put("url", "http://authomator.io/confirm/email");
    	
    	mockMvc
			.perform(
				post("/send-confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
			)
			.andDo(print())
	        .andExpect(status().isBadRequest())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Invalid http request"))
	        .andExpect(jsonPath("$.code").value("HttpMessageNotReadable"));
    	
    	verify(mockTransport, times(0)).sendConfirmEmailEmail(null, null);
    }
    
    
    /*
	   ___           __ _             ___            _ _ 
	  / __|___ _ _  / _(_)_ _ _ __   | __|_ __  __ _(_) |
	 | (__/ _ \ ' \|  _| | '_| '  \  | _|| '  \/ _` | | |
	  \___\___/_||_|_| |_|_| |_|_|_| |___|_|_|_\__,_|_|_|
                                                     
     */
    
    @Test
    public void confirmEmail() throws JsonProcessingException, Exception{
    	
    	Map<String, String> req = new HashMap<>();
    	req.put("confirmEmailToken", jwtService.getConfirmEmailToken(user).getCompactSerialization());

    	assertFalse(user.getEmailVerified());
    	
    	mockMvc
			.perform(
				post("/confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isNoContent());
    	
    	user = userRepository.findOne(user.getId());
    	assertTrue(user.getEmailVerified());
    }
    
    
    @Test
    public void confirmEmail_when_verification_is_disabled() throws JsonProcessingException, Exception{
    	
    	ReflectionTestUtils.setField(userService, "verificationEmailEnabled", false);
    	
    	Map<String, String> req = new HashMap<>();
    	req.put("confirmEmailToken", jwtService.getConfirmEmailToken(user).getCompactSerialization());

    	assertFalse(user.getEmailVerified());
    	
    	mockMvc
			.perform(
				post("/confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isForbidden())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Email confirmation is not enabled"))
	        .andExpect(jsonPath("$.code").value("EmailConfirmationNotEnabled"));
    	
    	user = userRepository.findOne(user.getId());
    	assertFalse(user.getEmailVerified());
    }
    
    
    @Test
    public void confirmEmail_when_verification_is_already_done() throws JsonProcessingException, Exception{
    	    	
    	Map<String, String> req = new HashMap<>();
    	req.put("confirmEmailToken", jwtService.getConfirmEmailToken(user).getCompactSerialization());
    	
    	user.setEmailVerified(true);
    	user = userRepository.save(user);
    	
    	mockMvc
			.perform(
				post("/confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isForbidden())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Email confirmation was already performed"))
	        .andExpect(jsonPath("$.code").value("UserEmailConfirmedAlready"));   	
    }
    
    @Test
    public void confirmEmail_when_user_is_deleted() throws JsonProcessingException, Exception{
    	    	
    	Map<String, String> req = new HashMap<>();
    	req.put("confirmEmailToken", jwtService.getConfirmEmailToken(user).getCompactSerialization());
    	
    	userRepository.delete(user);
    	
    	mockMvc
			.perform(
				post("/confirm-email")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
			.andDo(print())
	        .andExpect(status().isUnprocessableEntity())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
	        .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
	        .andExpect(jsonPath("$.fieldErrors[0].field").value("accessToken"))
	        .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid JWT access token"))
	        .andExpect(jsonPath("$.fieldErrors[0].code").value("CredentialsError"));
    }
    
}