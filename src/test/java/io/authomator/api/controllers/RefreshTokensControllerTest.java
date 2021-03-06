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
import io.authomator.api.domain.entity.Context;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.ContextRepository;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.domain.service.ContextService;
import io.authomator.api.dto.TokenReply;
import io.authomator.api.jwt.JwtService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class RefreshTokensControllerTest {
	
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
	private ContextRepository contextRepository;
	
	@Autowired
	private ContextService contextService;
		
	@Autowired
	private JwtService jwtService;
		
    private MockMvc mockMvc;
    
    private User user;
    
    private Context ctx;

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
        
        ctx = contextService.createDefaultContext(user);
        user.getContexts().add(ctx);
        userRepository.save(user);
        
    }
    
    @After
    public void cleanup() {
    	userRepository.deleteAll();
    	contextRepository.deleteAll();
    }
    
    @Test
    public void refresh_with_valid_token() throws Exception {
    	    	
    	TokenReply tokens = jwtService.createTokensForUser(user, ctx);
    	
    	HashMap<String, String> req = new HashMap<>();
    	req.put("refreshToken", tokens.getRefreshToken());
    	
    	mockMvc
    		.perform(
				post("/refresh-tokens")
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
			.andExpect(jsonPath("$.fieldErrors[0].field").value("refreshToken"));
	    }
    
    
    @Test
    public void refresh_with_valid_token_but_nonexisting_user() throws Exception {
    	    	
    	TokenReply tokens = jwtService.createTokensForUser(user, ctx);
    	userRepository.delete(user);
    	
    	HashMap<String, String> req = new HashMap<>();
    	req.put("refreshToken", tokens.getRefreshToken());
    	
    	expectGenericValidationError(
			mockMvc
        		.perform(
    				post("/refresh-tokens")
    				.accept(APPLICATION_JSON)
    				.contentType(APPLICATION_JSON)
    				.content(new ObjectMapper().writeValueAsString(req))
    			)
        		.andDo(print())
		);    	
    }
    
    
    @Test
    public void refresh_with_valid_token_but_nonexisting_context() throws Exception {
    	    	
    	TokenReply tokens = jwtService.createTokensForUser(user, ctx);
    	contextRepository.delete(ctx);
    	
    	HashMap<String, String> req = new HashMap<>();
    	req.put("refreshToken", tokens.getRefreshToken());
    	
    	expectGenericValidationError(
			mockMvc
        		.perform(
    				post("/refresh-tokens")
    				.accept(APPLICATION_JSON)
    				.contentType(APPLICATION_JSON)
    				.content(new ObjectMapper().writeValueAsString(req))
    			)
        		.andDo(print())
		);    	
    }
    
    
    @Test
    public void refresh_with_valid_token_but_without_access_to_context() throws Exception {
    	    	
    	TokenReply tokens = jwtService.createTokensForUser(user, ctx);
    	user.getContexts().remove(ctx);
    	userRepository.save(user);
    	
    	HashMap<String, String> req = new HashMap<>();
    	req.put("refreshToken", tokens.getRefreshToken());
    	
    	expectGenericValidationError(
			mockMvc
        		.perform(
    				post("/refresh-tokens")
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
    	req.put("refreshToken","eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ");
    	
    	expectGenericValidationError(
	    	mockMvc
	    		.perform(
					post("/refresh-tokens")
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
    	req.put("refreshToken","ll.aa.bb");
    	
    	expectGenericValidationError(
			mockMvc
				.perform(
					post("/refresh-tokens")
					.accept(APPLICATION_JSON)
					.accept(APPLICATION_JSON)
					.contentType(APPLICATION_JSON)
					.content(new ObjectMapper().writeValueAsString(req))
				)
				.andDo(print())
		);
    }
}