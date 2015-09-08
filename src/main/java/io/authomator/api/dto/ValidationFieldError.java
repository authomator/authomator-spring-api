package io.authomator.api.dto;

public class ValidationFieldError {
	
	private String field;
	private String message;
	private String code;
	
	public ValidationFieldError(){}
	
	public ValidationFieldError(String field, String message, String code) {
		this.field = field;
		this.message = message;
		this.code = code;
	}

	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
}
