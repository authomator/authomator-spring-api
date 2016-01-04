package io.authomator.api.domain.repository;

import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.repository.PagingAndSortingRepository;

import io.authomator.api.domain.entity.Context;

public interface ContextRepository extends PagingAndSortingRepository<Context, String> {
	public Set<Context> findByUsers(final ObjectId userId);
}
