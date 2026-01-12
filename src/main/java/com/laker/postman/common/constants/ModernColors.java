package com.laker.postman.common.constants;

import com.formdev.flatlaf.FlatLaf;
import lombok.experimental.UtilityClass;

import java.awt.*;

/**
 * 现代化UI配色方案
 * 统一的配色常量，确保整个应用的视觉一致性
 * 完全支持亮色和暗色主题自适应
 * <p>
 * 设计理念：
 * - 主色调：Blue（信任、专业、科技）
 * - 辅助色：Sky Blue（清新、现代）
 * - 强调色：Cyan（活力、突出）
 * - 中性色：Slate（优雅、易读）
 * <p>
 * - 背景、文字、边框等UI元素颜色全部主题自适应
 *
 */
@UtilityClass
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
     * 强调色浅色 - Cyan-400
     */
    public static final Color ACCENT_LIGHT = new Color(34, 211, 238);

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
     * 信息 - Cyan-500
     */
    public static final Color INFO = new Color(6, 182, 212);

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

    // ==================== 文字颜色（主题适配）====================

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
     * 文字反色（用于深色背景）- 主题自适应
     * 两种主题都返回浅色文字
     */
    public static Color getTextInverse() {
        return new Color(248, 250, 252);
    }

    // ==================== 背景颜色（主题适配）====================

    /**
     * 获取主背景色 - 根据主题自适应
     * 亮色主题：Slate-50 (248, 250, 252)
     * 暗色主题：深灰色 (60, 63, 65) - 基于 IntelliJ IDEA Darcula，对应 @panelBg
     */
    public static Color getBackgroundColor() {
        return isDarkTheme() ? new Color(60, 63, 65) : new Color(248, 250, 252);
    }

    /**
     * 获取卡片/区域背景色 - 根据主题自适应
     * 亮色主题：白色 (255, 255, 255)
     * 暗色主题：稍浅的深灰色 (55, 57, 59)
     */
    public static Color getCardBackgroundColor() {
        return isDarkTheme() ? new Color(55, 57, 59) : new Color(255, 255, 255);
    }

    /**
     * 获取输入框背景色 - 根据主题自适应
     * 亮色主题：白色 (255, 255, 255)
     * 暗色主题：比卡片背景稍亮 (65, 68, 70)
     */
    public static Color getInputBackgroundColor() {
        return isDarkTheme() ? new Color(65, 68, 70) : new Color(255, 255, 255);
    }

    /**
     * 获取悬停背景色 - 根据主题自适应
     * 亮色主题：Slate-100 (241, 245, 249)
     * 暗色主题：比背景稍亮 (70, 73, 75)
     */
    public static Color getHoverBackgroundColor() {
        return isDarkTheme() ? new Color(70, 73, 75) : new Color(241, 245, 249);
    }


    /**
     * 获取按钮按下状态背景 - 根据主题自适应
     * 亮色主题：Slate-200 (226, 232, 240)
     * 暗色主题：比背景更暗 (50, 52, 54)
     */
    public static Color getButtonPressedColor() {
        return isDarkTheme() ? new Color(50, 52, 54) : new Color(226, 232, 240);
    }

    // ==================== 边框颜色（主题适配）====================

    /**
     * 获取边框颜色（浅色）- 根据主题自适应
     * 亮色主题：Slate-200 (226, 232, 240)
     * 暗色主题：比背景稍亮 (75, 77, 80) - 对应 @borderColor
     */
    public static Color getBorderLightColor() {
        return isDarkTheme() ? new Color(75, 77, 80) : new Color(226, 232, 240);
    }

    /**
     * 获取边框颜色（中等）- 根据主题自适应
     * 亮色主题：Slate-300 (203, 213, 225)
     * 暗色主题：更明显的边框 (85, 87, 90)
     */
    public static Color getBorderMediumColor() {
        return isDarkTheme() ? new Color(85, 87, 90) : new Color(203, 213, 225);
    }

    /**
     * 获取分隔线/边框颜色 - 主题适配
     * 用于面板之间的分隔线、边框等
     * 亮色主题：浅灰色 (211, 211, 211)
     * 暗色主题：比背景亮的灰色 (80, 83, 85)
     */
    public static Color getDividerBorderColor() {
        return isDarkTheme() ? new Color(80, 83, 85) : new Color(211, 211, 211);
    }

    /**
     * 获取空单元格背景色 - 主题适配
     * 亮色主题：白色（清晰区分）
     * 暗色主题：比有值单元格稍亮的颜色（便于区分空单元格）
     */
    public static Color getEmptyCellBackground() {
        return isDarkTheme() ? new Color(85, 87, 89) : Color.WHITE;
    }

    // ==================== 滚动条颜色（主题适配）====================

    /**
     * 获取滚动条轨道颜色 - 根据主题自适应
     * 亮色主题：Slate-50 (245, 247, 250)
     * 暗色主题：与主背景相同 (60, 63, 65)
     */
    public static Color getScrollbarTrackColor() {
        return isDarkTheme() ? new Color(60, 63, 65) : new Color(245, 247, 250);
    }

    /**
     * 获取滚动条滑块颜色 - 根据主题自适应
     * 亮色主题：浅灰 (220, 225, 230)
     * 暗色主题：比背景亮的灰色 (85, 87, 90)
     */
    public static Color getScrollbarThumbColor() {
        return isDarkTheme() ? new Color(85, 87, 90) : new Color(220, 225, 230);
    }

    /**
     * 获取滚动条滑块悬停颜色 - 根据主题自适应
     * 亮色主题：中灰 (200, 210, 220)
     * 暗色主题：更亮的灰色 (100, 102, 105)
     */
    public static Color getScrollbarThumbHoverColor() {
        return isDarkTheme() ? new Color(100, 102, 105) : new Color(200, 210, 220);
    }

    // ==================== 警告/提示颜色（主题适配）====================

    /**
     * 获取警告背景色 - 根据主题自适应
     * 亮色主题：浅黄色 (255, 243, 205)
     * 暗色主题：暗黄色调 (70, 65, 50)
     */
    public static Color getWarningBackgroundColor() {
        return isDarkTheme() ? new Color(70, 65, 50) : new Color(255, 243, 205);
    }

    /**
     * 获取警告边框颜色 - 根据主题自适应
     * 亮色主题：黄色 (255, 193, 7)
     * 暗色主题：较亮的黄色 (120, 100, 60)
     */
    public static Color getWarningBorderColor() {
        return isDarkTheme() ? new Color(120, 100, 60) : new Color(255, 193, 7);
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

    // ==================== Console 控制台专用色（主题适配）====================

    /**
     * Console 文本区域背景色 - 根据主题自适应
     * 亮色主题：白色 (255, 255, 255)
     * 暗色主题：深灰 (40, 40, 45)
     */
    public static Color getConsoleTextAreaBg() {
        return isDarkTheme() ? new Color(40, 40, 45) : new Color(255, 255, 255);
    }

    /**
     * Console 普通文本颜色 - 根据主题自适应
     * 亮色主题：Slate-700 (51, 65, 85)
     * 暗色主题：浅灰 (220, 220, 220)
     */
    public static Color getConsoleText() {
        return isDarkTheme() ? new Color(220, 220, 220) : new Color(51, 65, 85);
    }

    /**
     * Console INFO 级别颜色 - 根据主题自适应
     * 亮色主题：Blue-700 (37, 99, 235)
     * 暗色主题：Blue-400 (96, 165, 250)
     */
    public static Color getConsoleInfo() {
        return isDarkTheme() ? new Color(96, 165, 250) : new Color(37, 99, 235);
    }

    /**
     * Console DEBUG 级别颜色 - 根据主题自适应
     * 亮色主题：Green-600 (22, 163, 74)
     * 暗色主题：Green-400 (74, 222, 128)
     */
    public static Color getConsoleDebug() {
        return isDarkTheme() ? new Color(74, 222, 128) : new Color(22, 163, 74);
    }

    /**
     * Console WARN 级别颜色 - 根据主题自适应
     * 亮色主题：Orange-600 (234, 88, 12)
     * 暗色主题：Orange-400 (251, 146, 60)
     */
    public static Color getConsoleWarn() {
        return isDarkTheme() ? new Color(251, 146, 60) : new Color(234, 88, 12);
    }

    /**
     * Console ERROR 级别颜色 - 根据主题自适应
     * 亮色主题：Red-600 (220, 38, 38)
     * 暗色主题：Red-400 (248, 113, 113)
     */
    public static Color getConsoleError() {
        return isDarkTheme() ? new Color(248, 113, 113) : new Color(220, 38, 38);
    }

    /**
     * Console 类名颜色 - 根据主题自适应
     * 亮色主题：Purple-700 (147, 51, 234)
     * 暗色主题：Purple-400 (192, 132, 252)
     */
    public static Color getConsoleClassName() {
        return isDarkTheme() ? new Color(192, 132, 252) : new Color(147, 51, 234);
    }

    /**
     * Console 方法名颜色 - 根据主题自适应
     * 亮色主题：Sky-600 (14, 165, 233)
     * 暗色主题：Sky-400 (56, 189, 248)
     */
    public static Color getConsoleMethodName() {
        return isDarkTheme() ? new Color(56, 189, 248) : new Color(14, 165, 233);
    }

    /**
     * Console 工具栏背景色 - 根据主题自适应
     * 亮色主题：Slate-50 (248, 250, 252)
     * 暗色主题：深灰 (35, 35, 40)
     */
    public static Color getConsoleToolbarBg() {
        return isDarkTheme() ? new Color(35, 35, 40) : new Color(248, 250, 252);
    }

    /**
     * Console 工具栏边框色 - 根据主题自适应
     * 亮色主题：Slate-200 (226, 232, 240)
     * 暗色主题：深灰 (60, 60, 70)
     */
    public static Color getConsoleToolbarBorder() {
        return isDarkTheme() ? new Color(60, 60, 70) : new Color(226, 232, 240);
    }

    /**
     * Console 选中文本背景色 - 根据主题自适应
     * 亮色主题：Blue-200 (191, 219, 254)
     * 暗色主题：深蓝 (60, 90, 120)
     */
    public static Color getConsoleSelectionBg() {
        return isDarkTheme() ? new Color(60, 90, 120) : new Color(191, 219, 254);
    }

    // ==================== 工具方法 ====================

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
