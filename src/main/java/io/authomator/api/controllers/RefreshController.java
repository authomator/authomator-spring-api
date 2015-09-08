package io.authomator.api.controllers;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.service.UserService;
import io.authomator.api.dto.TokenReply;
import io.authomator.api.dto.ValidationError;
import io.authomator.api.exception.UserNotFoundException;
import io.authomator.api.jwt.JwtService;

@RestController
@RequestMapping("/api/auth")
public class RefreshController {

	private static final Logger logger = Logger.getLogger(RefreshController.class);
	
	@Autowired
	UserService userService;
	
	@Autowired
	JwtService jwtService;

	@RequestMapping(path="/refresh/{refreshToken}", method=RequestMethod.POST)
	public TokenReply refresh(@PathVariable String refreshToken) throws InvalidJwtException, MalformedClaimException, UserNotFoundException, JoseException {
		JwtClaims refreshClaims = jwtService.validateRefreshToken(refreshToken);
		User user = userService.refresh(refreshClaims.getSubject());
		return jwtService.createTokensForUser(user);
	}
	
	/*
	 * Exception handling
	 * ------------------------------------------------------------------------------------------
	 */
		
	private ValidationError createInvalidTokenDto() {
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("token", "Invalid refresh token", "InvalidToken");
		return validationError;
	}
	
	
	@ExceptionHandler(InvalidJwtException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError handleInvalidJwtException(InvalidJwtException ex){
		logger.log(Level.WARN, String.format("Invalid jwt token received: %s", ex.getMessage()));
		return createInvalidTokenDto();
	}
	
	@ExceptionHandler(MalformedClaimException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError handleInvalidJwtException(MalformedClaimException ex){
		logger.log(Level.ERROR, String.format("Invalid jwt token received, claim malformed: %s", ex.getMessage()));
		return createInvalidTokenDto();
	}
	
	@ExceptionHandler(UserNotFoundException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError handleInvalidJwtException(UserNotFoundException ex){
		logger.log(Level.ERROR, String.format("Refresh token for nonexisting user: %s", ex.getEmail()));
		return createInvalidTokenDto();
	}
	
}
