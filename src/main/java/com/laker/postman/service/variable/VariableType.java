package com.laker.postman.service.variable;

import lombok.Getter;

import java.awt.*;

/**
 * 变量类型枚举
 * <p>
 * 定义了不同类型的变量源及其在UI中的展示样式
 */
@Getter
public enum VariableType {
    /**
     * 临时变量 - 优先级最高，仅在当前请求执行过程中有效
     */
    TEMPORARY("临时变量", "T", new Color(255, 152, 0), 1),

    /**
     * 分组变量 - 从请求所在分组继承的变量
     */
    GROUP("分组变量", "G", new Color(3, 169, 244), 2),

    /**
     * 环境变量 - 从当前激活的环境中获取
     */
    ENVIRONMENT("环境变量", "E", new Color(46, 125, 50), 5),

    /**
     * 内置函数 - 动态函数，如 $guid, $timestamp 等
     */
    BUILT_IN("内置函数", "$", new Color(156, 39, 176), 10);

    /**
     * 变量类型的中文名称
     */
    private final String displayName;

    /**
     * UI渲染时使用的图标符号
     */
    private final String iconSymbol;

    /**
     * UI渲染时使用的颜色
     */
    private final Color color;

    /**
     * 优先级（数值越小优先级越高）
     */
    private final int priority;

    VariableType(String displayName, String iconSymbol, Color color, int priority) {
        this.displayName = displayName;
        this.iconSymbol = iconSymbol;
        this.color = color;
        this.priority = priority;
    }
}
