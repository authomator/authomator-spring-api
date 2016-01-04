package io.authomator.api.exception;

public class MissingDefaultContextException extends Exception {

	private static final long serialVersionUID = 1L;

	public MissingDefaultContextException(final String message){
		super(message);
	}
	
	public MissingDefaultContextException(){
		super("User has no default context, login is not possible");
	}
	
}
