package com.laker.postman.util;

import com.laker.postman.common.constants.EasyPostManColors;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一风格工具类
 * 用于集中设置 FlatLaf/Swing 全局 UI 属性（如圆角、背景色等）
 */
@Slf4j
public class StyleUtils {

    // 圆角常量定义
    private static final int DEFAULT_ARC = 12;
    private static final int BUTTON_ARC = 10;
    private static final int COMPONENT_ARC = 10;

    // 分割线常量
    private static final int DIVIDER_SIZE = 6;
    private static final int BORDER_WIDTH = 1;

    // 私有构造函数，防止工具类被实例化
    private StyleUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 应用风格（圆角、背景色等）
     * 建议在 LookAndFeel 设置之后、主界面初始化之前调用
     * UIManager.put 用于设置 Swing 全局 UI 属性（如颜色、圆角、字体、背景等），会影响所有后续创建的组件。
     */
    public static void apply() {
        try {
            // 一次性设置所有UI属性，减少方法调用开销
            Map<String, Object> allProperties = new HashMap<>(100);

            // 应用背景色设置
            addBackgroundColors(allProperties);

            // 应用圆角设置
            addRoundedCorners(allProperties);

            // 应用分割线设置
            addSplitPaneStyles(allProperties);

            // 应用FlatLaf增强设置
            addFlatLafEnhancements(allProperties);

            // 批量设置所有属性
            setUIProperties(allProperties);

            log.debug("风格应用成功，共设置 {} 个UI属性", allProperties.size());

        } catch (Exception e) {
            // 如果应用样式失败，记录错误但不中断程序运行
            log.error("应用风格时发生错误", e);
        }
    }

    /**
     * 添加背景色相关设置
     */
    private static void addBackgroundColors(Map<String, Object> properties) {
        // 基础背景色设置
        Color panelBg = EasyPostManColors.PANEL_BACKGROUND;
        properties.put("Panel.background", panelBg);
        properties.put("ToolBar.background", panelBg);
        properties.put("TextArea.background", panelBg);
        properties.put("ScrollPane.background", panelBg);
        properties.put("Table.background", panelBg);
        properties.put("Label.background", panelBg);
        properties.put("Label.disabledBackground", panelBg);
        properties.put("Tree.background", panelBg);
        properties.put("SplitPane.background", panelBg);
        properties.put("TextPane.background", panelBg);
        properties.put("List.background", panelBg);
        properties.put("EditorPane.background", panelBg);

        // 菜单相关背景色
        Color menuBg = EasyPostManColors.MENU_BACKGROUND;
        properties.put("MenuBar.background", menuBg);
        properties.put("Menu.background", menuBg);
        properties.put("MenuItem.background", menuBg);

        // TabbedPane 背景色
        properties.put("TabbedPane.background", panelBg);
        properties.put("TabbedPane.tabAreaBackground", panelBg);
        properties.put("TabbedPane.selectedBackground", panelBg);
        properties.put("TabbedPane.contentAreaColor", panelBg);

        // 选中背景色设置
        properties.put("Menu.selectionBackground", EasyPostManColors.SELECTION_BACKGROUND);
        properties.put("MenuItem.selectionBackground", EasyPostManColors.SELECTION_BACKGROUND);
        properties.put("Table.selectionBackground", EasyPostManColors.TABLE_SELECTION_BACKGROUND);
        properties.put("TextArea.selectionBackground", panelBg);

        // Dialog 样式增强
        properties.put("Dialog.background", panelBg);
        properties.put("OptionPane.background", panelBg);
        properties.put("OptionPane.buttonArea.background", panelBg);
        properties.put("OptionPane.messageArea.background", panelBg);

        // 前景色设置
        Color accentColor = EasyPostManColors.ACCENT_COLOR;
        properties.put("TabbedPane.selectedForeground", accentColor);
        properties.put("TextArea.caretForeground", accentColor);
        properties.put("TextArea.selectionForeground", accentColor);

        // 特殊属性设置
        properties.put("Table.gridColor", EasyPostManColors.TABLE_GRID_COLOR);
        properties.put("TableHeader.background", EasyPostManColors.TABLE_HEADER_BACKGROUND);
        properties.put("ScrollPane.border", BorderFactory.createLineBorder(EasyPostManColors.BORDER_COLOR));
    }

    /**
     * 添加圆角设置
     */
    private static void addRoundedCorners(Map<String, Object> properties) {
        properties.put("Component.arc", DEFAULT_ARC);
        properties.put("Button.arc", BUTTON_ARC);
        properties.put("ProgressBar.arc", COMPONENT_ARC);
        properties.put("TextComponent.arc", COMPONENT_ARC);
        properties.put("ScrollBar.thumbArc", COMPONENT_ARC);
        properties.put("TabbedPane.tabAreaArc", COMPONENT_ARC);
        properties.put("TabbedPane.tabArc", COMPONENT_ARC);
    }

    /**
     * 添加分割线设置
     */
    private static void addSplitPaneStyles(Map<String, Object> properties) {
        properties.put("SplitPane.dividerSize", DIVIDER_SIZE);
        properties.put("SplitPane.dividerFocusColor", EasyPostManColors.DIVIDER_FOCUS_COLOR);
        properties.put("SplitPaneDivider.background", EasyPostManColors.DIVIDER_BACKGROUND);
        properties.put("SplitPaneDivider.border",
                BorderFactory.createLineBorder(EasyPostManColors.DIVIDER_BORDER_COLOR, BORDER_WIDTH));
    }

    /**
     * 添加 FlatLaf 细节增强设置
     */
    private static void addFlatLafEnhancements(Map<String, Object> properties) {
        // 标题栏增强
        properties.put("TitlePane.unifiedBackground", true);
        properties.put("TitlePane.showIcon", true);
        properties.put("TitlePane.centerTitle", true);
        properties.put("TitlePane.buttonHoverBackground", EasyPostManColors.SELECTION_BACKGROUND);
        properties.put("TitlePane.buttonPressedBackground", EasyPostManColors.TABLE_SELECTION_BACKGROUND);

        // 菜单悬停效果
        Color tableSelectionBg = EasyPostManColors.TABLE_SELECTION_BACKGROUND;
        Color accentColor = EasyPostManColors.ACCENT_COLOR;
        properties.put("Menu.selectionBackground", tableSelectionBg);
        properties.put("MenuItem.selectionBackground", tableSelectionBg);
        properties.put("Menu.selectionForeground", accentColor);
        properties.put("MenuItem.selectionForeground", accentColor);

        // 按钮悬停效果
        properties.put("Button.hoverBackground", EasyPostManColors.BUTTON_HOVER_BACKGROUND);
        properties.put("Button.pressedBackground", EasyPostManColors.BUTTON_PRESSED_BACKGROUND);

        // 滚动条样式
        properties.put("ScrollBar.thumb", EasyPostManColors.SCROLLBAR_THUMB);
        properties.put("ScrollBar.thumbHover", EasyPostManColors.SCROLLBAR_THUMB_HOVER);
        properties.put("ScrollBar.track", EasyPostManColors.SCROLLBAR_TRACK);

        // 分隔符样式
        properties.put("Separator.foreground", EasyPostManColors.SEPARATOR_FOREGROUND);
        properties.put("Separator.background", EasyPostManColors.SEPARATOR_BACKGROUND);
    }

    /**
     * 批量设置 UI 属性的工具方法
     *
     * @param properties 属性映射表
     */
    private static void setUIProperties(Map<String, ?> properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                UIManager.put(entry.getKey(), entry.getValue());
            }
        }
    }
}