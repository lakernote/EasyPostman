package com.laker.postman.ioc;

import java.lang.annotation.*;

/**
 * 标记一个方法在Bean销毁前执行
 * 该方法会在容器关闭时自动调用
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreDestroy {
}
