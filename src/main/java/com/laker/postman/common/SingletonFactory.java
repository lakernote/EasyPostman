package com.laker.postman.common;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 泛型单例工厂,用于创建和管理单例实例。
 */
@Slf4j
public class SingletonFactory {
    private static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();

    /**
     * 获取无参构造的单例实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        return (T) INSTANCE_MAP.computeIfAbsent(clazz, k -> {
            try {
                var constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Exception e) {
                log.error("创建单例失败: {}", clazz.getName(), e);
                throw new RuntimeException("创建单例失败: " + clazz.getName(), e);
            }
        });
    }

    /**
     * 获取带参数构造的单例实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz, Object... args) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        return (T) INSTANCE_MAP.computeIfAbsent(clazz, k -> {
            try {
                if (args == null || args.length == 0) {
                    var constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                }
                Class<?>[] argTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i] == null ? Object.class : args[i].getClass();
                }
                var constructor = clazz.getDeclaredConstructor(argTypes);
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            } catch (Exception e) {
                log.error("创建单例失败: {}", clazz.getName(), e);
                throw new RuntimeException("创建单例失败: " + clazz.getName(), e);
            }
        });
    }

    /**
     * 通过自定义工厂方法获取单例实例
     */
    public static <T> T getInstance(Class<T> clazz, Supplier<T> supplier) {
        if (clazz == null || supplier == null) {
            throw new IllegalArgumentException("Class and supplier must not be null");
        }
        return clazz.cast(INSTANCE_MAP.computeIfAbsent(clazz, k -> supplier.get()));
    }

    /**
     * 移除指定单例
     */
    public static void removeInstance(Class<?> clazz) {
        if (clazz != null) {
            INSTANCE_MAP.remove(clazz);
        }
    }

    /**
     * 清空所有单例
     */
    public static void clearAll() {
        INSTANCE_MAP.clear();
    }
}
