package io.authomator.api.dto;

public class TokenReply {

	/**
	 * Access token
	 */
	private String at;
	
	/**
	 * Identity token
	 */
	private String it;
	
	/**
	 * Refresh token
	 */
	private String rt;
	
	
	public String getAt() {
		return at;
	}
	public void setAt(String at) {
		this.at = at;
	}
	public String getIt() {
		return it;
	}
	public void setIt(String it) {
		this.it = it;
	}
	public String getRt() {
		return rt;
	}
	public void setRt(String rt) {
		this.rt = rt;
	}
}
