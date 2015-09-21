package io.authomator.api.builders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.authomator.api.dto.ForgotPasswordRequest;

public class ForgotPasswordRequestBuilder {
	
	private ForgotPasswordRequest request = new ForgotPasswordRequest();
	
	public ForgotPasswordRequestBuilder withEmail(String email){
		request.setEmail(email);
		return this;
	}
	
	public ForgotPasswordRequestBuilder withUrl(String url){
		request.setUrl(url);
		return this;
	}
	
	public String buildAsJson() throws JsonProcessingException{
		return new ObjectMapper().writeValueAsString(request);
	}
}
