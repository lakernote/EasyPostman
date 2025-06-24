package com.laker.postman.common;

import com.laker.postman.common.panel.BasePanel;
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
        log.info("尝试获取单例实例: {}", clazz.getName());
        return (T) INSTANCE_MAP.computeIfAbsent(clazz, k -> {
            try {
                log.info("开始创建单例实例: {}", clazz.getName());
                var constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                T instance = constructor.newInstance();
                log.info("单例实例创建成功: {}", clazz.getName());
                if (instance instanceof BasePanel panel) {
                    log.info("初始化面板: {}", clazz.getName());
                    panel.safeInit();
                }
                return instance;
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
        log.debug("尝试获取带参数构造的单例实例: {}", clazz.getName());
        return (T) INSTANCE_MAP.computeIfAbsent(clazz, k -> {
            try {
                log.info("创建带参数构造的单例实例: {}", clazz.getName());
                if (args == null || args.length == 0) {
                    var constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    T instance = constructor.newInstance();
                    if (instance instanceof BasePanel panel) {
                        panel.safeInit();
                    }
                    return instance;
                }
                Class<?>[] argTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i] == null ? Object.class : args[i].getClass();
                }
                var constructor = clazz.getDeclaredConstructor(argTypes);
                constructor.setAccessible(true);
                T instance = constructor.newInstance(args);
                if (instance instanceof BasePanel panel) {
                    panel.safeInit();
                }
                return instance;
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
        log.debug("尝试通过自定义工厂方法获取单例实例: {}", clazz.getName());
        return clazz.cast(INSTANCE_MAP.computeIfAbsent(clazz, k -> {
            log.info("通过自定义工厂方法创建单例实例: {}", clazz.getName());
            return supplier.get();
        }));
    }

    /**
     * 移除指定单例
     */
    public static void removeInstance(Class<?> clazz) {
        if (clazz != null) {
            log.info("移除单例实例: {}", clazz.getName());
            INSTANCE_MAP.remove(clazz);
        }
    }

    /**
     * 清空所有单例
     */
    public static void clearAll() {
        log.info("清空所有单例实例");
        INSTANCE_MAP.clear();
    }
}
