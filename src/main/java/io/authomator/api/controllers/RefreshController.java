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
import io.authomator.api.dto.RefreshTokensRequest;
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

	@RequestMapping(path="/refresh", method=RequestMethod.POST)
	public TokenReply refresh(@Valid @RequestBody RefreshTokensRequest req) throws InvalidJwtException, MalformedClaimException, 
																		UserNotFoundException, JoseException {
		
		JwtClaims refreshClaims = jwtService.validateRefreshToken(req.getRt());
		User user = userService.refresh(refreshClaims.getSubject());
		return jwtService.createTokensForUser(user);
	}
	
	/*
	 * Exception handling
	 * ------------------------------------------------------------------------------------------
	 */

	private ValidationError createInvalidRefreshTokenValidationError(){
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("rt", "Invalid jwt token", "InvalidToken");
		return validationError;
	}
			
	@ExceptionHandler(UserNotFoundException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError handleUserNotFoundException(UserNotFoundException ex){
		logger.log(Level.ERROR, String.format("Refresh token for nonexisting user: %s", ex.getEmail()));
		return createInvalidRefreshTokenValidationError();
	}
	
	@ExceptionHandler(InvalidJwtException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError handleInvalidJwtException(InvalidJwtException ex){
		logger.log(Level.ERROR, String.format("Refresh token is invalid: %s", ex.getMessage()));
		return createInvalidRefreshTokenValidationError();
	}
		
}
