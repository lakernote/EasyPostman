package com.laker.postman.common.constants;

import java.awt.*;

/**
 * 现代化UI配色方案
 * 统一的配色常量，确保整个应用的视觉一致性
 * <p>
 * 设计理念：
 * - 主色调：Indigo/Purple（专业、现代）
 * - 辅助色：Blue（信任、科技）
 * - 强调色：Pink（活力、突出）
 * - 中性色：Slate（优雅、易读）
 *
 */
public final class ModernColors {

    // ==================== 主色系 ====================

    /**
     * 主色 - Indigo-500
     */
    public static final Color PRIMARY = new Color(99, 102, 241);

    /**
     * 主色深色 - Indigo-600
     */
    public static final Color PRIMARY_DARK = new Color(79, 70, 229);

    /**
     * 主色超深 - Indigo-700
     */
    public static final Color PRIMARY_DARKER = new Color(67, 56, 202);

    /**
     * 主色浅色 - Indigo-400
     */
    public static final Color PRIMARY_LIGHT = new Color(129, 140, 248);

    /**
     * 主色超浅 - Indigo-100
     */
    public static final Color PRIMARY_LIGHTER = new Color(224, 231, 255);

    // ==================== 辅助色系 ====================

    /**
     * 辅助色 - Purple-500
     */
    public static final Color SECONDARY = new Color(139, 92, 246);

    /**
     * 辅助色深色 - Purple-600
     */
    public static final Color SECONDARY_DARK = new Color(124, 58, 237);

    /**
     * 辅助色浅色 - Purple-400
     */
    public static final Color SECONDARY_LIGHT = new Color(167, 139, 250);

    /**
     * 辅助色超浅 - Purple-100
     */
    public static final Color SECONDARY_LIGHTER = new Color(243, 232, 255);

    // ==================== 强调色系 ====================

    /**
     * 强调色 - Pink-500
     */
    public static final Color ACCENT = new Color(236, 72, 153);

    /**
     * 强调色深色 - Pink-600
     */
    public static final Color ACCENT_DARK = new Color(219, 39, 119);

    /**
     * 强调色浅色 - Pink-400
     */
    public static final Color ACCENT_LIGHT = new Color(244, 114, 182);

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

    // ==================== 中性色系 - 文字 ====================

    /**
     * 文字主色 - Slate-900
     */
    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);

    /**
     * 文字次要色 - Slate-700
     */
    public static final Color TEXT_SECONDARY = new Color(51, 65, 85);

    /**
     * 文字提示色 - Slate-500
     */
    public static final Color TEXT_HINT = new Color(100, 116, 139);

    /**
     * 文字禁用色 - Slate-400
     */
    public static final Color TEXT_DISABLED = new Color(148, 163, 184);

    /**
     * 文字反色（用于深色背景）
     */
    public static final Color TEXT_INVERSE = new Color(248, 250, 252);

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
     * 边框深色 - Slate-400
     */
    public static final Color BORDER_DARK = new Color(148, 163, 184);

    // ==================== 阴影色 ====================

    /**
     * 阴影浅色
     */
    public static final Color SHADOW_LIGHT = new Color(15, 23, 42, 8);

    /**
     * 阴影中色
     */
    public static final Color SHADOW_MEDIUM = new Color(15, 23, 42, 15);

    /**
     * 阴影深色
     */
    public static final Color SHADOW_DARK = new Color(15, 23, 42, 25);

    // ==================== 特殊效果色 ====================

    /**
     * 悬停背景
     */
    public static final Color HOVER_BG = new Color(241, 245, 249);

    /**
     * 选中背景
     */
    public static final Color SELECTED_BG = new Color(224, 231, 255);

    /**
     * 焦点边框
     */
    public static final Color FOCUS_BORDER = PRIMARY;

    /**
     * 分隔线
     */
    public static final Color DIVIDER = new Color(226, 232, 240);

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

    // 私有构造函数，防止实例化
    private ModernColors() {
        throw new AssertionError("Cannot instantiate ModernColors class");
    }
}

