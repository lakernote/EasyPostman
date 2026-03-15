package com.laker.postman.ioc;

/**
 * IOC容器相关异常的基类
 */
public class BeanException extends RuntimeException {

    public BeanException(String message) {
        super(message);
    }

    public BeanException(String message, Throwable cause) {
        super(message, cause);
    }
}

