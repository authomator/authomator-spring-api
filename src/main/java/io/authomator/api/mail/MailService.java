package io.authomator.api.mail;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.authomator.api.exception.EmailTransportException;
import io.authomator.api.exception.NonSecureUrlException;
import io.authomator.api.exception.UnauthorizedDomainException;

@Service
public class MailService {

	/**
	 * Name of the url query param to use for passing the reset token
	 */
	private static final String URL_RESET_TOKEN_NAME = "reset-token";
	
	/**
	 * Name of the url query param to use for passing the confirm email token
	 */
	private static final String URL_CONFIRM_EMAIL_TOKEN_NAME = "confirm-email-token";
	
	/**
	 * Determine if mails sent are allowed to have non-https links.
	 */
	private boolean httpsOnly = true;
	
	/**
	 * List of allowed domains that can be used as endpoint for the reset/forgot
	 * password link
	 */
	private String[] allowedDomains = new String[]{"test.com"};
	
	
	/**
	 * Injected mail transport service
	 */
	private MailTransport transport;
	
	
	/**
	 * Constructor
	 * 
	 * @param httpsOnly
	 * @param allowedDomains
	 */
	@Autowired
	public MailService(
		@Value("${io.authomator.api.mail.httpsonly:true}") Boolean httpsOnly,
		@Value("${io.authomator.api.mail.alloweddomains:authomator.io}") String[] allowedDomains,
		MailTransport transport) {
		
		this.httpsOnly = httpsOnly;
		this.allowedDomains = allowedDomains;
		this.transport = transport;
	}
	
	/**
	 * Parse a string url into an URL taking in account the security rules on sending reset/forgot tokens
	 * via email
	 * 
	 * @param urlString
	 * @return java.net.URL
	 * 
	 * @throws MalformedURLException
	 * @throws NonSecureUrlException
	 * @throws UnauthorizedDomainException
	 */
	private URL parseUrl(final String urlString) throws MalformedURLException, NonSecureUrlException, UnauthorizedDomainException{
		
		final URL url = new URL(urlString);
				
		if (httpsOnly && ( ! url.getProtocol().equals("https"))) {
			throw new NonSecureUrlException(urlString);
		}
		
		if (! Arrays.asList(allowedDomains).contains(url.getHost()) ){
			throw new UnauthorizedDomainException(urlString);
		}
		
		return url;
	}
	
	/**
	 * Create a url with the forgot/reset token
	 * 
	 * @param url
	 * @param tokenName
	 * @param token
	 * @return
	 */
	private String createTokenUrl(URL url, final String tokenName, final String token) {
				
		StringBuilder sb = new StringBuilder();
		sb.append(url.getProtocol())
			.append("://")
			.append(url.getAuthority());
		if (url.getPath().equals("")) {
			sb.append("/");
		}
		else {
			sb.append(url.getPath());
		}
		sb.append("?");
		if (url.getQuery() != null){
			sb.append(url.getQuery());
			sb.append("&");			
		}
		sb.append(tokenName)
			.append("=")
			.append(token);
		if (url.getRef() != null){
			sb.append("#");
			sb.append(url.getRef());
		}
		
		return sb.toString();
	}
	
	
	/**
	 * Send the forgot password email with reset link
	 * 
	 * @param email - email address to send the email to
	 * @param urlString - the URL to point to when sending the token
	 * @param forgotToken - the JWT token that authorizes a reset password
	 * 
	 * @throws MalformedURLException
	 * @throws NonSecureUrlException
	 * @throws UnauthorizedDomainException
	 * @throws EmailTransportException 
	 */
	public Boolean sendForgotPasswordMail(final String email, final String urlString, final String forgotToken) throws MalformedURLException, NonSecureUrlException, UnauthorizedDomainException, EmailTransportException{
		final String forgotUrl = createTokenUrl(parseUrl(urlString), URL_RESET_TOKEN_NAME, forgotToken);
		return transport.sendForgotEmail(email, forgotUrl);
	}
	
	
	/**
	 * Send the confirm email email with confirmation link
	 * 
	 * @param email - email address to send the email to
	 * @param urlString - the URL to point to when sending the token
	 * @param forgotToken - the JWT token that authorizes a confirm email
	 * 
	 * @throws MalformedURLException
	 * @throws NonSecureUrlException
	 * @throws UnauthorizedDomainException
	 * @throws EmailTransportException 
	 */
	public Boolean sendConfirmEmailMail(final String email, final String urlString, final String confirmToken) throws MalformedURLException, NonSecureUrlException, UnauthorizedDomainException, EmailTransportException{
		final String confirmUrl = createTokenUrl(parseUrl(urlString), URL_CONFIRM_EMAIL_TOKEN_NAME, confirmToken);
		return transport.sendConfirmEmailEmail(email, confirmUrl);
	}
}
