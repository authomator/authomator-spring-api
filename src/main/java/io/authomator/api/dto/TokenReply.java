package io.authomator.api.dto;

public class TokenReply {

	/**
	 * Access token
	 */
	private String accessToken;
	
	/**
	 * Identity token
	 */
	private String identityToken;
	
	/**
	 * Refresh token
	 */
	private String refreshToken;

	
	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getIdentityToken() {
		return identityToken;
	}

	public void setIdentityToken(String identityToken) {
		this.identityToken = identityToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
