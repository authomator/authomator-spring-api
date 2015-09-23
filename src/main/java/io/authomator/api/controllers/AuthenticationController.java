package io.authomator.api.controllers;

import javax.validation.Valid;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.service.UserService;
import io.authomator.api.dto.GenericError;
import io.authomator.api.dto.LoginRequest;
import io.authomator.api.dto.TokenReply;
import io.authomator.api.dto.ValidationError;
import io.authomator.api.exception.InvalidCredentialsException;
import io.authomator.api.exception.RegistrationNotEnabledException;
import io.authomator.api.exception.UserAlreadyExistsException;
import io.authomator.api.exception.UserNotFoundException;
import io.authomator.api.jwt.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

	@Autowired
	UserService userService;
	
	@Autowired
	JwtService jwtService;
	
	private static final Logger logger = Logger.getLogger(AuthenticationController.class);
	
	/*
	 * Controllers
	 * ------------------------------------------------------------------------------------------
	 */
	
	@RequestMapping(path="/sign-in", method=RequestMethod.POST)
	public TokenReply login(@Valid @RequestBody LoginRequest loginRequest) throws JoseException, UserNotFoundException, InvalidCredentialsException {		
		User user = userService.signIn(loginRequest.getEmail(), loginRequest.getPassword());
		return jwtService.createTokensForUser(user);
	}
	
	@RequestMapping(path="/register", method=RequestMethod.POST)
	public TokenReply signup(@Valid @RequestBody LoginRequest loginRequest) throws JoseException, UserAlreadyExistsException, RegistrationNotEnabledException {		
		User user = userService.register(loginRequest.getEmail(), loginRequest.getPassword());
		return jwtService.createTokensForUser(user);
	}
	
	
	/*
	 * Exception handling
	 * ------------------------------------------------------------------------------------------
	 */
	
	private ValidationError createInvalidCredentialDto() {
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("email", "Invalid email or password", "CredentialsError");
		validationError.addFieldError("password", "Invalid email or password", "CredentialsError");
		return validationError;
	}
	
	
	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError invalidCredentials(InvalidCredentialsException ex) {
		logger.log(Level.WARN, String.format("Invalid credentials received for: %s", ex.getEmail()));
		return createInvalidCredentialDto();
	}
	
	@ExceptionHandler(UserNotFoundException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError userNotFound(UserNotFoundException ex) {
		logger.log(Level.WARN, String.format("Unknown user tried to login: %s", ex.getEmail()));
		return createInvalidCredentialDto();
	}
	
	@ExceptionHandler(UserAlreadyExistsException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError userAlreadyExists(UserAlreadyExistsException ex) {
		logger.log(Level.WARN, String.format("User tried to register an already existing account: %s", ex.getEmail()));
		return createInvalidCredentialDto();
	}
	
	@ExceptionHandler(RegistrationNotEnabledException.class)
	@ResponseStatus(value=HttpStatus.FORBIDDEN)
	public GenericError registrationNotEnabled(RegistrationNotEnabledException ex) {
		logger.log(Level.WARN, String.format("A register request was received, but registrations are disabled"));		
		return new GenericError(new Exception("Registration is not allowed"), "RegistrationDisabled");
	}
}
