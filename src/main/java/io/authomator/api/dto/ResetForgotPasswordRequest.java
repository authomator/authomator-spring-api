package io.authomator.api.dto;

import org.hibernate.validator.constraints.Length;

public class ResetForgotPasswordRequest {

	@Length(min=6)
	private String password;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
		
}
