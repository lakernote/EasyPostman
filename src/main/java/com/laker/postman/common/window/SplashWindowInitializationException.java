package com.laker.postman.common.window;

import java.io.Serial;

public class SplashWindowInitializationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SplashWindowInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}