package io.authomator.api.domain.service;

import java.util.HashSet;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.authomator.api.domain.entity.Context;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.ContextRepository;
import io.authomator.api.exception.MissingDefaultContextException;

@Service
public class ContextServiceImpl implements ContextService {
	
	public static Logger LOGGER = Logger.getLogger(ContextServiceImpl.class);
	
	private final ContextRepository contextRepository;
	
	@Autowired
	public ContextServiceImpl(final ContextRepository contextRepository){
		this.contextRepository = contextRepository;
	}
	
	
	@Override
	public Context createContext(final User owner, final String name){
		Context context = new Context();
		context.setName(name);
		context.setOwner(owner);
		context.getUserRoles().put(owner.getId(), new HashSet<String>());
		return contextRepository.save(context);
	}


	@Override
	public Context createDefaultContext(User owner) {
		return createContext(owner, owner.getEmail());
	}


	@Override
	public Context getDefaultContext(User owner) throws MissingDefaultContextException {
		
		Optional<Context> ctx = owner.getContexts().stream()
			.filter(c -> c.getName().equals(owner.getEmail()))
			.findFirst();
		if (! ctx.isPresent() ) {
			throw new MissingDefaultContextException();
		}
		return ctx.get();
	}
}
