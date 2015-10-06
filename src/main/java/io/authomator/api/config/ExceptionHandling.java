package io.authomator.api.config;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.authomator.api.dto.GenericError;
import io.authomator.api.dto.ValidationError;



//TODO: implement unittests on @ControllerAdvice

@ControllerAdvice
public class ExceptionHandling {
	
	private static final Logger logger = Logger.getLogger(ExceptionHandling.class);
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ResponseBody
    public ValidationError processValidationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors();
        ValidationError validationError = new ValidationError();
        
        for(FieldError error: fieldErrors){
        	validationError.addFieldError(
    			error.getField(), 
    			error.getDefaultMessage(), 
    			error.getCode()
			);
        }
        return validationError;
    }
	
		
	@ExceptionHandler(InvalidJwtException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	@ResponseBody
	public ValidationError handleInvalidJwtException(InvalidJwtException ex){
		logger.log(Level.WARN, String.format("Invalid jwt token received: %s", ex.getMessage()));
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("token", "Invalid jwt token", "InvalidToken");
		return validationError;
	}
	
	
	@ExceptionHandler(MalformedClaimException.class)
	@ResponseStatus(value=HttpStatus.UNPROCESSABLE_ENTITY)
	@ResponseBody
	public ValidationError handleInvalidJwtException(MalformedClaimException ex){
		logger.log(Level.ERROR, String.format("Invalid jwt token received, claim malformed: %s", ex.getMessage()));
		ValidationError validationError = new ValidationError();
		validationError.addFieldError("token", "Invalid jwt token", "InvalidToken");
		return validationError;
	}

	
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(value=HttpStatus.BAD_REQUEST)
	@ResponseBody
	public GenericError handleEmptyBodyRequest(HttpMessageNotReadableException ex){
		logger.log(Level.ERROR, String.format("Invalid http request received: %s", ex.getMessage()));		
		return new GenericError(new RuntimeException("Invalid http request"), "HttpMessageNotReadable");
	}
	
	@ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public GenericError processUnknownExceptions(Exception ex) {
		logger.error("Handling an unknown exception", ex);
		Exception newEx = new RuntimeException("An unknown error occured");
		GenericError genericError = new GenericError(newEx, "UnknownError");
        return genericError;
	}
}