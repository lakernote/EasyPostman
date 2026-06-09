package com.laker.postman.variable;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
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
     * 执行上下文变量 - 优先级最高，在当前请求或当前运行上下文中有效
     */
    VARIABLE(MessageKeys.VARIABLE_TYPE_VARIABLE, "V", 1),

    /**
     * 迭代数据变量 - 当前 CSV/JSON 行数据
     */
    ITERATION_DATA(MessageKeys.VARIABLE_TYPE_ITERATION_DATA, "D", 2),

    /**
     * 分组变量 - 从请求所在分组继承的变量
     */
    GROUP(MessageKeys.VARIABLE_TYPE_GROUP, "C", 3),

    /**
     * 环境变量 - 从当前激活的环境中获取
     */
    ENVIRONMENT(MessageKeys.VARIABLE_TYPE_ENVIRONMENT, "E", 5),

    /**
     * 全局变量 - 应用级共享变量
     */
    GLOBAL(MessageKeys.VARIABLE_TYPE_GLOBAL, "G", 8),

    /**
     * 内置函数 - 动态函数，如 $guid, $timestamp 等
     */
    BUILT_IN(MessageKeys.VARIABLE_TYPE_BUILT_IN, "$", 10);

    /**
     * 变量类型的国际化消息键
     */
    private final String displayNameKey;

    /**
     * UI渲染时使用的图标符号
     */
    private final String iconSymbol;

    /**
     * 优先级（数值越小优先级越高）
     */
    private final int priority;

    VariableType(String displayNameKey, String iconSymbol, int priority) {
        this.displayNameKey = displayNameKey;
        this.iconSymbol = iconSymbol;
        this.priority = priority;
    }

    /**
     * UI渲染时使用的主题自适应颜色。
     */
    public Color getColor() {
        return switch (this) {
            case VARIABLE -> ModernColors.getVariableContextColor();
            case ITERATION_DATA -> ModernColors.getVariableIterationDataColor();
            case GROUP -> ModernColors.getVariableGroupColor();
            case ENVIRONMENT -> ModernColors.getVariableEnvironmentColor();
            case GLOBAL -> ModernColors.getVariableGlobalColor();
            case BUILT_IN -> ModernColors.getVariableBuiltInColor();
        };
    }

    /**
     * 获取国际化后的显示名称
     *
     * @return 根据当前语言环境返回的显示名称
     */
    public String getDisplayName() {
        return I18nUtil.getMessage(displayNameKey);
    }
}
