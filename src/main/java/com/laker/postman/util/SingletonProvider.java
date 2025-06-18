package com.laker.postman.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 通用单例提供器，支持任意类的懒加载单例。
 * 用法：MyClass instance = SingletonProvider.getInstance(MyClass.class, MyClass::new);
 * MyClass 必须有无参构造函数。
 */
public class SingletonProvider {
    private static final ConcurrentHashMap<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz, Supplier<T> supplier) {
        return (T) INSTANCES.computeIfAbsent(clazz, k -> supplier.get());
    }
}

