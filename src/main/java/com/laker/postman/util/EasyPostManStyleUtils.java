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
public class EasyPostManStyleUtils {

    // 圆角常量定义
    private static final int DEFAULT_ARC = 12;
    private static final int BUTTON_ARC = 10;
    private static final int COMPONENT_ARC = 10;

    // 分割线常量
    private static final int DIVIDER_SIZE = 6;
    private static final int BORDER_WIDTH = 1;

    // 私有构造函数，防止工具类被实例化
    private EasyPostManStyleUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 应用风格（圆角、背景色等）
     * 建议在 LookAndFeel 设置之后、主界面初始化之前调用
     * UIManager.put 用于设置 Swing 全局 UI 属性（如颜色、圆角、字体、背景等），会影响所有后续创建的组件。
     */
    public static void apply() {
        try {
            // 应用背景色设置
            applyBackgroundColors();

            // 应用圆角设置
            applyRoundedCorners();

            // 应用分割线设置
            applySplitPaneStyles();

            // 应用FlatLaf增强设置
            applyFlatLafEnhancements();

            log.debug("风格应用成功");

        } catch (Exception e) {
            // 如果应用样式失败，记录错误但不中断程序运行
            log.error("应用风格时发生错误", e);
        }
    }

    /**
     * 应用背景色相关设置
     */
    private static void applyBackgroundColors() {
        // 基础背景色设置
        Map<String, Color> backgroundColors = new HashMap<>();
        backgroundColors.put("Panel.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("ToolBar.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("TextArea.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("ScrollPane.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("Table.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("Label.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("Label.disabledBackground", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("Tree.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("SplitPane.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("TextPane.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("List.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("EditorPane.background", EasyPostManColors.PANEL_BACKGROUND);

        // 菜单相关背景色
        backgroundColors.put("MenuBar.background", EasyPostManColors.MENU_BACKGROUND);
        backgroundColors.put("Menu.background", EasyPostManColors.MENU_BACKGROUND);
        backgroundColors.put("MenuItem.background", EasyPostManColors.MENU_BACKGROUND);

        // TabbedPane 背景色
        backgroundColors.put("TabbedPane.background", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("TabbedPane.tabAreaBackground", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("TabbedPane.selectedBackground", EasyPostManColors.PANEL_BACKGROUND);
        backgroundColors.put("TabbedPane.contentAreaColor", EasyPostManColors.PANEL_BACKGROUND);

        // 批量设置背景色
        setUIProperties(backgroundColors);

        // 选中背景色设置
        Map<String, Color> selectionColors = new HashMap<>();
        selectionColors.put("Menu.selectionBackground", EasyPostManColors.SELECTION_BACKGROUND);
        selectionColors.put("MenuItem.selectionBackground", EasyPostManColors.SELECTION_BACKGROUND);
        selectionColors.put("Table.selectionBackground", EasyPostManColors.TABLE_SELECTION_BACKGROUND);
        selectionColors.put("TextArea.selectionBackground", EasyPostManColors.PANEL_BACKGROUND);

        setUIProperties(selectionColors);

        // 前景色设置
        Map<String, Color> foregroundColors = new HashMap<>();
        foregroundColors.put("TabbedPane.selectedForeground", EasyPostManColors.ACCENT_COLOR);
        foregroundColors.put("TextArea.caretForeground", EasyPostManColors.ACCENT_COLOR);
        foregroundColors.put("TextArea.selectionForeground", EasyPostManColors.ACCENT_COLOR);

        setUIProperties(foregroundColors);

        // 特殊属性设置
        UIManager.put("Table.gridColor", EasyPostManColors.TABLE_GRID_COLOR);
        UIManager.put("TableHeader.background", EasyPostManColors.TABLE_HEADER_BACKGROUND);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(EasyPostManColors.BORDER_COLOR));
    }

    /**
     * 应用圆角设置
     */
    private static void applyRoundedCorners() {
        Map<String, Integer> arcProperties = new HashMap<>();
        arcProperties.put("Component.arc", DEFAULT_ARC);
        arcProperties.put("Button.arc", BUTTON_ARC);
        arcProperties.put("ProgressBar.arc", COMPONENT_ARC);
        arcProperties.put("TextComponent.arc", COMPONENT_ARC);
        arcProperties.put("ScrollBar.thumbArc", COMPONENT_ARC);
        arcProperties.put("TabbedPane.tabAreaArc", COMPONENT_ARC);
        arcProperties.put("TabbedPane.tabArc", COMPONENT_ARC);

        for (Map.Entry<String, Integer> entry : arcProperties.entrySet()) {
            UIManager.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 应用分割线设置
     */
    private static void applySplitPaneStyles() {
        UIManager.put("SplitPane.dividerSize", DIVIDER_SIZE);
        UIManager.put("SplitPane.dividerFocusColor", EasyPostManColors.DIVIDER_FOCUS_COLOR);
        UIManager.put("SplitPaneDivider.background", EasyPostManColors.DIVIDER_BACKGROUND);
        UIManager.put("SplitPaneDivider.border",
                BorderFactory.createLineBorder(EasyPostManColors.DIVIDER_BORDER_COLOR, BORDER_WIDTH));
    }

    /**
     * 应用 FlatLaf 细节增强设置
     */
    private static void applyFlatLafEnhancements() {
        // 标题栏增强
        Map<String, Object> titlePaneProperties = new HashMap<>();
        titlePaneProperties.put("TitlePane.unifiedBackground", true);
        titlePaneProperties.put("TitlePane.showIcon", true);
        titlePaneProperties.put("TitlePane.centerTitle", true);
        titlePaneProperties.put("TitlePane.buttonHoverBackground", EasyPostManColors.SELECTION_BACKGROUND);
        titlePaneProperties.put("TitlePane.buttonPressedBackground", EasyPostManColors.TABLE_SELECTION_BACKGROUND);

        setUIProperties(titlePaneProperties);

        // 菜单悬停效果
        Map<String, Color> menuHoverColors = new HashMap<>();
        menuHoverColors.put("Menu.selectionBackground", EasyPostManColors.TABLE_SELECTION_BACKGROUND);
        menuHoverColors.put("MenuItem.selectionBackground", EasyPostManColors.TABLE_SELECTION_BACKGROUND);
        menuHoverColors.put("Menu.selectionForeground", EasyPostManColors.ACCENT_COLOR);
        menuHoverColors.put("MenuItem.selectionForeground", EasyPostManColors.ACCENT_COLOR);

        setUIProperties(menuHoverColors);

        // 按钮悬停效果
        UIManager.put("Button.hoverBackground", EasyPostManColors.BUTTON_HOVER_BACKGROUND);
        UIManager.put("Button.pressedBackground", EasyPostManColors.BUTTON_PRESSED_BACKGROUND);

        // 滚动条样式
        UIManager.put("ScrollBar.thumb", EasyPostManColors.SCROLLBAR_THUMB);
        UIManager.put("ScrollBar.thumbHover", EasyPostManColors.SCROLLBAR_THUMB_HOVER);
        UIManager.put("ScrollBar.track", EasyPostManColors.SCROLLBAR_TRACK);

        // 分隔符样式
        UIManager.put("Separator.foreground", EasyPostManColors.SEPARATOR_FOREGROUND);
        UIManager.put("Separator.background", EasyPostManColors.SEPARATOR_BACKGROUND);
    }

    /**
     * 批量设置 UI 属性的工具方法
     *
     * @param properties 属性映射表
     */
    private static void setUIProperties(Map<String, ?> properties) {
        if (properties == null) {
            return;
        }

        for (Map.Entry<String, ?> entry : properties.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                UIManager.put(entry.getKey(), entry.getValue());
            }
        }
    }
}