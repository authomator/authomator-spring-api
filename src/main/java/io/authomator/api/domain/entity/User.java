package io.authomator.api.domain.entity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.crypto.bcrypt.BCrypt;


@Document(collection="users")
@TypeAlias(value="user")
public class User {
	
	@Id
	private String id;					// Unique id. (Mongo ID: _id)
	
	@Indexed(unique=true)
	private String email;

	private Boolean emailVerified = false;
	
	private String password;
	
	private List<String> roles;			// User roles

	@DBRef
	private Set<Context> contexts = new LinkedHashSet<>();
	
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

	public Boolean getEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(Boolean emailVerified) {
		this.emailVerified = emailVerified;
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

	public Set<Context> getContexts() {
		return contexts;
	}

	public void setContexts(Set<Context> contexts) {
		this.contexts = contexts;
	}		
}
