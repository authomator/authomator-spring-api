package io.authomator.api.domain.service;

import io.authomator.api.domain.entity.User;
import io.authomator.api.exception.EmailConfirmationNotEnabledException;
import io.authomator.api.exception.InvalidContextException;
import io.authomator.api.exception.InvalidCredentialsException;
import io.authomator.api.exception.RegistrationNotEnabledException;
import io.authomator.api.exception.UserAlreadyExistsException;
import io.authomator.api.exception.UserEmailConfirmedAlreadyException;
import io.authomator.api.exception.UserNotFoundException;

public interface UserService {

	User register(String email, String password) throws UserAlreadyExistsException, RegistrationNotEnabledException;

	User signIn(String email, String password) throws UserNotFoundException, InvalidCredentialsException;

	User refresh(String userId, String contextId) throws UserNotFoundException, InvalidContextException;

	User forgotPassword(String email) throws UserNotFoundException;

	User resetPassword(String id, String newPassword) throws UserNotFoundException;
	
	User updatePassword(String userId, String contextId, String currentPassword, String newPassword)
			throws UserNotFoundException, InvalidCredentialsException, InvalidContextException;
	
	User getUserForEmailConfirmation(String id)
			throws EmailConfirmationNotEnabledException, UserNotFoundException, UserEmailConfirmedAlreadyException;

	User confirmEmail(String id)
			throws UserNotFoundException, EmailConfirmationNotEnabledException, UserEmailConfirmedAlreadyException;

}
