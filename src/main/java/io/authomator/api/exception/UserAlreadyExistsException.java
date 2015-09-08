package io.authomator.api.exception;

public class UserAlreadyExistsException extends Exception {

	private static final long serialVersionUID = 3937462738867085761L;

	private String email;
	
	public UserAlreadyExistsException(final String email){
		super("The user aleady exists");
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
}
