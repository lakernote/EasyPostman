package com.laker.postman.ioc;

import java.lang.annotation.*;

/**
 * 标记一个类为组件，会被IOC容器管理
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {
    /**
     * Bean名称，默认为类名首字母小写
     */
    String value() default "";
}
