package io.authomator.api.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import io.authomator.api.domain.entity.Context;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.repository.UserRepository;
import io.authomator.api.exception.EmailConfirmationNotEnabledException;
import io.authomator.api.exception.InvalidContextException;
import io.authomator.api.exception.InvalidCredentialsException;
import io.authomator.api.exception.RegistrationNotEnabledException;
import io.authomator.api.exception.UserAlreadyExistsException;
import io.authomator.api.exception.UserEmailConfirmedAlreadyException;
import io.authomator.api.exception.UserNotFoundException;

@Service
public class UserServiceImpl implements UserService {
	
	@Value("${io.authomator.api.registration.allow:false}")
	private boolean registrationEnabled = false;
	
	@Value("${io.authomator.api.registration.default.roles:}")
	private String[] defaultRoles;
	
	@Value("${io.authomator.api.verification.email.enabled:true}")
	private boolean verificationEmailEnabled = false;
			
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContextService contextService;
	
	
	/**
	 * Register/Signup a new user, returning the user entity
	 * 
	 * @param email
	 * @param password
	 * @return
	 * @throws RegistrationNotEnabledException 
	 * @throws RuntimeException
	 */
	@Override
	public User register(final String email, final String password) throws UserAlreadyExistsException, RegistrationNotEnabledException {
		
		if (!registrationEnabled) {
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
		userRepository.save(user);
		
		Context context = contextService.createContext(user, user.getEmail());
		user.getContexts().add(context);
		userRepository.save(user);
		
		return user;
	}
	
	
	/**
	 * signIn/Login a user, returning the user entity
	 * 
	 * @param email
	 * @param password
	 * @return
	 */
	@Override
	public User signIn(final String email, final String password) throws UserNotFoundException, InvalidCredentialsException {
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
	 * @throws InvalidContextException 
	 */
	@Override
	public User refresh(final String userId, final String contextId) throws UserNotFoundException, InvalidContextException{
		
		User user = userRepository.findOne(userId);
		
		if (user == null){
			throw new UserNotFoundException("mongoId: " + userId);
		}
		
		if (!contextService.hasContext(user, contextId)) {
			throw new InvalidContextException("User does not have access to the specified context or context not found");
		}
		
		return user;
	}
	
	//TODO: implement testing
	/**
	 * Return the user if this user can receive a forgot password email
	 * 
	 * @param email
	 * @return
	 * @throws UserNotFoundException
	 */
	@Override
	public User forgotPassword(final String email)  throws UserNotFoundException{
		User user = userRepository.findByEmail(email);
		
		if (user == null){
			throw new UserNotFoundException(email);
		}
		
		return user;		
	}
	
	//TODO: implement testing
	/**
	 * Reset the user password if this user can reset his password due to forgotten
	 * 
	 * @param id
	 * @param newPassword
	 * @return User
	 * @throws UserNotFoundException
	 */
	@Override
	public User resetPassword(final String id, final String newPassword) throws UserNotFoundException{
		
		User user = userRepository.findOne(id);
		
		if (user == null){
			throw new UserNotFoundException("mongoId: " + id);
		}
		
		user.setPassword(newPassword);
		return userRepository.save(user);
	}
	
	
	//TODO: implement testing
	/**
	 * Update password for a user by checking his current password before changing it
	 * 
	 * @param userId
	 * @param contextId
	 * @param currentPassword
	 * @param newPassword
	 * @return
	 * @throws UserNotFoundException
	 * @throws InvalidCredentialsException
	 * @throws InvalidContextException 
	 */
	@Override
	public User updatePassword(final String userId, final String contextId, final String currentPassword, final String newPassword) throws UserNotFoundException, InvalidCredentialsException, InvalidContextException{
		
		User user = userRepository.findOne(userId);
		
		if (user == null) {
			throw new UserNotFoundException("mongoId: " + userId);
		}
		
		if (!contextService.hasContext(user, contextId)){
			throw new InvalidContextException("User does not have access to the specified context or context not found");
		}
		
		if ( ! BCrypt.checkpw(currentPassword, user.getPassword())){
			throw new InvalidCredentialsException(user.getEmail(), currentPassword);
		};
		
		user.setPassword(newPassword);
		return userRepository.save(user);
		
	}
	
	
	/**
	 * Retrieve the user for email confirmation, taking in account all business logic if
	 * verification is possible for this account
	 * 
	 * @param id
	 * @return
	 * @throws EmailConfirmationNotEnabledException
	 * @throws UserNotFoundException
	 * @throws UserEmailConfirmedAlreadyException 
	 */
	@Override
	public User getUserForEmailConfirmation(final String id) throws EmailConfirmationNotEnabledException, UserNotFoundException, UserEmailConfirmedAlreadyException{
		
		if (!verificationEmailEnabled) {
			throw new EmailConfirmationNotEnabledException(id);
		}
		
		User user = userRepository.findOne(id);
		
		if (user == null) {
			throw new UserNotFoundException("mongoId: " + id);
		}
		
		if (user.getEmailVerified()){
			throw new UserEmailConfirmedAlreadyException(user.getEmail());
		}
		
		return user;
	}
	
	/**
	 * Confirm the email address for a user
	 * 
	 * @param id - the user id
	 * @return
	 * @throws UserNotFoundException
	 * @throws UserEmailConfirmedAlreadyException 
	 * @throws EmailConfirmationNotEnabledException 
	 */
	@Override
	public User confirmEmail(final String id) throws UserNotFoundException, EmailConfirmationNotEnabledException, UserEmailConfirmedAlreadyException {
		
		User user = getUserForEmailConfirmation(id);		
		user.setEmailVerified(true);
		return userRepository.save(user);
	}
	
	
	
}
