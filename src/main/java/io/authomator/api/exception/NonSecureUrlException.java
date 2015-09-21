package io.authomator.api.exception;

public class NonSecureUrlException extends Exception {

	private static final long serialVersionUID = -1775858422805359344L;

	private final String url;
	
	public NonSecureUrlException(final String url) {
		super("Invalid URL - non secure urls not allowed");
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}
