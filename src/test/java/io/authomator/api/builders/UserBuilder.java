package io.authomator.api.builders;

import io.authomator.api.domain.entity.User;

public class UserBuilder {

	private User user;
	
	public UserBuilder(){
		user = new User();
	}
	
	public UserBuilder withId(String id){
		user.setId(id);
		return this;
	}
	
	public UserBuilder withEmail(String email){
		user.setEmail(email);
		return this;
	}
	
	public UserBuilder withPassword(String password){
		user.setPassword(password);
		return this;
	}
	
	public UserBuilder withRoles(String... roles){
		for(String role : roles){
			user.setRoles(role);
		}
		return this;
	}
	
	
	public User build(){
		return user;
	}	
}
