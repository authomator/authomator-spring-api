package io.authomator.api.dto;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;

public class LoginRequest {
	
	@Email
	private String email;
	
	@Length(min=6)
	private String password;

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
