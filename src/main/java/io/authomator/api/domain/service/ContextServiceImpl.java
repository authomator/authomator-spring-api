package io.authomator.api.domain.service;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.authomator.api.domain.entity.Context;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.ContextRepository;

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
		context.getUsers().add(new ObjectId(owner.getId()));
		context.getUserRoles().put(owner.getId(), new HashSet<String>());
		return contextRepository.save(context);
	}
	
	
	@Override
	public Set<Context> findByUser(final User user){
		return contextRepository.findByUsers(new ObjectId(user.getId()));
	}

}
