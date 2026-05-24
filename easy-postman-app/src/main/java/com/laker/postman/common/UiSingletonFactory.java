package com.laker.postman.common;

import com.laker.postman.common.exception.GetInstanceException;
import com.laker.postman.frame.MainFrame;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * UI 单例工厂，仅用于创建和管理主窗口、单例面板和单例菜单栏。
 * <p>
 * 业务 Service/Repository 应通过 IOC 容器获取，不应放入该工厂。
 */
@Slf4j
public class UiSingletonFactory {
    private static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();
    private static final Set<Class<?>> CREATING_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Object CREATION_LOCK = new Object();

    private UiSingletonFactory() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取无参构造的单例实例
     * 首次创建统一在 EDT 上完成，避免 Swing 组件在后台线程初始化。
     * 如果 UI 单例初始化阶段发生环形依赖，直接失败并提示调整设计，不暴露半成品对象。
     */
    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class must not be null");
        }
        if (!isSupportedUiSingleton(clazz)) {
            throw new IllegalArgumentException(
                    "UiSingletonFactory can only create UI singletons. Use BeanFactory for services: "
                            + clazz.getName());
        }
        Object existing = INSTANCE_MAP.get(clazz);
        if (clazz.isInstance(existing)) {
            return clazz.cast(existing);
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return getOrCreateOnEdt(clazz);
        }

        return createOnEdtAndWait(clazz);
    }

    private static <T> T createOnEdtAndWait(Class<T> clazz) {
        AtomicReference<T> instanceRef = new AtomicReference<>();
        AtomicReference<RuntimeException> runtimeExceptionRef = new AtomicReference<>();
        AtomicReference<Error> errorRef = new AtomicReference<>();

        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    instanceRef.set(getOrCreateOnEdt(clazz));
                } catch (RuntimeException e) {
                    runtimeExceptionRef.set(e);
                } catch (Error e) {
                    errorRef.set(e);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GetInstanceException("创建单例失败: " + clazz.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new GetInstanceException("创建单例失败: " + clazz.getName(), cause);
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        if (runtimeExceptionRef.get() != null) {
            throw runtimeExceptionRef.get();
        }
        return instanceRef.get();
    }

    private static <T> T getOrCreateOnEdt(Class<T> clazz) {
        synchronized (CREATION_LOCK) {
            Object existing = INSTANCE_MAP.get(clazz);
            if (clazz.isInstance(existing)) {
                return clazz.cast(existing);
            }
            if (CREATING_CLASSES.contains(clazz)) {
                throw new GetInstanceException(
                        "Circular UI singleton creation detected: " + clazz.getName(),
                        new IllegalStateException("Circular UI singleton creation: " + clazz.getName()));
            }
            CREATING_CLASSES.add(clazz);
            try {
                return createInstance(clazz);
            } finally {
                CREATING_CLASSES.remove(clazz);
            }
        }
    }

    private static <T> T createInstance(Class<T> clazz) {
        boolean isUiSingletonPanel = UiSingletonPanel.class.isAssignableFrom(clazz);
        boolean isUiSingletonMenuBar = UiSingletonMenuBar.class.isAssignableFrom(clazz);
        try {
            // 对于UiSingletonPanel类型，设置创建标志
            if (isUiSingletonPanel) {
                UiSingletonPanel.setFactoryCreationAllowed(true);
            }

            if (isUiSingletonMenuBar) {
                UiSingletonMenuBar.setFactoryCreationAllowed(true);
            }

            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();

            if (instance instanceof UiSingletonPanel panel) {
                panel.initializeSingletonUi();
            } else if (instance instanceof UiSingletonMenuBar menuBar) {
                menuBar.initializeSingletonUi();
            }
            INSTANCE_MAP.put(clazz, instance);
            return instance;
        } catch (GetInstanceException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建单例失败: {}", clazz.getName(), e);
            throw new GetInstanceException("创建单例失败: " + clazz.getName(), e);
        } finally {
            if (UiSingletonPanel.class.isAssignableFrom(clazz)) {
                UiSingletonPanel.setFactoryCreationAllowed(false);
            }
            if (UiSingletonMenuBar.class.isAssignableFrom(clazz)) {
                UiSingletonMenuBar.setFactoryCreationAllowed(false);
            }
        }
    }

    private static boolean isSupportedUiSingleton(Class<?> clazz) {
        return UiSingletonPanel.class.isAssignableFrom(clazz)
                || UiSingletonMenuBar.class.isAssignableFrom(clazz)
                || MainFrame.class.equals(clazz);
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
