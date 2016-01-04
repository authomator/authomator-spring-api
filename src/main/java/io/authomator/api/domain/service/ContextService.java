package io.authomator.api.domain.service;

import io.authomator.api.domain.entity.Context;
import io.authomator.api.domain.entity.User;
import io.authomator.api.exception.MissingDefaultContextException;

public interface ContextService {

	Context createContext(User owner, String name);

	Context createDefaultContext(User owner);
	
	Context getDefaultContext(User owner) throws MissingDefaultContextException;
}
