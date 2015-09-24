package io.authomator.api.dto;

import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

public class UpdatePasswordRequest {
	
	@NotBlank
	private String accessToken;
	
	@NotBlank
	@Length(min=6, max=32)
	private String oldPassword;
	
	@NotBlank
	@Length(min=6, max=32)
	private String newPassword;

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}
	
}
