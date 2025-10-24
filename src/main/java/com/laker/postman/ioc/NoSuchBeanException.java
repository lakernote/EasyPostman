package com.laker.postman.ioc;

/**
 * Bean未找到异常
 */
public class NoSuchBeanException extends BeanException {

    public NoSuchBeanException(String beanName) {
        super("No bean named '" + beanName + "' found in the container");
    }

    public NoSuchBeanException(Class<?> requiredType) {
        super("No bean of type '" + requiredType.getName() + "' found in the container");
    }
}

