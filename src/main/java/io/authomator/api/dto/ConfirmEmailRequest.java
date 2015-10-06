package io.authomator.api.dto;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

public class ConfirmEmailRequest {

	@NotBlank
	@NotNull
	private String confirmEmailToken;

	public String getConfirmEmailToken() {
		return confirmEmailToken;
	}

	public void setConfirmEmailToken(String confirmEmailToken) {
		this.confirmEmailToken = confirmEmailToken;
	}
		
}
