package io.authomator.api.dto;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.URL;

public class ForgotPasswordRequest {

	@Email
	private String email;
	
	@URL
	private String url;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
		
}
