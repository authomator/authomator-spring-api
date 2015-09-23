package io.authomator.api.dto;

import org.hibernate.validator.constraints.NotBlank;

public class RefreshTokensRequest {

	@NotBlank
	private String rt;

	public String getRt() {
		return rt;
	}

	public void setRt(String rt) {
		this.rt = rt;
	}	
}
