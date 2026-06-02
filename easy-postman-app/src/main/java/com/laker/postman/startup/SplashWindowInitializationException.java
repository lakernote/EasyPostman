package com.laker.postman.startup;

import java.io.Serial;

class SplashWindowInitializationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SplashWindowInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
