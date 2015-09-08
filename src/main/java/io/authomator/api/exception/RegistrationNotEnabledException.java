package io.authomator.api.exception;

public class RegistrationNotEnabledException extends Exception {

	private static final long serialVersionUID = 6686094422206780826L;
	
	public RegistrationNotEnabledException(){
		super("User registration is not available");
	}
}
