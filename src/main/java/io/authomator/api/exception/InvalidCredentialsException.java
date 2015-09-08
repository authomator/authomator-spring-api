package io.authomator.api.exception;

public class InvalidCredentialsException extends Exception {
	
	private static final long serialVersionUID = -1186447871468554163L;

	private String email;
	private String password;
	
	public InvalidCredentialsException(final String email, final String password){
		super("Invalid credentials");
		this.email = email;
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}	
}
