package com.laker.postman.ioc;

/**
 * Bean创建失败异常
 */
public class BeanCreationException extends BeanException {

    public BeanCreationException(String beanName, String message) {
        super("Failed to create bean '" + beanName + "': " + message);
    }

    public BeanCreationException(String beanName, Throwable cause) {
        super("Failed to create bean '" + beanName + "'", cause);
    }

    public BeanCreationException(String beanName, String message, Throwable cause) {
        super("Failed to create bean '" + beanName + "': " + message, cause);
    }
}