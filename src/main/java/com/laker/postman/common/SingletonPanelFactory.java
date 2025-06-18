package com.laker.postman.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 泛型单例工厂，统一管理所有面板类的单例实例
 */
public class SingletonPanelFactory {
    private static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();
    private static final Object INITIALIZING = new Object();

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz) {
        Object instance = INSTANCE_MAP.get(clazz);
        if (instance == INITIALIZING) {
            throw new IllegalStateException("Recursive singleton initialization detected for: " + clazz.getName());
        }
        if (instance != null) {
            return (T) instance;
        }
        synchronized (INSTANCE_MAP) {
            instance = INSTANCE_MAP.get(clazz);
            if (instance == null) {
                INSTANCE_MAP.put(clazz, INITIALIZING);
                try {
                    instance = clazz.getDeclaredConstructor().newInstance();
                    INSTANCE_MAP.put(clazz, instance);
                } catch (Exception e) {
                    INSTANCE_MAP.remove(clazz);
                    throw new RuntimeException("Cannot create singleton instance for: " + clazz, e);
                }
            } else if (instance == INITIALIZING) {
                throw new IllegalStateException("Recursive singleton initialization detected for: " + clazz.getName());
            }
            return (T) instance;
        }
    }
}
