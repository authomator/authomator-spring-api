package io.authomator.api.controllers;

import static io.authomator.api.TestUtil.APPLICATION_JSON;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.authomator.api.AuthomatorApiApplication;
import io.authomator.api.builders.UserBuilder;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.dto.TokenReply;
import io.authomator.api.jwt.JwtService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class RefreshControllerTest {
	
	private static final String USER_EMAIL = "test@local.tld";
	private static final String USER_PASSWORD = "test@local.tld";
	private static final String[] USER_ROLES = new String[]{"USER", "ADMIN"};
	
	@Autowired
	private FilterChainProxy filterChainProxy;
	
	@Autowired
    private WebApplicationContext webApplicationContext;

	@Autowired
	private UserRepository userRepository;
		
	@Autowired
	private JwtService jwtService;
		
    private MockMvc mockMvc;
    
    private User user;

    @Before
    public void setup() {
    	mockMvc = MockMvcBuilders
    			.webAppContextSetup(webApplicationContext)
    			.addFilter(filterChainProxy)
    			.build();
        
        userRepository.deleteAll();
        User u = new UserBuilder()
        		.withEmail(USER_EMAIL)
        		.withPassword(USER_PASSWORD)
        		.withRoles(USER_ROLES)
        		.build();
        user = userRepository.save(u);
    }
    
    @After
    public void cleanup() {
    	userRepository.deleteAll();
    }
    
    @Test
    public void refresh_with_valid_token() throws Exception {
    	    	
    	TokenReply tokens = jwtService.createTokensForUser(user);
    	
    	HashMap<String, String> req = new HashMap<>();
    	req.put("rt", tokens.getRefreshToken());
    	
    	mockMvc
    		.perform(
				post("/api/auth/refresh")
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
    }
    
    private void expectGenericValidationError(ResultActions act) throws Exception{
    	act
	    	.andExpect(status().isUnprocessableEntity())
	        .andExpect(content().contentType(APPLICATION_JSON))
	        .andExpect(jsonPath("$.message").value("Validation Failed"))
	        .andExpect(jsonPath("$.code").value("ValidationFailed"))
			.andExpect(jsonPath("$.fieldErrors", hasSize(1)))
			.andExpect(jsonPath("$.fieldErrors[0].code").value("InvalidToken"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid jwt token"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("rt"));
	    }
    
    
    @Test
    public void refresh_with_valid_token_but_nonexisting_user() throws Exception {
    	    	
    	TokenReply tokens = jwtService.createTokensForUser(user);
    	userRepository.delete(user);
    	
    	HashMap<String, String> req = new HashMap<>();
    	req.put("rt", tokens.getRefreshToken());
    	
    	expectGenericValidationError(
			mockMvc
        		.perform(
    				post("/api/auth/refresh")
    				.accept(APPLICATION_JSON)
    				.contentType(APPLICATION_JSON)
    				.content(new ObjectMapper().writeValueAsString(req))
    			)
        		.andDo(print())
		);    	
    }
    
    @Test
    public void refresh_with_invalid_jwt_token() throws Exception {
    	    	
    	HashMap<String, String> req = new HashMap<>();
    	req.put("rt","eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ");
    	
    	expectGenericValidationError(
	    	mockMvc
	    		.perform(
					post("/api/auth/refresh")
					.accept(APPLICATION_JSON)
					.contentType(APPLICATION_JSON)
					.content(new ObjectMapper().writeValueAsString(req))
				)
	    		.andDo(print())
		);
    }
    
    @Test
    public void refresh_with_invalid_token() throws Exception {
    	
    	HashMap<String, String> req = new HashMap<>();
    	req.put("rt","ll.aa.bb");
    	
    	expectGenericValidationError(
			mockMvc
				.perform(
					post("/api/auth/refresh")
					.accept(APPLICATION_JSON)
					.accept(APPLICATION_JSON)
					.contentType(APPLICATION_JSON)
					.content(new ObjectMapper().writeValueAsString(req))
				)
				.andDo(print())
		);
    }
    

}
