package io.authomator.api.domain.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.crypto.bcrypt.BCrypt;


@Document(collection="users")
public class User {
	
	@Id
	private String id;					// Unique id. (Mongo ID: _id)
	
	@Indexed(unique=true)
	private String email;

	private String password;
	
	private List<String> roles;			// User roles

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = BCrypt.hashpw(password, BCrypt.gensalt(10));
	}

	public List<String> getRoles() {
		if (roles == null){
			return new ArrayList<String>();
		}
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	
	public void setRoles(String role) {
		if (roles == null) {
			roles = new ArrayList<String>();
		}
		if (!roles.contains(roles)) {
			roles.add(role);
		}
	}
	
}
