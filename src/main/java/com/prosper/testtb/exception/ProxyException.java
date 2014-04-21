package com.prosper.testtb.exception;

public class ProxyException extends RuntimeException {
	
	public ProxyException() {
		super();
	}
	
	public ProxyException(String s) {
		super(s);
	}

	public ProxyException(Throwable t) {
		super(t);
	}
}
