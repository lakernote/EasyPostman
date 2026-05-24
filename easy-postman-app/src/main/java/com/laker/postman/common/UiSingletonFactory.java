package com.laker.postman.common;

import com.laker.postman.common.exception.GetInstanceException;
import lombok.extern.slf4j.Slf4j;

import java.awt.Window;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UI 单例工厂，仅用于创建和管理主窗口、单例面板和单例菜单栏。
 * <p>
 * 业务 Service/Repository 应通过 IOC 容器获取，不应放入该工厂。
 */
@Slf4j
public class UiSingletonFactory {
    private static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();

    private UiSingletonFactory() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取无参构造的单例实例
     * 线程安全，支持并发访问。
     * "占位符"机制：
     * 在创建实例前，先往 INSTANCE_MAP 放入一个占位对象，防止递归依赖时重复创建或抛出递归异常。
     * 这样如果发生循环依赖，后续递归 getInstance 会直接返回占位符，避免死循环或 IllegalStateException。
     * 这个机制的作用是：
     * 防止 A 依赖 B，B 又依赖 A 时递归死锁；
     * 类似 Spring 的三级缓存，允许"半成品"对象先注册，打破依赖环；
     * 提高健壮性，便于排查和修复循环依赖问题。
     * "半成品"B 只是临时占位，最终会被完整实例替换。只要不在初始化阶段就用它的功能，后续是安全的。如果必须在构造阶段用到对方，建议优化设计，避免循环依赖
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        if (!isSupportedUiSingleton(clazz)) {
            throw new IllegalArgumentException(
                    "UiSingletonFactory can only create UI singletons. Use BeanFactory for services: "
                            + clazz.getName());
        }
        // 1. 创建占位符对象，防止递归依赖
        // 每个类的占位符是唯一的，避免不同类间冲突
        Object placeholder = new Object();
        // 2. 尝试将占位符放入  INSTANCE_MAP putIfAbsent 是原子操作，保证并发安全 如果没有值才放入，并返回null
        // 如果已存在实例，existing 会是真实实例或占位符对象 不会覆盖原有的 value，直接返回原有的 value。
        // 如果不存在实例，existing 会是 null，此时放入占位符
        Object existing = INSTANCE_MAP.putIfAbsent(clazz, placeholder);
        // 3. 如果已存在实例（或占位符），直接返回
        if (existing != null && existing != placeholder) {
            return (T) existing;
        }
        try {

            // 对于UiSingletonPanel类型，设置创建标志
            boolean isUiSingletonPanel = UiSingletonPanel.class.isAssignableFrom(clazz);
            if (isUiSingletonPanel) {
                UiSingletonPanel.setFactoryCreationAllowed(true);
            }

            boolean isUiSingletonMenuBar = UiSingletonMenuBar.class.isAssignableFrom(clazz);
            if (isUiSingletonMenuBar) {
                UiSingletonMenuBar.setFactoryCreationAllowed(true);
            }

            // 4. 反射创建实例
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();

            // 重置创建标志
            if (isUiSingletonPanel) {
                UiSingletonPanel.setFactoryCreationAllowed(false);
            }
            if (isUiSingletonMenuBar) {
                UiSingletonMenuBar.setFactoryCreationAllowed(false);
            }

            if (instance instanceof UiSingletonPanel panel) {
                panel.initializeSingletonUi();
            } else if (instance instanceof UiSingletonMenuBar menuBar) {
                menuBar.initializeSingletonUi();
            }
            INSTANCE_MAP.put(clazz, instance); // 替换占位符为真实实例
            return instance;
        } catch (Exception e) {
            log.error("创建单例失败: {}", clazz.getName(), e);
            INSTANCE_MAP.remove(clazz, placeholder); // 出错时移除占位符

            // 确保在异常情况下也重置创建标志
            if (UiSingletonPanel.class.isAssignableFrom(clazz)) {
                UiSingletonPanel.setFactoryCreationAllowed(false);
            }
            if (UiSingletonMenuBar.class.isAssignableFrom(clazz)) {
                UiSingletonMenuBar.setFactoryCreationAllowed(false);
            }

            throw new GetInstanceException("创建单例失败: " + clazz.getName(), e);
        }
    }

    private static boolean isSupportedUiSingleton(Class<?> clazz) {
        return UiSingletonPanel.class.isAssignableFrom(clazz)
                || UiSingletonMenuBar.class.isAssignableFrom(clazz)
                || Window.class.isAssignableFrom(clazz);
    }

    public static <T> Optional<T> getExistingInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }

        Object instance = INSTANCE_MAP.get(clazz);
        if (clazz.isInstance(instance)) {
            return Optional.of(clazz.cast(instance));
        }
        return Optional.empty();
    }
}
