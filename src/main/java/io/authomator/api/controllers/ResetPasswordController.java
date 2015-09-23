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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.authomator.api.domain.entity.User;
import io.authomator.api.domain.service.UserService;
import io.authomator.api.dto.ForgotPasswordRequest;
import io.authomator.api.dto.GenericError;
import io.authomator.api.dto.ResetPasswordRequest;
import io.authomator.api.dto.TokenReply;
import io.authomator.api.dto.ValidationError;
import io.authomator.api.exception.EmailTransportException;
import io.authomator.api.exception.NonSecureUrlException;
import io.authomator.api.exception.UnauthorizedDomainException;
import io.authomator.api.exception.UserNotFoundException;
import io.authomator.api.jwt.JwtService;
import io.authomator.api.mail.MailService;

@RestController
public class ResetPasswordController {
	
	@Autowired
	UserService userService;

	@Autowired
	JwtService jwtService;
	
	@Autowired
	MailService mailService;
	
	private static final Logger logger = Logger.getLogger(AuthenticationController.class);
	
	/*
	 * Controllers
	 * ------------------------------------------------------------------------------------------
	 */
	
	@RequestMapping(path="/forgot-password", method=RequestMethod.POST)
	@ResponseStatus(code=HttpStatus.NO_CONTENT)
	public void sendMailResetToken(
			@Valid @RequestBody final ForgotPasswordRequest req) throws UserNotFoundException, MalformedURLException, 
																		NonSecureUrlException, UnauthorizedDomainException, 
																		EmailTransportException, JoseException {
		
		User user = userService.forgotPassword(req.getEmail());
		JsonWebSignature jwt = jwtService.getForgotPasswordToken(user);
		mailService.sendForgotPasswordMail(user.getEmail(), req.getUrl(), jwt.getCompactSerialization());
	}
	

	@RequestMapping(path="/reset-password", method=RequestMethod.POST)
	public TokenReply forgotPassword(			
			@Valid @RequestBody() final ResetPasswordRequest req) throws InvalidJwtException, MalformedClaimException, 
																			UserNotFoundException, JoseException {
		JwtClaims claims = jwtService.validateForgotToken(req.getResetToken());
		User user = userService.resetPassword(claims.getSubject(), req.getPassword());
		return jwtService.createTokensForUser(user);
	}
	
	
	
	/*
	 * Exception handling
	 * ------------------------------------------------------------------------------------------
	 */
	
	private ValidationError createInvalidEmailDto() {
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("email", "Invalid or non existing email", "CredentialsError");
		return validationError;
	}
	
	@ExceptionHandler(UserNotFoundException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError userNotFound(UserNotFoundException ex) {
		logger.log(Level.WARN, String.format("Unknown user tried to send forgot or reset password: %s", ex.getEmail()));
		return createInvalidEmailDto();
	}
	
	@ExceptionHandler(NonSecureUrlException.class)
	@ResponseStatus(value=HttpStatus.UNAUTHORIZED)
	public GenericError nonSecureUrl(NonSecureUrlException ex){
		logger.log(Level.WARN, String.format("User tried to send forgot password to non secure url: %s", ex.getUrl()));
		return new GenericError(new Exception("The requested url is not secure"), "NonSecureUrl");
	}
		
	@ExceptionHandler(UnauthorizedDomainException.class)
	@ResponseStatus(value=HttpStatus.UNAUTHORIZED)
	public GenericError unauthorizedDomain(UnauthorizedDomainException ex){
		logger.log(Level.WARN, String.format("User tried to send forgot password to unauthorized domain: %s", ex.getUrl()));
		return new GenericError(new Exception("The requested url is not allowed"), "UnauthorizedDomain");
	}
	
	@ExceptionHandler(InvalidJwtException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	public ValidationError handleInvalidJwtException(InvalidJwtException ex){
		logger.log(Level.ERROR, String.format("Reset token is invalid for password reset: %s", ex.getMessage()));
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("resetToken", "Invalid jwt token", "InvalidToken");
		return validationError;
	}
}
