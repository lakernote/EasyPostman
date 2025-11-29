package com.laker.postman.util;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ComboBox 样式辅助类
 * 提供优雅的方式为特定 ComboBox 应用自定义样式
 */
@UtilityClass
public class ComboBoxStyleHelper {

    /**
     * 使用 PANEL_BACKGROUND 样式创建 ComboBox
     *
     * @param comboBoxSupplier ComboBox 的创建函数
     * @param <T>              ComboBox 的类型
     * @return 应用了样式的 ComboBox
     */
    public static <T extends JComboBox<?>> T createWithPanelStyle(Supplier<T> comboBoxSupplier) {
        return createWithCustomStyle(comboBoxSupplier, ModernColors.MENU_BACKGROUND);
    }

    /**
     * 使用自定义背景色创建 ComboBox
     *
     * @param comboBoxSupplier ComboBox 的创建函数
     * @param backgroundColor  背景色
     * @param <T>              ComboBox 的类型
     * @return 应用了样式的 ComboBox
     */
    public static <T extends JComboBox<?>> T createWithCustomStyle(Supplier<T> comboBoxSupplier, Color backgroundColor) {
        // 保存原始 UIManager 值
        Map<String, Object> originalValues = saveOriginalUIManagerValues();

        try {
            // 临时设置 UIManager 属性
            applyCustomStyle(backgroundColor);

            // 创建 ComboBox（此时会使用我们设置的 UIManager 属性）
            T comboBox = comboBoxSupplier.get();

            // 确保背景色被设置
            comboBox.setBackground(backgroundColor);

            return comboBox;
        } finally {
            // 恢复原始 UIManager 值
            restoreOriginalUIManagerValues(originalValues);
        }
    }

    /**
     * 为已存在的 ComboBox 应用 PANEL_BACKGROUND 样式
     *
     * @param comboBox 要应用样式的 ComboBox
     * @param <T>      ComboBox 的类型
     * @return 应用了样式的 ComboBox
     */
    public static <T extends JComboBox<?>> T applyPanelStyle(T comboBox) {
        return applyCustomStyle(comboBox, ModernColors.PANEL_BACKGROUND);
    }

    /**
     * 为已存在的 ComboBox 应用自定义样式
     *
     * @param comboBox        要应用样式的 ComboBox
     * @param backgroundColor 背景色
     * @param <T>             ComboBox 的类型
     * @return 应用了样式的 ComboBox
     */
    public static <T extends JComboBox<?>> T applyCustomStyle(T comboBox, Color backgroundColor) {
        // 保存原始 UIManager 值
        Map<String, Object> originalValues = saveOriginalUIManagerValues();

        try {
            // 临时设置 UIManager 属性
            applyCustomStyle(backgroundColor);

            // 更新 UI，让新的 UIManager 属性生效
            comboBox.updateUI();

            // 确保背景色被设置
            comboBox.setBackground(backgroundColor);

            return comboBox;
        } finally {
            // 恢复原始 UIManager 值
            restoreOriginalUIManagerValues(originalValues);
        }
    }

    /**
     * 保存原始的 UIManager 值
     */
    private static Map<String, Object> saveOriginalUIManagerValues() {
        Map<String, Object> values = new HashMap<>();
        values.put("ComboBox.buttonBackground", UIManager.getColor("ComboBox.buttonBackground"));
        values.put("ComboBox.buttonEditableBackground", UIManager.getColor("ComboBox.buttonEditableBackground"));
        values.put("ComboBox.buttonFocusedBackground", UIManager.getColor("ComboBox.buttonFocusedBackground"));
        values.put("ComboBox.buttonSeparatorColor", UIManager.getColor("ComboBox.buttonSeparatorColor"));
        values.put("ComboBox.buttonDisabledSeparatorColor", UIManager.getColor("ComboBox.buttonDisabledSeparatorColor"));
        return values;
    }

    /**
     * 应用自定义样式到 UIManager
     */
    private static void applyCustomStyle(Color backgroundColor) {
        UIManager.put("ComboBox.buttonBackground", backgroundColor);
        UIManager.put("ComboBox.buttonEditableBackground", backgroundColor);
        UIManager.put("ComboBox.buttonFocusedBackground", backgroundColor);
        UIManager.put("ComboBox.buttonSeparatorColor", null);
        UIManager.put("ComboBox.buttonDisabledSeparatorColor", null);
    }

    /**
     * 恢复原始的 UIManager 值
     */
    private static void restoreOriginalUIManagerValues(Map<String, Object> originalValues) {
        originalValues.forEach(UIManager::put);
    }
}
