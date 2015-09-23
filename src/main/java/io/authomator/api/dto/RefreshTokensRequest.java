package io.authomator.api.dto;

import org.hibernate.validator.constraints.NotBlank;

public class RefreshTokensRequest {

	@NotBlank
	private String refreshToken;

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

}
