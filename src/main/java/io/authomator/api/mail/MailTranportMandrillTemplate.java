package io.authomator.api.mail;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.microtripit.mandrillapp.lutung.MandrillApi;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.MergeVar;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.MergeVarBucket;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.Recipient;
import com.microtripit.mandrillapp.lutung.view.MandrillMessageStatus;

import io.authomator.api.exception.EmailTransportException;

@ConditionalOnProperty(name="io.authomator.api.mandrill.key")
@Service("MailTransport")
public class MailTranportMandrillTemplate implements MailTransport {
	
	private final String mandrillKey;
	
	private final String mandrillForgotPasswordTemplate;
	
	private final String mandrillConfirmEmailTemplate;
	
	private final MandrillApi mandrillApi;
		
	@Autowired
	public MailTranportMandrillTemplate(
			@Value("${io.authomator.api.mailtransport.mandrill.key}") String mandrillKey,
			@Value("${io.authomator.api.mailtransport.mandrill.template.forgotpassword:forgot-password}") String mandrillForgotPasswordTemplate,
			@Value("${io.authomator.api.mailtransport.mandrill.template.confirmemail:confirm-email}") String mandrillConfirmEmailTemplate ) {
		this.mandrillKey = mandrillKey;
		this.mandrillForgotPasswordTemplate = mandrillForgotPasswordTemplate;
		this.mandrillConfirmEmailTemplate = mandrillConfirmEmailTemplate;
		this.mandrillApi = new MandrillApi(this.mandrillKey);
	}
	
	
	/**
	 * Create a default MandrillMessage with defaults and email set
	 * 
	 * @param email
	 * @return
	 */
	private MandrillMessage createMandrillMessage(final String email) {
		
		MandrillMessage m = new MandrillMessage();
		ArrayList<Recipient> recipients = new ArrayList<Recipient>();
		Recipient recipient = new Recipient();
		recipient.setEmail(email);
		recipients.add(recipient);
		m.setTo(recipients);
		m.setTrackClicks(false);
		m.setViewContentLink(false);
		
		return m;
	}

	
	@Override
	public Boolean sendForgotEmail(String email, String urlString) throws EmailTransportException {
						
		MandrillMessage m = createMandrillMessage(email);
		
		List<MergeVarBucket> mergeVarBuckets = new LinkedList<>();
		MergeVarBucket mergeVarBucket = new MergeVarBucket();
		mergeVarBucket.setRcpt(email);
		
		MergeVar resetUrl = new MergeVar();
		resetUrl.setName("resetUrl");
		resetUrl.setContent(urlString);
		
		List<MergeVar> mergeVars = new ArrayList<>();
		mergeVars.add(resetUrl);
				
		mergeVarBucket.setVars(mergeVars.toArray(new MergeVar[0]));
		mergeVarBuckets.add(mergeVarBucket);
		m.setMergeVars(mergeVarBuckets);
		
		MandrillMessageStatus[] status = null;
		
		try {
			status =  mandrillApi.messages().sendTemplate(mandrillForgotPasswordTemplate, null, m, false);
		} catch (Exception e) {
			throw new EmailTransportException("An error occured while sending email to " + email, e);
		}
		
		if (status != null) {
			return status[0].getStatus().equals("sent");
		}
		return false;		
	}
		
	@Override
	public Boolean sendConfirmEmailEmail(String email, String urlString) throws EmailTransportException {
						
		MandrillMessage m = createMandrillMessage(email);
		
		List<MergeVarBucket> mergeVarBuckets = new LinkedList<>();
		MergeVarBucket mergeVarBucket = new MergeVarBucket();
		mergeVarBucket.setRcpt(email);
		
		MergeVar resetUrl = new MergeVar();
		resetUrl.setName("confirmEmailUrl");
		resetUrl.setContent(urlString);
		
		List<MergeVar> mergeVars = new ArrayList<>();
		mergeVars.add(resetUrl);
				
		mergeVarBucket.setVars(mergeVars.toArray(new MergeVar[0]));
		mergeVarBuckets.add(mergeVarBucket);
		m.setMergeVars(mergeVarBuckets);
		
		MandrillMessageStatus[] status = null;
		
		try {
			status =  mandrillApi.messages().sendTemplate(mandrillConfirmEmailTemplate, null, m, false);
		} catch (Exception e) {
			throw new EmailTransportException("An error occured while sending email to " + email, e);
		}
		
		if (status != null) {
			return status[0].getStatus().equals("sent");
		}
		return false;		
	}
}
