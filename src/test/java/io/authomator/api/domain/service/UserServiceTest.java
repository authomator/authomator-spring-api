package io.authomator.api.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import io.authomator.api.AuthomatorApiApplication;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.exception.InvalidCredentialsException;
import io.authomator.api.exception.RegistrationNotEnabledException;
import io.authomator.api.exception.UserAlreadyExistsException;
import io.authomator.api.exception.UserNotFoundException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class UserServiceTest {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private UserService userService;
	
	@Value("${io.authomator.api.signup.allow:false}")
	private boolean registrationStatus;
	
	@Value("${io.authomator.api.signup.default.roles:}")
	private String[] defaultRoles;
	
	
	@After
	public void cleanup(){
		ReflectionTestUtils.setField(userService, "signupEnabled", registrationStatus);
		ReflectionTestUtils.setField(userService, "defaultRoles", defaultRoles);
		userRepository.deleteAll();
	}
	
	//--------------------------------------------------------------------------
	//  General functionality & Config tests
	//--------------------------------------------------------------------------
	
	@Test
	public void userRegistrationConfig(){
		assertFalse ("The registration status should reflect the io.authomator.api.signup.allow property",
			(boolean) ReflectionTestUtils.getField(userService, "signupEnabled")
		);
	}
	
	@Test
	public void userRegistrationDefaultDisabled(){
		assertFalse ("The registration should be disabled by default",
			(boolean) ReflectionTestUtils.getField(userService, "signupEnabled")
		);
	}
	
	//--------------------------------------------------------------------------
	//  .register()
	//--------------------------------------------------------------------------
	
	@Test
	public void signUpShouldSignupUsers() throws UserAlreadyExistsException, RegistrationNotEnabledException{
		ReflectionTestUtils.setField(userService, "signupEnabled", true);
		User user = userService.register("sometest@domain.tld", "test");
		assertNotNull(user);
	}
	

	@Test
	public void signUpShouldSignupUsersAndAddDefaultGroups() throws UserAlreadyExistsException, RegistrationNotEnabledException{
		ReflectionTestUtils.setField(userService, "signupEnabled", true);
		ReflectionTestUtils.setField(userService, "defaultRoles", new String[]{"ADMIN", "TESTER"});		
		User user = userService.register("sometest@domain.tld", "test");
		assertFalse(user.getRoles().isEmpty());
		assertTrue(user.getRoles().contains("ADMIN"));	
		assertTrue(user.getRoles().contains("TESTER"));
	}

	
	@Test(expected=RegistrationNotEnabledException.class)
	public void signUpShouldNotSignupUsersIfSignupIsDisabled() throws UserAlreadyExistsException, RegistrationNotEnabledException{
		ReflectionTestUtils.setField(userService, "signupEnabled", false);
		userService.register("sometest@domain.tld", "test");
	}
	
	@Test(expected=UserAlreadyExistsException.class)
	public void signUpShouldNotCreateDuplicateUsers() throws UserAlreadyExistsException, RegistrationNotEnabledException{
		ReflectionTestUtils.setField(userService, "signupEnabled", true);
		
		User user = new User();
		user.setEmail("sometest@domain.tld");
		user.setPassword("somepassword");
		assertNotNull(userRepository.save(user));
		
		userService.register("sometest@domain.tld", "someotherpass");		
	}
	
	//--------------------------------------------------------------------------
	//  .signIn()
	//--------------------------------------------------------------------------

	@Test
	public void loginShouldloginCorrectCredentials() throws UserNotFoundException, InvalidCredentialsException, UserAlreadyExistsException, RegistrationNotEnabledException{
		ReflectionTestUtils.setField(userService, "signupEnabled", true);
		assertNotNull(
			userService.register("sometest@domain.tld", "yeahright")
		);
		User user = userService.signIn("sometest@domain.tld", "yeahright");
		assertNotNull(user);
		assertEquals("sometest@domain.tld", user.getEmail());
		assertNotNull(user.getId());
	}
	
	@Test(expected=UserNotFoundException.class)
	public void loginThrowsUserNotFoundException() throws UserNotFoundException, InvalidCredentialsException{
		userService.signIn("sometest@domain.tld", "test");
	}

	@Test(expected=InvalidCredentialsException.class)
	public void loginThrowsInvalidCredentialsException() throws UserNotFoundException, InvalidCredentialsException, UserAlreadyExistsException, RegistrationNotEnabledException{
		ReflectionTestUtils.setField(userService, "signupEnabled", true);
		assertNotNull(
				userService.register("sometest@domain.tld", "mypassword")
			);
		userService.signIn("sometest@domain.tld", "notmypassword");
	}

	//--------------------------------------------------------------------------
	//  .refresh()
	//--------------------------------------------------------------------------
	@Test
	public void refreshReturnsAUser() throws UserAlreadyExistsException, RegistrationNotEnabledException, UserNotFoundException{
		ReflectionTestUtils.setField(userService, "signupEnabled", true);
		User user = userService.register("sometest@domain.tld", "yeahright");
		assertNotNull(user);
		User refreshedUser = userService.refresh(user.getId());
		assertNotNull(refreshedUser);
		assertEquals(user.getEmail(), refreshedUser.getEmail());
	}
	
	@Test(expected=UserNotFoundException.class)
	public void refreshThrowsUserNotFoundForUnknownUsers() throws UserNotFoundException{		
		userService.refresh("nonexisting");
	}

	
}
