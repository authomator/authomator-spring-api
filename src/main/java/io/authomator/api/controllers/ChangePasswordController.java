package io.authomator.api.controllers;

import javax.validation.Valid;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
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
import io.authomator.api.dto.ChangePasswordRequest;
import io.authomator.api.dto.TokenReply;
import io.authomator.api.dto.ValidationError;
import io.authomator.api.exception.InvalidCredentialsException;
import io.authomator.api.exception.UserNotFoundException;
import io.authomator.api.jwt.JwtService;

@RestController
@RequestMapping(path="/api/auth")
public class ChangePasswordController {

	@Autowired
	UserService userService;
	
	@Autowired
	JwtService jwtService;
	
	private static final Logger logger = Logger.getLogger(AuthenticationController.class);
	
	/*
	 * Controllers
	 * ------------------------------------------------------------------------------------------
	 */
	
	@RequestMapping(path="/change", method=RequestMethod.POST)
	public TokenReply changePassword(@Valid @RequestBody ChangePasswordRequest req) throws InvalidJwtException, MalformedClaimException, 
																							UserNotFoundException, InvalidCredentialsException, 
																							JoseException {
		
		JwtClaims claims = jwtService.validateAccessToken(req.getAt());
		User user = userService.changePassword(claims.getSubject(), req.getPassword(), req.getNewPassword());
		return jwtService.createTokensForUser(user);
	}
	
		
	/*
	 * Exception handling
	 * ------------------------------------------------------------------------------------------
	 */
	
	private ValidationError createInvalidCredentialDto() {
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("password", "Invalid credentials", "CredentialsError");
		return validationError;
	}
	
	
	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError invalidCredentials(InvalidCredentialsException ex) {
		logger.log(Level.WARN, String.format("Invalid credentials received for password change: %s", ex.getEmail()));
		return createInvalidCredentialDto();
	}
	
	@ExceptionHandler(UserNotFoundException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError userNotFound(UserNotFoundException ex) {
		logger.log(Level.WARN, String.format("Unknown user tried to change password: %s", ex.getEmail()));
		return createInvalidCredentialDto();
	}
}
