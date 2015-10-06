package io.authomator.api.exception;

public class UserEmailConfirmedAlreadyException extends Exception {

	private static final long serialVersionUID = 1L;

	public UserEmailConfirmedAlreadyException(final String userInfo){
		super(String.format("The user %s already has his email confirmed", userInfo));
	}
}
