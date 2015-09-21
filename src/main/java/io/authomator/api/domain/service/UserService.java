package io.authomator.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.exception.InvalidCredentialsException;
import io.authomator.api.exception.RegistrationNotEnabledException;
import io.authomator.api.exception.UserAlreadyExistsException;
import io.authomator.api.exception.UserNotFoundException;

@Service
public class UserService implements IUserService {
	
	@Value("${io.authomator.api.signup.allow:false}")
	private boolean signupEnabled = false;
	
	@Value("${io.authomator.api.signup.default.roles:}")
	private String[] defaultRoles;
	
	@Autowired
	private UserRepository userRepository;
	
	
	/**
	 * Signup a new user, returning the user entity
	 * 
	 * @param email
	 * @param password
	 * @return
	 * @throws RegistrationNotEnabledException 
	 * @throws RuntimeException
	 */
	public User signUp(final String email, final String password) throws UserAlreadyExistsException, RegistrationNotEnabledException {
		
		if (!signupEnabled) {
			throw new RegistrationNotEnabledException();
		}
		
		User existing = userRepository.findByEmail(email);
		if (existing != null) {
			throw new UserAlreadyExistsException(email);
		}
		
		User user = new User();
		user.setEmail(email);
		user.setPassword(password);
		
		for(String role: defaultRoles) {
			user.setRoles(role);
		}
		
		return userRepository.save(user);
	}
	
	
	/**
	 * Login a user, returning the user entity
	 * 
	 * @param email
	 * @param password
	 * @return
	 */
	public User login(final String email, final String password) throws UserNotFoundException, InvalidCredentialsException {
		User user = userRepository.findByEmail(email);
		if (user == null){
			throw new UserNotFoundException(email);
		}
		
		if ( ! BCrypt.checkpw(password, user.getPassword())){
			throw new InvalidCredentialsException(email, password);
		};
		
		return user;
	}
	
	
	//TODO: implement testing
	/**
	 * Refresh a user, returning the user if it can be refreshed
	 * 
	 * @param id
	 * @return User
	 * @throws UserNotFoundException
	 */
	public User refresh(final String id) throws UserNotFoundException{
		
		User user = userRepository.findOne(id);
		
		if (user == null){
			throw new UserNotFoundException("mongoId: " + id);
		}
		
		return user;
	}
	
	
}
