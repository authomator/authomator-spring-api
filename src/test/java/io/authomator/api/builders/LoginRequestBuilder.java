package io.authomator.api.builders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.authomator.api.dto.LoginRequest;

public class LoginRequestBuilder {
	
	private LoginRequest request;
	
	public LoginRequestBuilder(){
		request = new LoginRequest();
	}
	
	public LoginRequestBuilder withEmail(String email){
		request.setEmail(email);
		return this;
	}
	
	public LoginRequestBuilder withPassword(String password){
		request.setPassword(password);
		return this;
	}
	
	public LoginRequest build(){
		return request;
	}
	
	public String buildAsJson() throws JsonProcessingException{
		return new ObjectMapper().writeValueAsString(request);
	}
}
