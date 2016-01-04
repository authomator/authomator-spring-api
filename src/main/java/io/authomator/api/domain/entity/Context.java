package io.authomator.api.domain.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="contexts")
@TypeAlias(value="context")
public class Context {

	@Id
	private String id;
	
	private String name;
	
	@DBRef
	private User owner;
	
	private Set<ObjectId> users = new HashSet<>();
	
	private HashMap<String, Set<String>> userRoles = new HashMap<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public Set<ObjectId> getUsers() {
		return users;
	}

	public void setUsers(Set<ObjectId> users) {
		this.users = users;
	}

	public HashMap<String, Set<String>> getUserRoles() {
		return userRoles;
	}

	public void setUserRoles(HashMap<String, Set<String>> userRoles) {
		this.userRoles = userRoles;
	}
	
}