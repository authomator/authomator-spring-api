package io.authomator.api.exception;

public class UnauthorizedDomainException extends Exception {
	
	private static final long serialVersionUID = 541121338908613013L;
		
	public UnauthorizedDomainException(){
		super("Unauthorized domain in url");
	}
}
