package io.authomator.api.dto;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

public class ChangePasswordRequest {
	
	@NotBlank
	private String at;
	
	@Length(min=6)
	private String password;
	
	@Length(min=6)
	private String newPassword;

	public String getAt() {
		return at;
	}

	public String getPassword() {
		return password;
	}

	public String getNewPassword() {
		return newPassword;
	}
}
