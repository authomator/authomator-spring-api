package io.authomator.api.mail;

import io.authomator.api.exception.EmailTransportException;

public interface MailTransport {

	/**
	 * Send the forgot password email
	 * 
	 * @param email - email address to send the email to
	 * @param url - the reset password URL
	 * @return
	 * @throws EmailTransportException
	 */
	public Boolean sendForgotEmail(final String email, final String urlString) throws EmailTransportException;
	
	/**
	 * Send the confirm your email email
	 * 
	 * @param email - email address to send the email to
	 * @param urlString - the confirm your email URL
	 * @return
	 * @throws EmailTransportException
	 */
	public Boolean sendConfirmEmailEmail(final String email, final String urlString) throws EmailTransportException;
}
