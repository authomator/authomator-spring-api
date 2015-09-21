package io.authomator.api.exception;

public class UnauthorizedDomainException extends Exception {
	
	private static final long serialVersionUID = 541121338908613013L;
	
	private final String url;
	
	public UnauthorizedDomainException(final String url){
		super("Unauthorized domain in url");
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
		
}
