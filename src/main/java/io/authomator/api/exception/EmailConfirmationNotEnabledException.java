package io.authomator.api.exception;

public class EmailConfirmationNotEnabledException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public EmailConfirmationNotEnabledException(final String userInfo){
		super(String.format("Email confirmations are not enabled, user tried to confirm: %s", userInfo));
	}

	
}
