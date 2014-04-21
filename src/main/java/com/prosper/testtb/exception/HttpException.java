package com.prosper.testtb.exception;

public class HttpException extends RuntimeException {
	
	public HttpException() {
		super();
	}
	
	public HttpException(String s) {
		super(s);
	}

	public HttpException(Throwable t) {
		super(t);
	}
}
