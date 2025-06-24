package com.laker.postman.common;

import com.laker.postman.common.panel.BasePanel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 泛型单例工厂,用于创建和管理单例实例。
 */
@Slf4j
public class SingletonFactory {
    private static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();

    /**
     * 获取无参构造的单例实例
     * “占位符”机制：
     * 在创建实例前，先往 INSTANCE_MAP 放入一个占位对象，防止递归依赖时重复创建或抛出递归异常。
     * 这样如果发生循环依赖，后续递归 getInstance 会直接返回占位符，避免死循环或 IllegalStateException。
     * 这个机制的作用是：
     * 防止 A 依赖 B，B 又依赖 A 时递归死锁；
     * 类似 Spring 的三级缓存，允许“半成品”对象先注册，打破依赖环；
     * 提高健壮性，便于排查和修复循环依赖问题。
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        log.info("尝试获取单例实例: {}", clazz.getName());
        // 占位符防止递归依赖
        Object placeholder = new Object();
        Object existing = INSTANCE_MAP.putIfAbsent(clazz, placeholder);
        if (existing != null && existing != placeholder) {
            log.info("已存在单例实例: {}", clazz.getName());
            return (T) existing;
        }
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
            INSTANCE_MAP.put(clazz, instance); // 替换占位符为真实实例
            return instance;
        } catch (Exception e) {
            log.error("创建单例失败: {}", clazz.getName(), e);
            INSTANCE_MAP.remove(clazz, placeholder); // 出错时移除占位符
            throw new RuntimeException("创建单例失败: " + clazz.getName(), e);
        }
    }
}