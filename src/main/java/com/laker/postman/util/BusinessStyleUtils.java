package com.laker.postman.util;

import com.laker.postman.common.constants.Colors;

import javax.swing.*;
import java.awt.*;

/**
 * 统一商务风格工具类
 * 用于集中设置 FlatLaf/Swing 全局 UI 属性（如圆角、背景色等）
 */
public class BusinessStyleUtils {

    /**
     * 应用统一商务风格（圆角、背景色等）
     * 建议在 LookAndFeel 设置之后、主界面初始化之前调用
     * UIManager.put 用于设置 Swing 全局 UI 属性（如颜色、圆角、字体、背景等），会影响所有后续创建的组件。
     */
    public static void applyBusinessStyle() {
        // ===== 1. 背景色相关 =====
        UIManager.put("Panel.background", Colors.PANEL_BACKGROUND); // 主体面板背景色
        UIManager.put("MenuBar.background", new Color(240, 242, 245)); // 菜单栏背景色
        UIManager.put("Menu.background", new Color(240, 242, 245)); // 菜单背景色
        UIManager.put("MenuItem.background", new Color(240, 242, 245)); // 菜单项背景色
        UIManager.put("Menu.selectionBackground", new Color(220, 230, 245)); // 菜单选中背景色
        UIManager.put("MenuItem.selectionBackground", new Color(220, 230, 245)); // 菜单项选中背景色
        UIManager.put("ToolBar.background", Colors.PANEL_BACKGROUND); // 工具栏背景色
        // ===== tabbedPane =====
        UIManager.put("TabbedPane.background", Colors.PANEL_BACKGROUND); // 标签页整体背景
        UIManager.put("TabbedPane.tabAreaBackground", Colors.PANEL_BACKGROUND); // 标签栏区域背景
        UIManager.put("TabbedPane.selectedBackground", Colors.PANEL_BACKGROUND); // 选中标签背景
        UIManager.put("TabbedPane.contentAreaColor", Colors.PANEL_BACKGROUND); // 内容区域背景
        // JTabbedPane 选项卡字体和前景色
        UIManager.put("TabbedPane.selectedForeground", new Color(33, 150, 243));
        // ===== JTextArea 颜色 =====
        UIManager.put("TextArea.background", Colors.PANEL_BACKGROUND);
        UIManager.put("TextArea.caretForeground", new Color(33, 150, 243));
        UIManager.put("TextArea.selectionBackground", Colors.PANEL_BACKGROUND);
        UIManager.put("TextArea.selectionForeground", new Color(33, 150, 243));
        // ===== JScrollPane 颜色 =====
        UIManager.put("ScrollPane.background", Colors.PANEL_BACKGROUND);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(new Color(230, 230, 230)));
        // ===== JTable 颜色 =====
        UIManager.put("Table.background", Colors.PANEL_BACKGROUND);
        UIManager.put("Table.selectionBackground", new Color(200, 220, 245));
        UIManager.put("Table.gridColor", new Color(227, 215, 215));
        UIManager.put("TableHeader.background", new Color(240, 242, 245));
        // ===== JLabel 颜色 =====
        UIManager.put("Label.background", Colors.PANEL_BACKGROUND); // 标签背景色
        UIManager.put("Label.disabledBackground", Colors.PANEL_BACKGROUND); // 禁用标签背景色
        // ===== JTree 颜色 =====
        UIManager.put("Tree.background", Colors.PANEL_BACKGROUND); // 树背景色
        // ===== JSplitPane 颜色 =====
        UIManager.put("SplitPane.background", Colors.PANEL_BACKGROUND);
        // ===== JTextPane 颜色 =====
        UIManager.put("TextPane.background", Colors.PANEL_BACKGROUND); // 文本面板背景色
        // ===== List 颜色 =====
        UIManager.put("List.background", Colors.PANEL_BACKGROUND); // 列表背景色
        // ===== EditorPane 颜色 =====
        UIManager.put("EditorPane.background", Colors.PANEL_BACKGROUND); // 编辑器面板背景色

        // ===== 2. 圆角相关 =====
        UIManager.put("Component.arc", 12); // 统一圆角大小（全局组件）
        UIManager.put("Button.arc", 10); // 按钮圆角
        UIManager.put("ProgressBar.arc", 10); // 进度条圆角
        UIManager.put("TextComponent.arc", 10); // 文本组件圆角
        UIManager.put("ScrollBar.thumbArc", 10); // 滚动条滑块圆角
        UIManager.put("TabbedPane.tabAreaArc", 10); // 标签页区域圆角
        UIManager.put("TabbedPane.tabArc", 10); // 标签页圆角

        // ===== 3. 分割线 =====
        UIManager.put("SplitPane.dividerSize", 6); // 分割线宽度
        UIManager.put("SplitPane.dividerFocusColor", new Color(160, 164, 170)); // 分割线获得焦点时颜色
        UIManager.put("SplitPaneDivider.background", new Color(245, 246, 248)); // 分割线背景色
        UIManager.put("SplitPaneDivider.border", BorderFactory.createLineBorder(new Color(200, 200, 200), 1)); // 分割线边框

        // ===== 4. FlatLaf 细节增强 =====
        // 增加窗口阴影
        UIManager.put("TitlePane.unifiedBackground", true);
        UIManager.put("TitlePane.showIcon", true);
        UIManager.put("TitlePane.centerTitle", true);
        UIManager.put("TitlePane.buttonHoverBackground", new Color(220, 230, 245));
        UIManager.put("TitlePane.buttonPressedBackground", new Color(200, 210, 230));
        // 菜单悬停高亮
        UIManager.put("Menu.selectionBackground", new Color(200, 220, 245));
        UIManager.put("MenuItem.selectionBackground", new Color(200, 220, 245));
        UIManager.put("Menu.selectionForeground", new Color(33, 150, 243));
        UIManager.put("MenuItem.selectionForeground", new Color(33, 150, 243));
        // 按钮悬停
        UIManager.put("Button.hoverBackground", new Color(230, 240, 250));
        UIManager.put("Button.pressedBackground", new Color(210, 225, 245));
        // 滚动条更细致
        UIManager.put("ScrollBar.thumb", new Color(220, 225, 230));
        UIManager.put("ScrollBar.thumbHover", new Color(200, 210, 220));
        UIManager.put("ScrollBar.track", new Color(245, 247, 250));
        // 侧边栏分割线更明显
        UIManager.put("Separator.foreground", new Color(210, 210, 210));
        UIManager.put("Separator.background", new Color(240, 240, 240));
    }
}