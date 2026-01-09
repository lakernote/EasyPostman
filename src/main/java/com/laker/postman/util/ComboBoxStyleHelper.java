package com.laker.postman.util;

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

    // UIManager 属性键常量
    private static final String COMBOBOX_BUTTON_BACKGROUND = "ComboBox.buttonBackground";
    private static final String COMBOBOX_BUTTON_EDITABLE_BACKGROUND = "ComboBox.buttonEditableBackground";
    private static final String COMBOBOX_BUTTON_FOCUSED_BACKGROUND = "ComboBox.buttonFocusedBackground";
    private static final String COMBOBOX_BUTTON_SEPARATOR_COLOR = "ComboBox.buttonSeparatorColor";
    private static final String COMBOBOX_BUTTON_DISABLED_SEPARATOR_COLOR = "ComboBox.buttonDisabledSeparatorColor";

    /**
     * 使用 MenuBar.background 样式创建 ComboBox（自动适配亮/暗模式）
     *
     * @param comboBoxSupplier ComboBox 的创建函数
     * @param <T>              ComboBox 的类型
     * @return 应用了样式的 ComboBox
     */
    public static <T extends JComboBox<?>> T createWithPanelStyle(Supplier<T> comboBoxSupplier) {
        return createWithCustomStyle(comboBoxSupplier, UIManager.getColor("MenuBar.background"));
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
     * 为已存在的 ComboBox 应用 Panel.background 样式（自动适配亮/暗模式）
     *
     * @param comboBox 要应用样式的 ComboBox
     * @param <T>      ComboBox 的类型
     * @return 应用了样式的 ComboBox
     */
    public static <T extends JComboBox<?>> T applyPanelStyle(T comboBox) {
        return applyCustomStyle(comboBox, UIManager.getColor("Panel.background"));
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
        values.put(COMBOBOX_BUTTON_BACKGROUND, UIManager.getColor(COMBOBOX_BUTTON_BACKGROUND));
        values.put(COMBOBOX_BUTTON_EDITABLE_BACKGROUND, UIManager.getColor(COMBOBOX_BUTTON_EDITABLE_BACKGROUND));
        values.put(COMBOBOX_BUTTON_FOCUSED_BACKGROUND, UIManager.getColor(COMBOBOX_BUTTON_FOCUSED_BACKGROUND));
        values.put(COMBOBOX_BUTTON_SEPARATOR_COLOR, UIManager.getColor(COMBOBOX_BUTTON_SEPARATOR_COLOR));
        values.put(COMBOBOX_BUTTON_DISABLED_SEPARATOR_COLOR, UIManager.getColor(COMBOBOX_BUTTON_DISABLED_SEPARATOR_COLOR));
        return values;
    }

    /**
     * 应用自定义样式到 UIManager
     */
    private static void applyCustomStyle(Color backgroundColor) {
        UIManager.put(COMBOBOX_BUTTON_BACKGROUND, backgroundColor);
        UIManager.put(COMBOBOX_BUTTON_EDITABLE_BACKGROUND, backgroundColor);
        UIManager.put(COMBOBOX_BUTTON_FOCUSED_BACKGROUND, backgroundColor);
        UIManager.put(COMBOBOX_BUTTON_SEPARATOR_COLOR, null);
        UIManager.put(COMBOBOX_BUTTON_DISABLED_SEPARATOR_COLOR, null);
    }

    /**
     * 恢复原始的 UIManager 值
     */
    private static void restoreOriginalUIManagerValues(Map<String, Object> originalValues) {
        originalValues.forEach(UIManager::put);
    }
}
