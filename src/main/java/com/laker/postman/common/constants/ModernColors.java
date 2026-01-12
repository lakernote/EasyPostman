package com.laker.postman.common.constants;

import com.formdev.flatlaf.FlatLaf;

import java.awt.*;

/**
 * 现代化UI配色方案
 * 统一的配色常量，确保整个应用的视觉一致性
 * 支持亮色和暗色主题自适应
 * <p>
 * 设计理念：
 * - 主色调：Blue（信任、专业、科技）
 * - 辅助色：Sky Blue（清新、现代）
 * - 强调色：Cyan（活力、突出）
 * - 中性色：Slate（优雅、易读）
 *
 */
public final class ModernColors {

    // ==================== 主题检测 ====================

    /**
     * 检查当前是否为暗色主题
     */
    private static boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    // ==================== 主色系 ====================

    /**
     * 主色 - Blue-500（iOS蓝）
     */
    public static final Color PRIMARY = new Color(0, 122, 255);

    /**
     * 主色深色 - Blue-600
     */
    public static final Color PRIMARY_DARK = new Color(0, 102, 221);

    /**
     * 主色超深 - Blue-700
     */
    public static final Color PRIMARY_DARKER = new Color(0, 88, 191);

    /**
     * 主色浅色 - Blue-400
     */
    public static final Color PRIMARY_LIGHT = new Color(51, 153, 255);

    /**
     * 主色超浅 - Blue-100
     */
    public static final Color PRIMARY_LIGHTER = new Color(219, 234, 254);

    // ==================== 辅助色系 ====================

    /**
     * 辅助色 - Sky-500
     */
    public static final Color SECONDARY = new Color(14, 165, 233);

    /**
     * 辅助色深色 - Sky-600
     */
    public static final Color SECONDARY_DARK = new Color(2, 132, 199);

    /**
     * 辅助色浅色 - Sky-400
     */
    public static final Color SECONDARY_LIGHT = new Color(56, 189, 248);

    /**
     * 辅助色超浅 - Sky-100
     */
    public static final Color SECONDARY_LIGHTER = new Color(224, 242, 254);

    // ==================== 强调色系 ====================

    /**
     * 强调色 - Cyan-500
     */
    public static final Color ACCENT = new Color(6, 182, 212);

    /**
     * 强调色深色 - Cyan-600
     */
    public static final Color ACCENT_DARK = new Color(8, 145, 178);

    /**
     * 强调色浅色 - Cyan-400
     */
    public static final Color ACCENT_LIGHT = new Color(34, 211, 238);

    // ==================== 蓝色系 ====================

    /**
     * 蓝色 - Blue-500
     */
    public static final Color BLUE = new Color(59, 130, 246);

    /**
     * 蓝色深色 - Blue-700
     */
    public static final Color BLUE_DARK = new Color(29, 78, 216);

    /**
     * 蓝色浅色 - Blue-100
     */
    public static final Color BLUE_LIGHT = new Color(219, 234, 254);

    // ==================== 成功/错误/警告色 ====================

    /**
     * 成功 - Green-500
     */
    public static final Color SUCCESS = new Color(34, 197, 94);

    /**
     * 成功深色 - Green-600
     */
    public static final Color SUCCESS_DARK = new Color(22, 163, 74);

    /**
     * 成功浅色 - Green-100
     */
    public static final Color SUCCESS_LIGHT = new Color(220, 252, 231);

    /**
     * 错误 - Red-500
     */
    public static final Color ERROR = new Color(239, 68, 68);

    /**
     * 错误深色 - Red-600
     */
    public static final Color ERROR_DARK = new Color(220, 38, 38);

    /**
     * 错误超深 - Red-700
     */
    public static final Color ERROR_DARKER = new Color(185, 28, 28);

    /**
     * 错误浅色 - Red-100
     */
    public static final Color ERROR_LIGHT = new Color(254, 226, 226);

    /**
     * 警告 - Amber-500
     */
    public static final Color WARNING = new Color(245, 158, 11);

    /**
     * 警告深色 - Amber-600
     */
    public static final Color WARNING_DARK = new Color(217, 119, 6);

    /**
     * 警告超深 - Amber-700
     */
    public static final Color WARNING_DARKER = new Color(180, 83, 9);

    /**
     * 警告浅色 - Amber-100
     */
    public static final Color WARNING_LIGHT = new Color(254, 243, 199);

    /**
     * 信息 - Cyan-500
     */
    public static final Color INFO = new Color(6, 182, 212);

    /**
     * 信息浅色 - Cyan-100
     */
    public static final Color INFO_LIGHT = new Color(207, 250, 254);

    // ==================== 中性色系（用于 Close 等操作）====================

    /**
     * 中性 - Slate-500
     */
    public static final Color NEUTRAL = new Color(100, 116, 139);

    /**
     * 中性深色 - Slate-600
     */
    public static final Color NEUTRAL_DARK = new Color(71, 85, 105);

    /**
     * 中性超深 - Slate-700
     */
    public static final Color NEUTRAL_DARKER = new Color(51, 65, 85);

    // ==================== Git 操作颜色（现代简约风格）====================

    /**
     * Git Commit（提交）- 柔和翠绿色
     */
    public static final Color GIT_COMMIT = new Color(34, 197, 94);  // Green-500

    /**
     * Git Push（推送）- 优雅深蓝色
     */
    public static final Color GIT_PUSH = new Color(59, 130, 246);  // Blue-500

    /**
     * Git Pull（拉取）- 清新紫色
     */
    public static final Color GIT_PULL = new Color(168, 85, 247);  // Purple-500

    // ==================== 中性色系 - 文字（主题适配）====================

    /**
     * 文字主色 - 根据主题自适应
     * 亮色主题：Slate-900 (深色文字)
     * 暗色主题：Slate-100 (浅色文字)
     */
    public static Color getTextPrimary() {
        return isDarkTheme() ? new Color(241, 245, 249) : new Color(15, 23, 42);
    }

    /**
     * 文字次要色 - 根据主题自适应
     * 亮色主题：Slate-700
     * 暗色主题：Slate-300
     */
    public static Color getTextSecondary() {
        return isDarkTheme() ? new Color(203, 213, 225) : new Color(51, 65, 85);
    }

    /**
     * 文字提示色 - 根据主题自适应
     * 亮色主题：Slate-500
     * 暗色主题：Slate-400
     */
    public static Color getTextHint() {
        return isDarkTheme() ? new Color(148, 163, 184) : new Color(100, 116, 139);
    }

    /**
     * 文字禁用色 - 根据主题自适应
     * 亮色主题：Slate-400
     * 暗色主题：Slate-600
     */
    public static Color getTextDisabled() {
        return isDarkTheme() ? new Color(71, 85, 105) : new Color(148, 163, 184);
    }

    /**
     * 文字反色（用于深色背景）- 根据主题自适应
     * 亮色主题：接近白色
     * 暗色主题：接近白色（保持一致）
     */
    public static Color getTextInverse() {
        return new Color(248, 250, 252);
    }

    // ==================== 中性色系 - 背景 ====================

    /**
     * 背景白色
     */
    public static final Color BG_WHITE = new Color(255, 255, 255);

    /**
     * 背景浅灰 - Slate-50
     */
    public static final Color BG_LIGHT = new Color(248, 250, 252);

    /**
     * 背景中灰 - Slate-100
     */
    public static final Color BG_MEDIUM = new Color(241, 245, 249);

    /**
     * 背景深灰 - Slate-200
     */
    public static final Color BG_DARK = new Color(226, 232, 240);

    // ==================== 边框色 ====================

    /**
     * 边框浅色 - Slate-200
     */
    public static final Color BORDER_LIGHT = new Color(226, 232, 240);

    /**
     * 边框中色 - Slate-300
     */
    public static final Color BORDER_MEDIUM = new Color(203, 213, 225);

    /**
     * 获取分隔线/边框颜色 - 主题适配
     * 用于面板之间的分隔线、边框等
     * 亮色主题：浅灰色（LIGHT_GRAY）
     * 暗色主题：比背景亮的灰色，有明显区分度
     */
    public static Color getDividerBorderColor() {
        if (isDarkTheme()) {
            return new Color(80, 83, 85);
        } else {
            // 亮色主题：使用 LIGHT_GRAY
            return Color.LIGHT_GRAY;
        }
    }

    // ==================== 特殊效果色 ====================

    /**
     * 悬停背景
     */
    public static final Color HOVER_BG = new Color(241, 245, 249);

    /**
     * 选中背景
     */
    public static final Color SELECTED_BG = new Color(224, 231, 255);

    // ==================== EasyPostman 应用颜色（从 EasyPostManColors 迁移） ====================

    // 基础颜色
    /**
     * 主色调 - 面板背景色
     */
    public static final Color PANEL_BACKGROUND = new Color(245, 247, 250);

    /**
     * 获取空单元格背景色 - 主题适配
     * 亮色主题：白色（清晰区分）
     * 暗色主题：比有值单元格稍亮的颜色（便于区分空单元格）
     */
    public static Color getEmptyCellBackground() {
        if (isDarkTheme()) {
            return new Color(85, 87, 89);
        } else {
            // 亮色主题：保持白色
            return Color.WHITE;
        }
    }

    // ==================== Table 表格专用色（主题适配）====================

    /**
     * 获取表格选中背景色 - 主题适配
     * 亮色主题：浅蓝色 (220, 235, 252)
     * 暗色主题：深蓝色 (75, 110, 175)
     */
    public static Color getTableSelectionBackground() {
        return isDarkTheme() ? new Color(75, 110, 175) : new Color(220, 235, 252);
    }

    /**
     * 获取表格选中前景色 - 主题适配
     * 亮色主题：深色文字
     * 暗色主题：浅色文字
     */
    public static Color getTableSelectionForeground() {
        return isDarkTheme() ? new Color(255, 255, 255) : new Color(0, 0, 0);
    }

    /**
     * 获取表格网格线颜色 - 主题适配
     * 亮色主题：浅灰色 (237, 237, 237)
     * 暗色主题：比背景稍亮 (75, 77, 80)
     */
    public static Color getTableGridColor() {
        return isDarkTheme() ? new Color(75, 77, 80) : new Color(237, 237, 237);
    }

    /**
     * 获取表格头部背景色 - 主题适配
     * 亮色主题：BG_MEDIUM (241, 245, 249)
     * 暗色主题：比背景稍亮 (70, 73, 75)
     */
    public static Color getTableHeaderBackground() {
        return isDarkTheme() ? new Color(70, 73, 75) : BG_MEDIUM;
    }

    /**
     * 获取表格头部前景色 - 主题适配
     * 亮色主题：深色文字
     * 暗色主题：浅色文字
     */
    public static Color getTableHeaderForeground() {
        return getTextPrimary();
    }

    // ==================== 滚动条相关颜色 ====================
    /**
     * 滚动条滑块颜色
     */
    public static final Color SCROLLBAR_THUMB = new Color(220, 225, 230);

    /**
     * 滚动条滑块悬停颜色
     */
    public static final Color SCROLLBAR_THUMB_HOVER = new Color(200, 210, 220);

    /**
     * 滚动条轨道颜色
     */
    public static final Color SCROLLBAR_TRACK = new Color(245, 247, 250);


    /**
     * 验证错误图标色
     */
    public static final Color VALIDATION_ERROR_ICON = ERROR;

    /**
     * 未保存更改警告背景色
     */
    public static final Color SETTINGS_UNSAVED_WARNING_BG = new Color(255, 243, 205);

    /**
     * 未保存更改警告边框色
     */
    public static final Color SETTINGS_UNSAVED_WARNING_BORDER = new Color(255, 193, 7);

    // ==================== 交互状态色 ====================

    /**
     * 已修改状态
     */
    public static final Color STATE_MODIFIED = new Color(255, 193, 7); // 黄色

    /**
     * 重置图标颜色
     */
    public static final Color ICON_RESET = new Color(156, 163, 175); // 灰色


    // ==================== Console 控制台专用色（主题适配）====================

    /**
     * Console 背景色 - 根据主题自适应
     */
    public static Color getConsoleBackground() {
        return isDarkTheme() ? new Color(30, 30, 35) : new Color(250, 251, 252);
    }

    /**
     * Console 文本区域背景色 - 根据主题自适应
     */
    public static Color getConsoleTextAreaBg() {
        return isDarkTheme() ? new Color(40, 40, 45) : new Color(255, 255, 255);
    }

    /**
     * Console 普通文本颜色 - 根据主题自适应
     */
    public static Color getConsoleText() {
        return isDarkTheme() ? new Color(220, 220, 220) : new Color(51, 65, 85);
    }

    /**
     * Console INFO 级别颜色 - 根据主题自适应
     */
    public static Color getConsoleInfo() {
        return isDarkTheme() ? new Color(96, 165, 250) : new Color(37, 99, 235);
    }

    /**
     * Console DEBUG 级别颜色 - 根据主题自适应
     */
    public static Color getConsoleDebug() {
        return isDarkTheme() ? new Color(74, 222, 128) : new Color(22, 163, 74);
    }

    /**
     * Console WARN 级别颜色 - 根据主题自适应
     */
    public static Color getConsoleWarn() {
        return isDarkTheme() ? new Color(251, 146, 60) : new Color(234, 88, 12);
    }

    /**
     * Console ERROR 级别颜色 - 根据主题自适应
     */
    public static Color getConsoleError() {
        return isDarkTheme() ? new Color(248, 113, 113) : new Color(220, 38, 38);
    }

    /**
     * Console 时间戳颜色 - 根据主题自适应
     */
    public static Color getConsoleTimestamp() {
        return isDarkTheme() ? new Color(148, 163, 184) : new Color(100, 116, 139);
    }

    /**
     * Console 类名颜色 - 根据主题自适应
     */
    public static Color getConsoleClassName() {
        return isDarkTheme() ? new Color(192, 132, 252) : new Color(147, 51, 234);
    }

    /**
     * Console 方法名颜色 - 根据主题自适应
     */
    public static Color getConsoleMethodName() {
        return isDarkTheme() ? new Color(56, 189, 248) : new Color(14, 165, 233);
    }

    /**
     * Console 工具栏背景色 - 根据主题自适应
     */
    public static Color getConsoleToolbarBg() {
        return isDarkTheme() ? new Color(35, 35, 40) : new Color(248, 250, 252);
    }

    /**
     * Console 工具栏边框色 - 根据主题自适应
     */
    public static Color getConsoleToolbarBorder() {
        return isDarkTheme() ? new Color(60, 60, 70) : new Color(226, 232, 240);
    }

    /**
     * Console 按钮悬停背景色 - 根据主题自适应
     */
    public static Color getConsoleButtonHover() {
        return isDarkTheme() ? new Color(50, 50, 60) : new Color(224, 242, 254);
    }

    /**
     * Console 滚动条颜色 - 根据主题自适应
     */
    public static Color getConsoleScrollbar() {
        return isDarkTheme() ? new Color(80, 80, 90) : new Color(203, 213, 225);
    }

    /**
     * Console 滚动条悬停颜色 - 根据主题自适应
     */
    public static Color getConsoleScrollbarHover() {
        return isDarkTheme() ? new Color(100, 100, 110) : new Color(148, 163, 184);
    }

    /**
     * Console 选中文本背景色 - 根据主题自适应
     */
    public static Color getConsoleSelectionBg() {
        return isDarkTheme() ? new Color(60, 90, 120) : new Color(191, 219, 254);
    }

    /**
     * Console 当前行高亮色 - 根据主题自适应
     */
    public static Color getConsoleCurrentLine() {
        return isDarkTheme() ? new Color(45, 45, 50) : new Color(248, 250, 252);
    }

    // ==================== UI 组件主题适配色（用于 Settings 等面板）====================

    /**
     * 获取主背景色 - 根据主题自适应
     * 亮色主题：BG_LIGHT (248, 250, 252)
     * 暗色主题：深灰色 (60, 63, 65) - 基于 IntelliJ IDEA Darcula
     */
    public static Color getBackgroundColor() {
        return isDarkTheme() ? new Color(60, 63, 65) : BG_LIGHT;
    }

    /**
     * 获取卡片/区域背景色 - 根据主题自适应
     * 亮色主题：BG_WHITE (255, 255, 255)
     * 暗色主题：稍浅的深灰色 (55, 57, 59)
     */
    public static Color getCardBackgroundColor() {
        return isDarkTheme() ? new Color(55, 57, 59) : BG_WHITE;
    }

    /**
     * 获取输入框背景色 - 根据主题自适应
     * 亮色主题：BG_WHITE (255, 255, 255)
     * 暗色主题：比卡片背景稍亮 (65, 68, 70)
     */
    public static Color getInputBackgroundColor() {
        return isDarkTheme() ? new Color(65, 68, 70) : BG_WHITE;
    }

    /**
     * 获取边框颜色（浅色）- 根据主题自适应
     * 亮色主题：BORDER_LIGHT (226, 232, 240)
     * 暗色主题：比背景稍亮 (75, 77, 80)
     */
    public static Color getBorderLightColor() {
        return isDarkTheme() ? new Color(75, 77, 80) : BORDER_LIGHT;
    }

    /**
     * 获取边框颜色（中等）- 根据主题自适应
     * 亮色主题：BORDER_MEDIUM (203, 213, 225)
     * 暗色主题：更明显的边框 (85, 87, 90)
     */
    public static Color getBorderMediumColor() {
        return isDarkTheme() ? new Color(85, 87, 90) : BORDER_MEDIUM;
    }

    /**
     * 获取悬停背景色 - 根据主题自适应
     * 亮色主题：HOVER_BG (241, 245, 249)
     * 暗色主题：比背景稍亮 (70, 73, 75)
     */
    public static Color getHoverBackgroundColor() {
        return isDarkTheme() ? new Color(70, 73, 75) : HOVER_BG;
    }

    /**
     * 获取按钮暗色背景（按下状态）- 根据主题自适应
     * 亮色主题：BG_DARK (226, 232, 240)
     * 暗色主题：比背景更暗 (50, 52, 54)
     */
    public static Color getButtonDarkColor() {
        return isDarkTheme() ? new Color(50, 52, 54) : BG_DARK;
    }

    /**
     * 获取滚动条轨道颜色 - 根据主题自适应
     * 亮色主题：SCROLLBAR_TRACK (245, 247, 250)
     * 暗色主题：与主背景相同 (60, 63, 65)
     */
    public static Color getScrollbarTrackColor() {
        return isDarkTheme() ? new Color(60, 63, 65) : SCROLLBAR_TRACK;
    }

    /**
     * 获取滚动条滑块颜色 - 根据主题自适应
     * 亮色主题：SCROLLBAR_THUMB (220, 225, 230)
     * 暗色主题：比背景亮的灰色 (85, 87, 90)
     */
    public static Color getScrollbarThumbColor() {
        return isDarkTheme() ? new Color(85, 87, 90) : SCROLLBAR_THUMB;
    }

    /**
     * 获取滚动条滑块悬停颜色 - 根据主题自适应
     * 亮色主题：SCROLLBAR_THUMB_HOVER (200, 210, 220)
     * 暗色主题：更亮的灰色 (100, 102, 105)
     */
    public static Color getScrollbarThumbHoverColor() {
        return isDarkTheme() ? new Color(100, 102, 105) : SCROLLBAR_THUMB_HOVER;
    }

    /**
     * 获取阴影颜色 - 根据主题自适应
     * 暗色主题使用更柔和的阴影（黑色，增强透明度）
     * 亮色主题使用深蓝黑色调
     *
     * @param alpha 基础透明度 (0-255)
     */
    public static Color getShadowColor(int alpha) {
        if (isDarkTheme()) {
            return new Color(0, 0, 0, (int) (alpha * 1.5));
        }
        return new Color(15, 23, 42, alpha);
    }

    /**
     * 获取警告背景色 - 根据主题自适应
     * 亮色主题：SETTINGS_UNSAVED_WARNING_BG (255, 243, 205)
     * 暗色主题：暗黄色调 (70, 65, 50)
     */
    public static Color getWarningBackgroundColor() {
        return isDarkTheme() ? new Color(70, 65, 50) : SETTINGS_UNSAVED_WARNING_BG;
    }

    /**
     * 获取警告边框颜色 - 根据主题自适应
     * 亮色主题：SETTINGS_UNSAVED_WARNING_BORDER (255, 193, 7)
     * 暗色主题：较亮的黄色 (120, 100, 60)
     */
    public static Color getWarningBorderColor() {
        return isDarkTheme() ? new Color(120, 100, 60) : SETTINGS_UNSAVED_WARNING_BORDER;
    }

    // ==================== 透明度变体 ====================

    /**
     * 获取带透明度的主色
     *
     * @param alpha 透明度 (0-255)
     */
    public static Color primaryWithAlpha(int alpha) {
        return new Color(PRIMARY.getRed(), PRIMARY.getGreen(), PRIMARY.getBlue(), alpha);
    }

    /**
     * 获取带透明度的辅助色
     *
     * @param alpha 透明度 (0-255)
     */
    public static Color secondaryWithAlpha(int alpha) {
        return new Color(SECONDARY.getRed(), SECONDARY.getGreen(), SECONDARY.getBlue(), alpha);
    }

    /**
     * 获取带透明度的强调色
     *
     * @param alpha 透明度 (0-255)
     */
    public static Color accentWithAlpha(int alpha) {
        return new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), alpha);
    }

    /**
     * 获取带透明度的白色
     *
     * @param alpha 透明度 (0-255)
     */
    public static Color whiteWithAlpha(int alpha) {
        return new Color(255, 255, 255, alpha);
    }

    /**
     * 获取带透明度的黑色
     *
     * @param alpha 透明度 (0-255)
     */
    public static Color blackWithAlpha(int alpha) {
        return new Color(0, 0, 0, alpha);
    }

    // ==================== 渐变帮助方法 ====================

    /**
     * 创建主色渐变
     */
    public static GradientPaint createPrimaryGradient(int width, int height) {
        return new GradientPaint(0, 0, PRIMARY, width, height, PRIMARY_DARK);
    }

    /**
     * 创建辅助色渐变
     */
    public static GradientPaint createSecondaryGradient(int width, int height) {
        return new GradientPaint(0, 0, SECONDARY_LIGHT, width, height, SECONDARY_DARK);
    }

    /**
     * 创建主色到辅助色渐变
     */
    public static GradientPaint createPrimaryToSecondaryGradient(int width, int height) {
        return new GradientPaint(0, 0, PRIMARY, width, height, SECONDARY);
    }

    /**
     * 创建深色背景渐变
     */
    public static GradientPaint createDarkBackgroundGradient(int width, int height) {
        return new GradientPaint(0, 0, PRIMARY_DARKER, width, height, BLUE_DARK);
    }

    /**
     * 混合两种颜色
     *
     * @param c1    颜色1
     * @param c2    颜色2
     * @param ratio 混合比例 (0.0 = 全部c1, 1.0 = 全部c2)
     */
    public static Color blendColors(Color c1, Color c2, float ratio) {
        float invRatio = 1 - ratio;
        int r = (int) (c1.getRed() * invRatio + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * invRatio + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * invRatio + c2.getBlue() * ratio);
        int a = (int) (c1.getAlpha() * invRatio + c2.getAlpha() * ratio);
        return new Color(r, g, b, a);
    }
}
