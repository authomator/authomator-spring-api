package io.authomator.api.dto;

public class GenericError {
	
	private String message;
	private String code;
	
	public GenericError(Exception ex){
		message = ex.getMessage();			
	}
	
	public GenericError(Exception ex, String code){
		message = ex.getMessage();
		this.code = code;			
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
