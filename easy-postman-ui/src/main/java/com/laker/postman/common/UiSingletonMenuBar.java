package com.laker.postman.common;

import javax.swing.*;

/**
 * 菜单栏基础抽象类，统一UI初始化和监听注册结构
 * 提供了initUI和registerListeners两个抽象方法，
 * 子类需要实现这两个方法来完成具体的UI组件初始化和事件监听注册。
 * 所有子类必须通过UiSingletonFactory获取实例，禁止直接new创建实例。
 */
public abstract class UiSingletonMenuBar extends JMenuBar {
    // 使用静态线程局部变量直接控制创建状态
    private static final ThreadLocal<Boolean> CREATING_ALLOWED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // 构造函数设为protected，限制外部直接new
    protected UiSingletonMenuBar() {
        // 验证是否允许创建实例
        if (!CREATING_ALLOWED.get()) {
            throw new IllegalStateException(
                    "UiSingletonMenuBar子类必须通过UiSingletonFactory.getInstance()获取实例，禁止直接new创建: " +
                            this.getClass().getName());
        }
    }

    public void initializeSingletonUi() {
        initUI();
        registerListeners();
    }

    /**
     * 初始化UI组件
     */
    protected abstract void initUI();

    /**
     * 注册事件监听
     */
    protected abstract void registerListeners();

    /**
     * 设置是否允许创建实例，供UiSingletonFactory使用
     */
    public static void setFactoryCreationAllowed(boolean allowed) {
        CREATING_ALLOWED.set(allowed);
    }
}
