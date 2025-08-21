package com.laker.postman.common.constants;

import java.awt.*;

/**
 * EasyPostman 应用颜色常量类
 * 统一管理应用中使用的所有颜色
 */
public class EasyPostManColors {

    private EasyPostManColors() {
        // 私有构造函数，防止实例化
    }

    // ==================== 基础颜色 ====================

    // 主色调 - 面板背景色
    public static final Color PANEL_BACKGROUND = new Color(245, 247, 250);

    // 空值列配色
    public static final Color EMPTY_CELL_YELLOW = Color.WHITE;

    // ==================== 菜单相关颜色 ====================

    // 菜单背景色
    public static final Color MENU_BACKGROUND = new Color(240, 242, 245);

    // 选中背景色
    public static final Color SELECTION_BACKGROUND = new Color(220, 230, 245);

    // ==================== 主题色彩 ====================

    // 强调色/主色调
    public static final Color ACCENT_COLOR = new Color(33, 150, 243);

    // ==================== 表格相关颜色 ====================

    // 表格选中背景色
    public static final Color TABLE_SELECTION_BACKGROUND = new Color(200, 220, 245);

    // 表格网格线颜色
    public static final Color TABLE_GRID_COLOR = new Color(227, 215, 215);

    // 表格头部背景色
    public static final Color TABLE_HEADER_BACKGROUND = new Color(240, 242, 245);

    // ==================== 边框和分割线颜色 ====================

    // 边框颜色
    public static final Color BORDER_COLOR = new Color(230, 230, 230);

    // 分割线焦点颜色
    public static final Color DIVIDER_FOCUS_COLOR = new Color(160, 164, 170);

    // 分割线背景色
    public static final Color DIVIDER_BACKGROUND = new Color(245, 246, 248);

    // 分割线边框颜色
    public static final Color DIVIDER_BORDER_COLOR = new Color(200, 200, 200);

    // ==================== 按钮相关颜色 ====================

    // 按钮悬停背景色
    public static final Color BUTTON_HOVER_BACKGROUND = new Color(230, 240, 250);

    // 按钮按下背景色
    public static final Color BUTTON_PRESSED_BACKGROUND = new Color(210, 225, 245);

    // ==================== 滚动条相关颜色 ====================

    // 滚动条滑块颜色
    public static final Color SCROLLBAR_THUMB = new Color(220, 225, 230);

    // 滚动条滑块悬停颜色
    public static final Color SCROLLBAR_THUMB_HOVER = new Color(200, 210, 220);

    // 滚动条轨道颜色
    public static final Color SCROLLBAR_TRACK = new Color(245, 247, 250);

    // ==================== 分隔符相关颜色 ====================

    // 分隔符前景色
    public static final Color SEPARATOR_FOREGROUND = new Color(210, 210, 210);

    // 分隔符背景色
    public static final Color SEPARATOR_BACKGROUND = new Color(240, 240, 240);
}
