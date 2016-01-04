package io.authomator.api.exception;

public class ContextNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public ContextNotFoundException(final String context) {
		super("Context not found: " + context);
	}
	
}
