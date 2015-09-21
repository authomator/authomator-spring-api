package io.authomator.api.controllers;

import static io.authomator.api.TestUtil.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
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
import org.springframework.web.context.WebApplicationContext;

import io.authomator.api.AuthomatorApiApplication;
import io.authomator.api.builders.ForgotPasswordRequestBuilder;
import io.authomator.api.builders.UserBuilder;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.mail.MailService;
import io.authomator.api.mail.MailTransport;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class ForgotControllerTest {

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
	private MailService mailService;
	
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
    
    
    @Test
    public void sendMailResetToken_sends_mail() throws Exception {
    	    	
    	String req = new ForgotPasswordRequestBuilder()
    			.withEmail(USER_EMAIL)
    			.withUrl(RESET_URL)
    			.buildAsJson();
    	    	
    	mockMvc
    		.perform(
				post("/api/auth/forgot/mail")
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
				post("/api/auth/forgot/mail")
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
	        .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid email or non existing"))
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
				post("/api/auth/forgot/mail")
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
    
	
}
