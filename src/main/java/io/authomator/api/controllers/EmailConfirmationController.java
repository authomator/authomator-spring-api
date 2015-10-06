package io.authomator.api.controllers;

import java.net.MalformedURLException;

import javax.validation.Valid;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.service.UserService;
import io.authomator.api.dto.GenericError;
import io.authomator.api.dto.SendConfirmEmailRequest;
import io.authomator.api.dto.ValidationError;
import io.authomator.api.exception.EmailConfirmationNotEnabledException;
import io.authomator.api.exception.EmailTransportException;
import io.authomator.api.exception.NonSecureUrlException;
import io.authomator.api.exception.UnauthorizedDomainException;
import io.authomator.api.exception.UserEmailConfirmedAlreadyException;
import io.authomator.api.exception.UserNotFoundException;
import io.authomator.api.jwt.JwtService;
import io.authomator.api.mail.MailService;

@RestController
public class EmailConfirmationController {

	@Autowired
	JwtService jwtService;

	@Autowired
	UserService userService;

	@Autowired
	MailService mailService;
	
	private static Logger logger = Logger.getLogger(EmailConfirmationController.class);
	

	@RequestMapping("/send-confirm-email")
	@ResponseStatus(value=HttpStatus.NO_CONTENT)
	public void sendConfirmEmail(@Valid @RequestBody SendConfirmEmailRequest req)
			throws InvalidJwtException, MalformedURLException, NonSecureUrlException, UnauthorizedDomainException,
			EmailTransportException, JoseException, MalformedClaimException, EmailConfirmationNotEnabledException,
			UserNotFoundException, UserEmailConfirmedAlreadyException {
		
		JwtClaims claims = jwtService.validateAccessToken(req.getAccessToken());
		User user = userService.getUserForEmailConfirmation(claims.getSubject());
		JsonWebSignature confirmToken = jwtService.getConfirmEmailToken(user);
		mailService.sendConfirmEmailMail(user.getEmail(), req.getUrl(), confirmToken.getCompactSerialization());
	}
	
	
	/*
	 * Exception handling
	 * ------------------------------------------------------------------------------------------
	 */
	
	private ValidationError createInvalidAccessTokenDto() {
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("accessToken", "Invalid JWT access token", "CredentialsError");
		return validationError;
	}
	
	@ExceptionHandler(UserNotFoundException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError userNotFound(UserNotFoundException ex) {
		logger.log(Level.WARN, String.format("Unknown user tried to send confirm email %s", ex.getEmail()));
		return createInvalidAccessTokenDto();
	}
	
	@ExceptionHandler(NonSecureUrlException.class)
	@ResponseStatus(value=HttpStatus.FORBIDDEN)
	public GenericError nonSecureUrl(NonSecureUrlException ex){
		logger.log(Level.WARN, String.format("User tried to send confirm email to non secure url: %s", ex.getUrl()));
		return new GenericError(new Exception("The requested url is not secure"), "NonSecureUrl");
	}
		
	@ExceptionHandler(UnauthorizedDomainException.class)
	@ResponseStatus(value=HttpStatus.FORBIDDEN)
	public GenericError unauthorizedDomain(UnauthorizedDomainException ex){
		logger.log(Level.WARN, String.format("User tried to send confirm email to unauthorized domain: %s", ex.getUrl()));
		return new GenericError(new Exception("The requested url is not allowed"), "UnauthorizedDomain");
	}
	
	@ExceptionHandler(InvalidJwtException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError handleInvalidJwtException(InvalidJwtException ex){
		logger.log(Level.ERROR, String.format("Access token is invalid for email confirmation: %s", ex.getMessage()));
		return createInvalidAccessTokenDto();
	}
	
	@ExceptionHandler(EmailConfirmationNotEnabledException.class)
	@ResponseStatus(value=HttpStatus.FORBIDDEN)
	public GenericError emailConfirmationNotEnabled(EmailConfirmationNotEnabledException ex){
		logger.log(Level.WARN, String.format("User tried to send confirm email, but email confirmation is not enabled: %s", ex.getMessage()));
		return new GenericError(new Exception("Email confirmation is not enabled"), "EmailConfirmationNotEnabled");
	}
	
	@ExceptionHandler(UserEmailConfirmedAlreadyException.class)
	@ResponseStatus(value=HttpStatus.FORBIDDEN)
	public GenericError userEmailAlreadyConfirmed(UserEmailConfirmedAlreadyException ex){
		logger.log(Level.WARN, String.format("User tried to send confirm email, but email confirmation was already ok: %s", ex.getMessage()));
		return new GenericError(new Exception("Email confirmation was already performed"), "UserEmailConfirmedAlready");
	}
	
	
}
