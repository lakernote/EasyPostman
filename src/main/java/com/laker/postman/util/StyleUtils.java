package com.laker.postman.util;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;
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
@UtilityClass
public class StyleUtils {

    // 圆角常量定义
    private static final int DEFAULT_ARC = 12;
    private static final int BUTTON_ARC = 10;
    private static final int COMPONENT_ARC = 10;

    // 分割线常量
    private static final int DIVIDER_SIZE = 6;
    private static final int BORDER_WIDTH = 1;

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
        Color panelBg = ModernColors.PANEL_BACKGROUND;
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
        Color menuBg = ModernColors.MENU_BACKGROUND;
        properties.put("MenuBar.background", menuBg);
        properties.put("Menu.background", menuBg);
        properties.put("MenuItem.background", menuBg);

        // TabbedPane 背景色
        properties.put("TabbedPane.background", panelBg);
        properties.put("TabbedPane.tabAreaBackground", panelBg);
        properties.put("TabbedPane.selectedBackground", panelBg);
        properties.put("TabbedPane.contentAreaColor", panelBg);

        // 选中背景色设置
        properties.put("Menu.selectionBackground", ModernColors.SELECTION_BACKGROUND);
        properties.put("MenuItem.selectionBackground", ModernColors.SELECTION_BACKGROUND);
        properties.put("TextArea.selectionBackground", panelBg);

        // Table 选中效果：使用表格选中背景色，更加明显
        properties.put("Table.selectionBackground", ModernColors.TABLE_SELECTION_BACKGROUND);

        // List 选中效果：使用表格选中背景色，更加明显
        properties.put("List.selectionBackground", ModernColors.TABLE_SELECTION_BACKGROUND);

        // Tree 选中效果：使用表格选中背景色，更加明显
        properties.put("Tree.selectionBackground", ModernColors.TABLE_SELECTION_BACKGROUND);

        // Dialog 样式增强
        properties.put("Dialog.background", panelBg);
        properties.put("OptionPane.background", panelBg);
        properties.put("OptionPane.buttonArea.background", panelBg);
        properties.put("OptionPane.messageArea.background", panelBg);

        // 前景色设置
        Color accentColor = ModernColors.ACCENT_COLOR;
        properties.put("TextArea.caretForeground", accentColor);
        properties.put("TextArea.selectionForeground", accentColor);

        // 特殊属性设置
        properties.put("Table.gridColor", ModernColors.TABLE_GRID_COLOR);
        properties.put("TableHeader.background", ModernColors.TABLE_HEADER_BACKGROUND);
        properties.put("ScrollPane.border", BorderFactory.createLineBorder(ModernColors.BORDER_COLOR));
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
        properties.put("SplitPane.dividerFocusColor", ModernColors.DIVIDER_FOCUS_COLOR);
        properties.put("SplitPaneDivider.background", ModernColors.DIVIDER_BACKGROUND);
        properties.put("SplitPaneDivider.border",
                BorderFactory.createLineBorder(ModernColors.DIVIDER_BORDER_COLOR, BORDER_WIDTH));
    }

    /**
     * 添加 FlatLaf 细节增强设置
     */
    private static void addFlatLafEnhancements(Map<String, Object> properties) {
        // 标题栏增强
        properties.put("TitlePane.unifiedBackground", true);
        properties.put("TitlePane.showIcon", true);
        properties.put("TitlePane.centerTitle", true);
        properties.put("TitlePane.buttonHoverBackground", ModernColors.SELECTION_BACKGROUND);
        properties.put("TitlePane.buttonPressedBackground", ModernColors.TABLE_SELECTION_BACKGROUND);

        // 菜单悬停效果
        Color tableSelectionBg = ModernColors.TABLE_SELECTION_BACKGROUND;
        Color accentColor = ModernColors.ACCENT_COLOR;
        properties.put("Menu.selectionBackground", tableSelectionBg);
        properties.put("MenuItem.selectionBackground", tableSelectionBg);
        properties.put("Menu.selectionForeground", accentColor);
        properties.put("MenuItem.selectionForeground", accentColor);

        // 按钮悬停效果
        properties.put("Button.hoverBackground", ModernColors.BUTTON_HOVER_BACKGROUND);
        properties.put("Button.pressedBackground", ModernColors.BUTTON_PRESSED_BACKGROUND);

        // 滚动条样式
        properties.put("ScrollBar.thumb", ModernColors.SCROLLBAR_THUMB);
        properties.put("ScrollBar.thumbHover", ModernColors.SCROLLBAR_THUMB_HOVER);
        properties.put("ScrollBar.track", ModernColors.SCROLLBAR_TRACK);

        // 分隔符样式
        properties.put("Separator.foreground", ModernColors.SEPARATOR_FOREGROUND);
        properties.put("Separator.background", ModernColors.SEPARATOR_BACKGROUND);
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