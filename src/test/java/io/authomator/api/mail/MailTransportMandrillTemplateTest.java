package io.authomator.api.mail;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.Assert;

import io.authomator.api.AuthomatorApiApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = AuthomatorApiApplication.class)
@WebAppConfiguration
public class MailTransportMandrillTemplateTest {

	@Autowired
	private MailTransport mandrillTemplate;
	
	@Test
	public void test_if_injected(){
		Assert.notNull(mandrillTemplate);
	}
	
	@Test
	public void test_if_MailTransportMandrillTemplateInstance(){
		Assert.isInstanceOf(MailTranportMandrillTemplate.class, mandrillTemplate);
	}
	
	/*
	@Test
	public void test_if_sends_mail() throws Throwable {
		Assert.isTrue(
			mandrillTemplate.sendForgotEmail("email", "http://some.url.local/")
		);
	}
	*/
}
