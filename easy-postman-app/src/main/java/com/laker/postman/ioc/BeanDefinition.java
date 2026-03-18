package com.laker.postman.ioc;

import lombok.Data;

/**
 * Bean定义，描述一个Bean的元数据
 */
@Data
public class BeanDefinition {
    /**
     * Bean名称
     */
    private String name;

    /**
     * Bean的Class类型
     */
    private Class<?> beanClass;

    /**
     * 是否单例（默认为单例）
     */
    private boolean singleton = true;


    public BeanDefinition(String name, Class<?> beanClass) {
        this.name = name;
        this.beanClass = beanClass;
    }

    public BeanDefinition(String name, Class<?> beanClass, boolean singleton) {
        this.name = name;
        this.beanClass = beanClass;
        this.singleton = singleton;
    }
}

