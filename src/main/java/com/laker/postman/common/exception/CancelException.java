package com.laker.postman.common.exception;

public class CancelException extends RuntimeException {

    public CancelException() {
        super();
    }

    public CancelException(String message) {
        super(message);
    }
}
