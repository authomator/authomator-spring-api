package io.authomator.api.exception;

public class EmailTransportException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4669735925375926803L;
	
	public EmailTransportException(String message, Exception e){
		super(message, e);
	}

}
