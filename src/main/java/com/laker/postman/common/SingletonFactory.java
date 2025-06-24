package com.laker.postman.common;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 泛型单例工厂,用于创建和管理单例实例。
 */
@Slf4j
public class SingletonFactory {
    private static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();

    // 递归检测：记录当前线程正在创建的单例类型
    private static final ThreadLocal<Set<Class<?>>> CREATING_CLASSES = ThreadLocal.withInitial(HashSet::new);

    /**
     * 获取无参构造的单例实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        Set<Class<?>> creating = CREATING_CLASSES.get();
        if (creating.contains(clazz)) {
            log.error("递归依赖检测: {} 正在被递归创建, 当前递归链: {}", clazz.getName(), creating);
            throw new IllegalStateException("递归依赖检测: " + clazz.getName() + " 正在被递归创建, 请检查单例依赖关系，避免循环依赖。");
        }
        try {
            creating.add(clazz);
            log.debug("开始创建单例: {}，当前递归链: {}", clazz.getName(), creating);
            return (T) INSTANCE_MAP.computeIfAbsent(clazz, k -> {
                try {
                    var constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    Object instance = constructor.newInstance();
                    log.info("单例创建成功: {}", clazz.getName());
                    return instance;
                } catch (Exception e) {
                    log.error("创建单例失败: {}", clazz.getName(), e);
                    throw new RuntimeException("创建单例失败: " + clazz.getName(), e);
                }
            });
        } finally {
            creating.remove(clazz);
            log.debug("结束创建单例: {}，当前递归链: {}", clazz.getName(), creating);
        }
    }

    /**
     * 获取带参数构造的单例实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz, Object... args) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        Set<Class<?>> creating = CREATING_CLASSES.get();
        if (creating.contains(clazz)) {
            throw new IllegalStateException("递归依赖检测: " + clazz.getName() + " 正在被递归创建, 请检查单例依赖关系，避免循环依赖。");
        }
        try {
            creating.add(clazz);
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
        } finally {
            creating.remove(clazz);
        }
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
