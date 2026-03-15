package com.laker.postman.ioc;


import java.lang.annotation.*;

/* 标记一个方法在Bean初始化后执行
 * 该方法会在依赖注入完成后自动调用
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PostConstruct {
}

