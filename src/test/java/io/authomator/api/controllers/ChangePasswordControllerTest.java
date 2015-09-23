package io.authomator.api.controllers;

import static io.authomator.api.TestUtil.APPLICATION_JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.jose4j.jws.JsonWebSignature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.authomator.api.AuthomatorApiApplication;
import io.authomator.api.builders.UserBuilder;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.domain.service.UserService;
import io.authomator.api.jwt.JwtService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class ChangePasswordControllerTest {
	
	
	private static final String USER_EMAIL = "testchange@local.tld";
	private static final String USER_PASSWORD = "somepassword";
	
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
	
	private MockMvc mockMvc;
	

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
    }
    
    @After
	public void cleanup(){
    	userRepository.deleteAll();
    }
    
    
    @Test
    public void testChangePasswordShouldChangePassword() throws Throwable {
    	
    	final String newPassword = "newPass";
    	
    	User user = userRepository.findByEmail(USER_EMAIL);
    	Assert.notNull(user);
    	
    	JsonWebSignature token = jwtService.getAccessToken(user);
    	
    	Map<String, String> req = new HashMap<>();
    	req.put("at", token.getCompactSerialization());
    	req.put("password", USER_PASSWORD);
    	req.put("newPassword", newPassword);
    	
    	mockMvc
    		.perform(
				post("/api/auth/change")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
    		.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.at").exists())
            .andExpect(jsonPath("$.rt").exists())
    		.andExpect(jsonPath("$.it").exists());
    	
    	User newPassUser = userService.login(USER_EMAIL, newPassword);
    	Assert.notNull(newPassUser);
    }
    
    @Test
    public void testChangePasswordShouldNotChangePasswordIfCurrentPasswordIsIncorrect() throws Throwable {
    	
    	final String newPassword = "newPass";
    	
    	User user = userRepository.findByEmail(USER_EMAIL);
    	Assert.notNull(user);
    	
    	JsonWebSignature token = jwtService.getAccessToken(user);
    	
    	Map<String, String> req = new HashMap<>();
    	req.put("at", token.getCompactSerialization());
    	req.put("password", "incorrectcurrent");
    	req.put("newPassword", newPassword);
    	
    	mockMvc
    		.perform(
				post("/api/auth/change")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
    		.andDo(print())
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
	        .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid credentials"))
	        .andExpect(jsonPath("$.fieldErrors[0].code").value("CredentialsError"));
    	
    	// password is unchanged
    	User newPassUser = userService.login(USER_EMAIL, USER_PASSWORD);
    	Assert.notNull(newPassUser);
    }
	
    @Test
    public void testChangePasswordShouldNotChangePasswordIUserWasRemoved() throws Throwable {
    	
    	final String newPassword = "newPass";
    	
    	User user = userRepository.findByEmail(USER_EMAIL);
    	Assert.notNull(user);
    	
    	JsonWebSignature token = jwtService.getAccessToken(user);
    	
    	Map<String, String> req = new HashMap<>();
    	req.put("at", token.getCompactSerialization());
    	req.put("password", USER_PASSWORD);
    	req.put("newPassword", newPassword);
    	
    	userRepository.deleteAll();
    	
    	mockMvc
    		.perform(
				post("/api/auth/change")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
    		.andDo(print())
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
	        .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid credentials"))
	        .andExpect(jsonPath("$.fieldErrors[0].code").value("CredentialsError"));
    	
    }
    
    
    @Test
    public void testChangePasswordShouldNotChangePasswordWithIncorrectAccessToken() throws Throwable {
    	
    	Map<String, String> req = new HashMap<>();
    	req.put("at", "someinvalidjwttoken");
    	req.put("password", USER_PASSWORD);
    	req.put("newPassword", "newPass");
    	
    	mockMvc
    		.perform(
				post("/api/auth/change")
				.accept(APPLICATION_JSON)
				.contentType(APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req))
			)
    		.andDo(print())
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("token"))
	        .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid jwt token"))
	        .andExpect(jsonPath("$.fieldErrors[0].code").value("InvalidToken"));
    	
    }

}