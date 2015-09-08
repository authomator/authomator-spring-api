package io.authomator.api.dto;

import java.util.ArrayList;
import java.util.List;



public class ValidationError {
	
	private final String message = "Validation Failed";
	private final String code = "ValidationFailed";
	
	private List<ValidationFieldError> fieldErrors = new ArrayList<>();
	
	public ValidationError(){}

	public String getMessage() {
		return message;
	}
	
	public String getCode() {
		return code;
	}

	public void addFieldError(String field, String message, String code){
		fieldErrors.add(new ValidationFieldError(field, message, code));
	}

	public void setFieldErrors(List<ValidationFieldError> fieldErrors) {
		this.fieldErrors = fieldErrors;
	}

	public List<ValidationFieldError> getFieldErrors() {
		if (fieldErrors == null){
			return new ArrayList<ValidationFieldError>(0);
		}
		return fieldErrors;
	}
}
