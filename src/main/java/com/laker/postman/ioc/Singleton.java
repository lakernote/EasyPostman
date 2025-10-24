package com.laker.postman.ioc;

import java.lang.annotation.*;

/**
 * 标记一个Bean为单例模式（默认）
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Singleton {
}

