package io.authomator.api.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import io.authomator.api.domain.entity.Context;

public interface ContextRepository extends PagingAndSortingRepository<Context, String> {

	public Context findByName(final String name);
}
