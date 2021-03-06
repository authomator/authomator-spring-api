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

import io.authomator.api.domain.entity.Context;
import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.service.ContextService;
import io.authomator.api.domain.service.UserService;
import io.authomator.api.dto.RefreshTokensRequest;
import io.authomator.api.dto.TokenReply;
import io.authomator.api.dto.ValidationError;
import io.authomator.api.exception.ContextNotFoundException;
import io.authomator.api.exception.InvalidContextException;
import io.authomator.api.exception.UserNotFoundException;
import io.authomator.api.jwt.JwtService;

@RestController
public class RefreshTokensController {

	private static final Logger logger = Logger.getLogger(RefreshTokensController.class);
	
	@Autowired
	UserService userService;
	
	@Autowired
	ContextService contextService;
	
	@Autowired
	JwtService jwtService;

	@RequestMapping(value="/refresh-tokens", method=RequestMethod.POST)
	public TokenReply refresh(@Valid @RequestBody RefreshTokensRequest req) throws InvalidJwtException, MalformedClaimException, 
																		UserNotFoundException, JoseException, InvalidContextException, ContextNotFoundException {
		
		JwtClaims refreshClaims = jwtService.validateRefreshToken(req.getRefreshToken());				
		Context ctx = contextService.findOne(refreshClaims.getStringClaimValue("ctx"));
		User user = userService.refresh(refreshClaims.getSubject(), ctx.getId());
		
		return jwtService.createTokensForUser(user, ctx);
	}
	
	/*
	 * Exception handling
	 * ------------------------------------------------------------------------------------------
	 */

	private ValidationError createInvalidRefreshTokenValidationError(){
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("refreshToken", "Invalid jwt token", "InvalidToken");
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
	
	@ExceptionHandler(InvalidContextException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError handleInvalidContextException(InvalidContextException ex){
		logger.log(Level.ERROR, String.format("Refresh token is invalid: %s", ex.getMessage()));
		return createInvalidRefreshTokenValidationError();
	}
	
	@ExceptionHandler(ContextNotFoundException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError handleContextNotFoundException(ContextNotFoundException ex){
		logger.log(Level.ERROR, String.format("Refresh token is invalid: %s", ex.getMessage()));
		return createInvalidRefreshTokenValidationError();
	}
		
}
