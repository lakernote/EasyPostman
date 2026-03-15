package com.laker.postman.ioc;

import java.lang.annotation.*;

/**
 * 指定Bean的作用域
 * 默认为单例（singleton）
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

    /**
     * Bean的作用域
     * - "singleton": 单例模式（默认）
     * - "prototype": 原型模式（每次获取都创建新实例）
     */
    String value() default "singleton";

    /**
     * 常量：单例
     */
    String SINGLETON = "singleton";

    /**
     * 常量：原型
     */
    String PROTOTYPE = "prototype";
}

