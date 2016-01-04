package io.authomator.api.domain.service;

import java.util.Set;

import io.authomator.api.domain.entity.Context;
import io.authomator.api.domain.entity.User;

public interface ContextService {

	Context createContext(User owner, String name);

	Set<Context> findByUser(User user);

}
