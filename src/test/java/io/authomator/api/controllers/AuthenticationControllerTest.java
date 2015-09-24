package io.authomator.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;

import static org.hamcrest.Matchers.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.authomator.api.AuthomatorApiApplication;
import static io.authomator.api.TestUtil.APPLICATION_JSON;
import io.authomator.api.builders.LoginRequestBuilder;
import io.authomator.api.builders.UserBuilder;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.domain.service.UserService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class AuthenticationControllerTest {
	
	private static final String USER_EMAIL = "test@local.tld";
	private static final String USER_PASSWORD = "test@local.tld";
	private static final String[] USER_ROLES = new String[]{"USER", "ADMIN"};

	private static final String SIGNUP_USER_EMAIL = "signmeup@test.com";
		
	@Autowired
	private FilterChainProxy filterChainProxy;
	
	@Autowired
    private WebApplicationContext webApplicationContext;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private UserService userService;
	
	@Value("${io.authomator.api.registration.allow:false}")
	private boolean registrationStatus;
	
	
    private MockMvc mockMvc;

    @Before
    public void setup() {
    	System.out.println(filterChainProxy);
    	mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilters(filterChainProxy)
                .build();
                
        User user = new UserBuilder()
        		.withEmail(USER_EMAIL)
        		.withPassword(USER_PASSWORD)
        		.withRoles(USER_ROLES)
        		.build();
        userRepository.save(user);
                
        ReflectionTestUtils.setField(userService, "registrationEnabled", true);
    }
    
    @After
	public void cleanup(){
    	userRepository.deleteAll();
		ReflectionTestUtils.setField(userService, "registrationEnabled", registrationStatus);
    }
    
    /*
     _              _        _          _      
    | |   ___  __ _(_)_ _   | |_ ___ __| |_ ___
    | |__/ _ \/ _` | | ' \  |  _/ -_|_-<  _(_-<
    |____\___/\__, |_|_||_|  \__\___/__/\__/__/
              |___/                            
    */
    @Test
    public void getAccount_for_valid_returns_tokens() throws Exception {
    	    	
    	String req = new LoginRequestBuilder()
    			.withEmail(USER_EMAIL)
    			.withPassword(USER_PASSWORD)
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/sign-in")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(req)
			)
    		.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
    		.andExpect(jsonPath("$.identityToken").exists());
    }
    
    
    private void expectGenericCredentialsError(ResultActions act) throws Exception{
    	act
	    	.andExpect(status().isUnprocessableEntity())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors", hasSize(2)))
			.andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("email", "password")))
	        .andExpect(jsonPath("$.fieldErrors[*].message", hasItems("Invalid email or password")))
	        .andExpect(jsonPath("$.fieldErrors[*].code",  hasItems("CredentialsError")));
    }
    
    @Test
    public void getAccount_for_unknown_returns_unprocessable() throws Exception {
    	    	
    	String req = new LoginRequestBuilder()
    			.withEmail("non@existing.be")
    			.withPassword(USER_PASSWORD)
    			.buildAsJson();
    	
    	expectGenericCredentialsError(	
	    	mockMvc
	    		.perform(
					post("/sign-in")
					.accept(APPLICATION_JSON)
					.contentType(APPLICATION_JSON)
					.content(req)
				)
	    		.andDo(print())
		);
                				
    }
    
    
    @Test
    public void getAccount_for_incorrect_password_returns_unprocessable() throws Exception {
    	    	
    	String req = new LoginRequestBuilder()
    			.withEmail(USER_EMAIL)
    			.withPassword("InC0rr3cP8zssw891;__")
    			.buildAsJson();
    	
    	expectGenericCredentialsError(    	
	    	mockMvc
	    		.perform(
					post("/sign-in")
					.accept(APPLICATION_JSON)
					.contentType(APPLICATION_JSON)
					.content(req)
				)
	    		.andDo(print())
		);    				
    }
    
    
    @Test
    public void getAccount_for_malformed_email_returns_unprocessable() throws Exception {
    	    	
    	String req = new LoginRequestBuilder()
    			.withEmail("noemail")
    			.withPassword(USER_PASSWORD)
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/sign-in")
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
            .andExpect(jsonPath("$.fieldErrors[0].message").value("not a well-formed email address"))
            .andExpect(jsonPath("$.fieldErrors[0].code").value("Email"));    				
    }
    
    
    @Test
    public void getAccount_for_short_password_returns_unprocessable() throws Exception {
    	    	
    	String req = new LoginRequestBuilder()
    			.withEmail(USER_EMAIL)
    			.withPassword("1")
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/sign-in")
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
    		.andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("length must be between 6 and 32"))
            .andExpect(jsonPath("$.fieldErrors[0].code").value("Length"));    				
    }
    
    @Test
    public void getAccount_for_missing_password_returns_unprocessable() throws Exception {
    	    	
    	HashMap<String,String> req = new HashMap<>();
    	req.put("email", USER_EMAIL);
    	    	
    	mockMvc
    		.perform(
				post("/sign-in")
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
    		.andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("may not be empty"))
            .andExpect(jsonPath("$.fieldErrors[0].code").value("NotBlank"));				
    }
    
    
    /*
	  ___ _                      _          _      
	 / __(_)__ _ _ _ _  _ _ __  | |_ ___ __| |_ ___
	 \__ \ / _` | ' \ || | '_ \ |  _/ -_|_-<  _(_-<
	 |___/_\__, |_||_\_,_| .__/  \__\___/__/\__/__/
	       |___/         |_|                                                 
   */
    @Test
    public void signup_for_valid_returns_tokens() throws Exception {
    	
    	String req = new LoginRequestBuilder()
    			.withEmail(SIGNUP_USER_EMAIL)
    			.withPassword(USER_PASSWORD)
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/register")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(req)
			)
    		.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.identityToken").exists());
    }
    
    @Test
    public void signup_for_existing_returns_unprocessable() throws Exception {
    	
    	String req = new LoginRequestBuilder()
    			.withEmail(USER_EMAIL)
    			.withPassword(USER_PASSWORD)
    			.buildAsJson();
    	
    	expectGenericCredentialsError(
	    	mockMvc
	    		.perform(
					post("/register")
					.accept(APPLICATION_JSON)
					.contentType(APPLICATION_JSON)
					.content(req)
				)
	    		.andDo(print())
    	);
    }
    
    @Test
    public void signup_with_invalid_email_returns_unprocessable() throws Exception {
    	
    	String req = new LoginRequestBuilder()
    			.withEmail("noemail")
    			.withPassword(USER_PASSWORD)
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/register")
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
            .andExpect(jsonPath("$.fieldErrors[0].message").value("not a well-formed email address"))
            .andExpect(jsonPath("$.fieldErrors[0].code").value("Email"));
    }
    
    
    @Test
    public void signup_with_missing_password_returns_unprocessable() throws Exception {
    	    	
    	HashMap<String,String> req = new HashMap<>();
    	req.put("email", USER_EMAIL);
    	    	
    	mockMvc
    		.perform(
				post("/sign-in")
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
    		.andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("may not be empty"))
            .andExpect(jsonPath("$.fieldErrors[0].code").value("NotBlank"));			
    }
    
    
    @Test
    public void signup_with_too_long_password_returns_unprocessable() throws Exception {
    	    	
    	HashMap<String,String> req = new HashMap<>();
    	req.put("email", USER_EMAIL);
    	req.put("password", "123456789012345678901234567890333"); // max 32
    	    	
    	mockMvc
    		.perform(
				post("/sign-in")
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
    		.andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("length must be between 6 and 32"))
            .andExpect(jsonPath("$.fieldErrors[0].code").value("Length"));			
    }
    
    
    @Test
    public void signup_when_signup_is_disabled() throws Exception {
    	
    	ReflectionTestUtils.setField(userService, "registrationEnabled", false);
    	
    	String req = new LoginRequestBuilder()
    			.withEmail(SIGNUP_USER_EMAIL)
    			.withPassword(USER_PASSWORD)
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/register")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(req)
			)
    		.andDo(print())
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("Registration is not allowed"))
            .andExpect(jsonPath("$.code").value("RegistrationDisabled"));
    	
    }
    
}