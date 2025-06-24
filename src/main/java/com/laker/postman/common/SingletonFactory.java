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
     * 线程安全，支持并发访问。
     * “占位符”机制：
     * 在创建实例前，先往 INSTANCE_MAP 放入一个占位对象，防止递归依赖时重复创建或抛出递归异常。
     * 这样如果发生循环依赖，后续递归 getInstance 会直接返回占位符，避免死循环或 IllegalStateException。
     * 这个机制的作用是：
     * 防止 A 依赖 B，B 又依赖 A 时递归死锁；
     * 类似 Spring 的三级缓存，允许“半成品”对象先注册，打破依赖环；
     * 提高健壮性，便于排查和修复循环依赖问题。
     * “半成品”B 只是临时占位，最终会被完整实例替换。只要不在初始化阶段就用它的功能，后续是安全的。如果必须在构造阶段用到对方，建议优化设计，避免循环依赖
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        log.info("尝试获取单例实例: {}", clazz.getName());
        // 1. 创建占位符对象，防止递归依赖
        // 每个类的占位符是唯一的，避免不同类间冲突
        Object placeholder = new Object();
        // 2. 尝试将占位符放入  INSTANCE_MAP putIfAbsent 是原子操作，保证并发安全 如果没有值才放入，并返回null
        // 如果已存在实例，existing 会是真实实例或占位符对象 不会覆盖原有的 value，直接返回原有的 value。
        // 如果不存在实例，existing 会是 null，此时放入占位符
        Object existing = INSTANCE_MAP.putIfAbsent(clazz, placeholder);
        // 3. 如果已存在实例（或占位符），直接返回
        if (existing != null && existing != placeholder) {
            log.warn("已存在单例实例: {}", clazz.getName());
            return (T) existing;
        }
        try {
            log.info("开始创建单例实例: {}", clazz.getName());
            // 4. 反射创建实例
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();
            log.info("单例实例创建成功: {}", clazz.getName());
            if (instance instanceof BasePanel panel) {
                log.info("初始化面板: {}", clazz.getName());
                panel.safeInit();
            }
            INSTANCE_MAP.put(clazz, instance); // 替换占位符为真实实例
            log.info("单例实例已注册到 INSTANCE_MAP: {}", clazz.getName());
            return instance;
        } catch (Exception e) {
            log.error("创建单例失败: {}", clazz.getName(), e);
            INSTANCE_MAP.remove(clazz, placeholder); // 出错时移除占位符
            throw new RuntimeException("创建单例失败: " + clazz.getName(), e);
        }
    }
}