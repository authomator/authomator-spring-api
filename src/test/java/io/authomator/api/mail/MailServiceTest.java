package io.authomator.api.mail;

import static org.mockito.Mockito.*;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import io.authomator.api.AuthomatorApiApplication;
import io.authomator.api.exception.NonSecureUrlException;
import io.authomator.api.exception.UnauthorizedDomainException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class MailServiceTest {
	
	@Autowired
	MailService mailService;
	
			
	@Test(expected=NonSecureUrlException.class)
	public void parseUrl_checks_if_non_https_is_allowed() throws Throwable{
		try {
			ReflectionTestUtils.invokeMethod(mailService, "parseUrl", "http://authomator.io");
		}
		catch (UndeclaredThrowableException ex){
			throw ex.getCause();
		}
	}
	
	@Test
	public void validateForgotUrl_checks_if_domain_is_allowed() throws Throwable{
		URL url = ReflectionTestUtils.invokeMethod(mailService, "parseUrl", "https://authomator.io");
		Assert.assertNotNull(url);
	}
		
	@Test(expected=UnauthorizedDomainException.class)
	public void validateForgotUrl_checks_if_domain_is_not_allowed() throws Throwable{
		try {
			ReflectionTestUtils.invokeMethod(mailService, "parseUrl", "https://drevil.me");
		}
		catch (UndeclaredThrowableException ex){
			throw ex.getCause();
		}
	}
	
	@Test
	public void test_createTokenUrl_leaves_intact(){
		URL url = ReflectionTestUtils.invokeMethod(mailService, "parseUrl", "https://stefan:weird@authomator.io:8443/t/./index.html?test=me#pageSection");
		String createUrl = ReflectionTestUtils.invokeMethod(mailService, "createTokenUrl", url, "my-token-test", "testje");
		Assert.assertNotNull(createUrl);		
		Assert.assertEquals(createUrl, "https://stefan:weird@authomator.io:8443/t/./index.html?test=me&my-token-test=testje#pageSection");
	}
	
	@Test
	public void test_createTokenUrl_works_with_empty_path(){
		URL url = ReflectionTestUtils.invokeMethod(mailService, "parseUrl", "https://authomator.io#test");
		String createUrl = ReflectionTestUtils.invokeMethod(mailService, "createTokenUrl", url, "perhaps-a-reset-token", "tokendatadatatata");
		Assert.assertNotNull(createUrl);
		Assert.assertEquals(createUrl, "https://authomator.io/?perhaps-a-reset-token=tokendatadatatata#test");
	}
	
	
	@Test
	public void test_send_forgot_calls_transport() throws Throwable {
		
		MailTransport mock = Mockito.mock(MailTransport.class);
		when(mock.sendForgotEmail("test@local.local", "https://authomator.io/test")).thenReturn(true);
		ReflectionTestUtils.setField(mailService, "transport", mock);
		
		mailService.sendForgotPasswordMail("test@local.local", "https://authomator.io/", "test");
		
		verify(mock, times(1)).sendForgotEmail("test@local.local", "https://authomator.io/?reset-token=test");
	}		
	
	
	@Test
	public void test_send_confirm_calls_transport() throws Throwable {
		
		MailTransport mock = Mockito.mock(MailTransport.class);
		when(mock.sendConfirmEmailEmail("test@local.local", "https://authomator.io/test")).thenReturn(true);
		ReflectionTestUtils.setField(mailService, "transport", mock);
		
		mailService.sendConfirmEmailMail("test@local.local", "https://authomator.io/", "test");
		
		verify(mock, times(1)).sendForgotEmail("test@local.local", "https://authomator.io/?confirm-email-token=test");
	}
	
	
}
