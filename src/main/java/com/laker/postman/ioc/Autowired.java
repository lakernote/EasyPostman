package com.laker.postman.ioc;

import java.lang.annotation.*;

/**
 * 标记需要自动注入的字段
 */
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    /**
     * 是否必须，如果为true且找不到对应的bean则抛出异常
     */
    boolean required() default true;
}

