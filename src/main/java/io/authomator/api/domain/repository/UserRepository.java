package io.authomator.api.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import io.authomator.api.domain.entity.User;

public interface UserRepository extends PagingAndSortingRepository<User, String>{
	public User findByEmail(String email);
}
