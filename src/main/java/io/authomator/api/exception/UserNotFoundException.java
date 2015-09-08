package io.authomator.api.exception;

public class UserNotFoundException extends Exception {

	private static final long serialVersionUID = -8248171120606831407L;

	private String email;
	
	public UserNotFoundException(final String email) {
		super("User not found");
		this.email = email;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
