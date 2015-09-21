package io.authomator.api.exception;

public class NonSecureUrlException extends Exception {

	private static final long serialVersionUID = -1775858422805359344L;

	public NonSecureUrlException() {
		super("Invalid URL - non secure urls not allowed");
	}
}
