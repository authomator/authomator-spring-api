package io.authomator.api.controllers;

import static io.authomator.api.TestUtil.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.jose4j.jws.JsonWebSignature;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.authomator.api.AuthomatorApiApplication;
import io.authomator.api.builders.ForgotPasswordRequestBuilder;
import io.authomator.api.builders.UserBuilder;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.domain.service.UserService;
import io.authomator.api.dto.TokenReply;
import io.authomator.api.jwt.JwtService;
import io.authomator.api.mail.MailService;
import io.authomator.api.mail.MailTransport;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class ResetPasswordControllerTest {

	private static final String USER_EMAIL = "test@local.tld";
	private static final String USER_PASSWORD = "somepassword";
	private static final String RESET_URL = "https://authomator.io";
	
	@Autowired
	private FilterChainProxy filterChainProxy;
	
	@Autowired
    private WebApplicationContext webApplicationContext;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private MailService mailService;
	
	@Autowired 
	private JwtService jwtService;
	
	private MockMvc mockMvc;
	
	private MailTransport mockTransport;

    @Before
    public void setup() {
    	mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilters(filterChainProxy)
                .build();
    	
    	User user = new UserBuilder()
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
    }
    
    /*
	  ___              _   ___                 _     __  __      _ _ 
	 / __| ___ _ _  __| | | __|__ _ _ __ _ ___| |_  |  \/  |__ _(_) |
	 \__ \/ -_) ' \/ _` | | _/ _ \ '_/ _` / _ \  _| | |\/| / _` | | |
	 |___/\___|_||_\__,_| |_|\___/_| \__, \___/\__| |_|  |_\__,_|_|_|
	                                 |___/                           
    */
    
    @Test
    public void sendMailResetToken_sends_mail() throws Exception {
    	    	
    	String req = new ForgotPasswordRequestBuilder()
    			.withEmail(USER_EMAIL)
    			.withUrl(RESET_URL)
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/forgot-password")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(req)
			)
    		.andDo(print())
            .andExpect(status().isNoContent());
    	
    	verify(mockTransport, times(1)).sendForgotEmail(eq(USER_EMAIL), startsWith(RESET_URL));
    }
    
    
    @Test
    public void sendMailResetToken_sends_unprocessable_for_unknown() throws Exception {
    	    	
    	String req = new ForgotPasswordRequestBuilder()
    			.withEmail("incorrect@email.local")
    			.withUrl(RESET_URL)
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/forgot-password")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(req)
			)
    		.andDo(print())
            .andExpect(status().isUnprocessableEntity())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
	        .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid or non existing email"))
	        .andExpect(jsonPath("$.fieldErrors[0].code").value("CredentialsError"));
    	
    	verify(mockTransport, never()).sendForgotEmail(null, null);
    }
    
    
    @Test
    public void sendMailResetToken_sends_unauthorized_for_non_secure_url() throws Exception {
    	    	
    	String req = new ForgotPasswordRequestBuilder()
    			.withEmail(USER_EMAIL)
    			.withUrl("http://authomator.io")
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/forgot-password")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(req)
			)
    		.andDo(print())
            .andExpect(status().isUnauthorized())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("The requested url is not secure"))
	        .andExpect(jsonPath("$.code").value("NonSecureUrl"));
    	
    	verify(mockTransport, never()).sendForgotEmail(null, null);
    }
    
    
    @Test
    public void sendMailResetToken_sends_unauthorized_for_unauthorized_domain() throws Exception {
    	    	
    	String req = new ForgotPasswordRequestBuilder()
    			.withEmail(USER_EMAIL)
    			.withUrl("https://hckz.io")
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/forgot-password")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(req)
			)
    		.andDo(print())
            .andExpect(status().isUnauthorized())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("The requested url is not allowed"))
	        .andExpect(jsonPath("$.code").value("UnauthorizedDomain"));
    	
    	verify(mockTransport, never()).sendForgotEmail(null, null);
    }
    
    /*
	  ___             _     ___                              _ 
	 | _ \___ ___ ___| |_  | _ \__ _ _______ __ _____ _ _ __| |
	 |   / -_|_-</ -_)  _| |  _/ _` (_-<_-< V  V / _ \ '_/ _` |
	 |_|_\___/__/\___|\__| |_| \__,_/__/__/\_/\_/\___/_| \__,_|
                                                            
     */
    
    
    @Test
    public void reset_with_valid_token() throws Exception {

    	User user = userRepository.findByEmail(USER_EMAIL);
    	System.out.println(user.getPassword());
    	Assert.notNull(user);
    	
    	JsonWebSignature token = jwtService.getForgotPasswordToken(user);
    	
    	Map<String, String> req = new HashMap<>();
    	req.put("resetToken", token.getCompactSerialization());
    	req.put("password", "newpassword");
    	
    	mockMvc
    		.perform(
				post("/reset-password")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
    		.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
    		.andExpect(jsonPath("$.identityToken").exists());
    	
    	User newPassUser = userService.signIn(USER_EMAIL, "newpassword");
    	Assert.notNull(newPassUser);

    }
    

    @Test
    public void reset_with_valid_token_but_nonexisting_user() throws Exception {

    	User user = userRepository.findByEmail(USER_EMAIL);
    	Assert.notNull(user);
    	
    	JsonWebSignature token = jwtService.getForgotPasswordToken(user);
    	
    	userRepository.deleteAll();
    	
    	Map<String, String> req = new HashMap<>();
    	req.put("resetToken", token.getCompactSerialization());
    	req.put("password", "newpassword");
    	
    	mockMvc
    		.perform(
				post("/reset-password")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
    		.andDo(print())
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
	        .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid or non existing email"))
	        .andExpect(jsonPath("$.fieldErrors[0].code").value("CredentialsError"));
    }
    
    @Test
    public void reset_with_invalid_token() throws Exception {

    	User user = userRepository.findByEmail(USER_EMAIL);
    	Assert.notNull(user);
    	
    	JsonWebSignature token = jwtService.getForgotPasswordToken(user);
    	
    	userRepository.deleteAll();
    	
    	Map<String, String> req = new HashMap<>();
    	req.put("resetToken", token.getCompactSerialization() + "defect");
    	req.put("password", "newpassword");
    	
    	mockMvc
    		.perform(
				post("/reset-password")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
    		.andDo(print())
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("resetToken"))
	        .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid jwt token"))
	        .andExpect(jsonPath("$.fieldErrors[0].code").value("InvalidToken"));
    }
	
}
