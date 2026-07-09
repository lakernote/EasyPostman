package com.laker.postman.common.constants;

import com.formdev.flatlaf.FlatLaf;
import lombok.experimental.UtilityClass;

import javax.swing.UIManager;
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
 * 面板背景色（暗色主题 - 外层边界）38, 40, 44
 * @panelBg=#26282c
 * 面板背景色（亮色主题 - 主背景色浅灰色）
 * @panelBg=#e9eaee
 *
 */
@UtilityClass
public final class ModernColors {

    // ==================== 主题检测 ====================

    /**
     * 检查当前是否为暗色主题
     * @return true 如果当前是暗色主题，false 如果是亮色主题
     */
    public static boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    private static Color color(String key, Color lightFallback, Color darkFallback) {
        return ThemeColors.color(key, isDarkTheme() ? darkFallback : lightFallback);
    }

    private static Color color(String key, Color fallback) {
        return ThemeColors.color(key, fallback);
    }

    private static Color uiColor(String key, Color lightFallback, Color darkFallback) {
        Color color = UIManager.getColor(key);
        if (color != null) {
            return ThemeColors.explicit(color);
        }
        return ThemeColors.explicit(isDarkTheme() ? darkFallback : lightFallback);
    }

    private static Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return ThemeColors.explicit(color != null ? color : fallback);
    }

    // ==================== 主色系 ====================

    /**
     * 主色 - IDEA-inspired Blue
     */
    public static final Color PRIMARY = new Color(55, 113, 225);

    /**
     * 主色深色 - IDEA-inspired Blue-600
     */
    public static final Color PRIMARY_DARK = new Color(47, 98, 201);

    /**
     * 主色超深 - IDEA-inspired Blue-700
     */
    public static final Color PRIMARY_DARKER = new Color(40, 84, 173);

    /**
     * 主色浅色 - IDEA-inspired Blue-400
     */
    public static final Color PRIMARY_LIGHT = new Color(94, 143, 240);

    /**
     * 主色超浅 - IDEA-inspired Blue-100
     */
    public static final Color PRIMARY_LIGHTER = new Color(212, 227, 255);

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
     * 成功 - IDEA-like muted green.
     */
    public static final Color SUCCESS = new Color(87, 150, 92);

    /**
     * 成功深色 - IDEA-like muted green.
     */
    public static final Color SUCCESS_DARK = new Color(78, 143, 85);

    /**
     * 错误 - IDEA-like muted red.
     */
    public static final Color ERROR = new Color(219, 88, 96);

    /**
     * 错误深色 - IDEA-like muted red.
     */
    public static final Color ERROR_DARK = new Color(199, 84, 80);

    /**
     * 错误超深 - IDEA-like muted red.
     */
    public static final Color ERROR_DARKER = new Color(166, 69, 69);

    /**
     * 警告 - IDEA-like muted amber.
     */
    public static final Color WARNING = new Color(194, 158, 73);

    /**
     * 警告深色 - IDEA-like muted amber.
     */
    public static final Color WARNING_DARK = new Color(168, 135, 63);

    /**
     * 警告超深 - IDEA-like muted amber.
     */
    public static final Color WARNING_DARKER = new Color(138, 109, 50);

    /**
     * 信息 - IDEA-like cyan.
     */
    public static final Color INFO = new Color(79, 174, 208);

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
    public static final Color GIT_COMMIT = SUCCESS;

    /**
     * Git Push（推送）- 优雅深蓝色
     */
    public static final Color GIT_PUSH = new Color(59, 130, 246);  // Blue-500

    /**
     * Git Pull（拉取）- 清新紫色
     */
    public static final Color GIT_PULL = new Color(168, 85, 247);  // Purple-500

    // ==================== 变量类型颜色 ====================

    public static final Color VARIABLE_CONTEXT = WARNING;
    public static final Color VARIABLE_ITERATION_DATA = new Color(25, 118, 210);
    public static final Color VARIABLE_GROUP = new Color(77, 182, 172);
    public static final Color VARIABLE_ENVIRONMENT = SUCCESS;
    public static final Color VARIABLE_GLOBAL = new Color(67, 56, 202);
    public static final Color VARIABLE_BUILT_IN = new Color(156, 39, 176);

    public static Color getPrimary() {
        return color(ThemeColors.PRIMARY, PRIMARY);
    }

    public static Color getPrimaryDark() {
        return color(ThemeColors.PRIMARY_DARK, PRIMARY_DARK);
    }

    public static Color getPrimaryDarker() {
        return color(ThemeColors.PRIMARY_DARKER, PRIMARY_DARKER);
    }

    public static Color getPrimaryLight() {
        return color(ThemeColors.PRIMARY_LIGHT, PRIMARY_LIGHT);
    }

    public static Color getPrimaryLighter() {
        return color(ThemeColors.PRIMARY_LIGHTER, PRIMARY_LIGHTER);
    }

    public static Color getSecondary() {
        return color(ThemeColors.SECONDARY, SECONDARY);
    }

    public static Color getSecondaryDark() {
        return color(ThemeColors.SECONDARY_DARK, SECONDARY_DARK);
    }

    public static Color getSecondaryLight() {
        return color(ThemeColors.SECONDARY_LIGHT, SECONDARY_LIGHT);
    }

    public static Color getSecondaryLighter() {
        return color(ThemeColors.SECONDARY_LIGHTER, SECONDARY_LIGHTER);
    }

    public static Color getAccent() {
        return color(ThemeColors.ACCENT, ACCENT);
    }

    public static Color getAccentLight() {
        return color(ThemeColors.ACCENT_LIGHT, ACCENT_LIGHT);
    }

    public static Color getSuccess() {
        return color(ThemeColors.SUCCESS, SUCCESS);
    }

    public static Color getSuccessDark() {
        return color(ThemeColors.SUCCESS_DARK, SUCCESS_DARK);
    }

    public static Color getError() {
        return color(ThemeColors.ERROR, ERROR);
    }

    public static Color getErrorDark() {
        return color(ThemeColors.ERROR_DARK, ERROR_DARK);
    }

    public static Color getErrorDarker() {
        return color(ThemeColors.ERROR_DARKER, ERROR_DARKER);
    }

    public static Color getWarning() {
        return color(ThemeColors.WARNING, WARNING);
    }

    public static Color getWarningDark() {
        return color(ThemeColors.WARNING_DARK, WARNING_DARK);
    }

    public static Color getWarningDarker() {
        return color(ThemeColors.WARNING_DARKER, WARNING_DARKER);
    }

    public static Color getInfo() {
        return color(ThemeColors.INFO, INFO);
    }

    public static Color getNeutral() {
        return color(ThemeColors.NEUTRAL, NEUTRAL);
    }

    public static Color getNeutralDark() {
        return color(ThemeColors.NEUTRAL_DARK, NEUTRAL_DARK);
    }

    public static Color getNeutralDarker() {
        return color(ThemeColors.NEUTRAL_DARKER, NEUTRAL_DARKER);
    }

    public static Color getGitCommit() {
        return color(ThemeColors.GIT_COMMIT, GIT_COMMIT);
    }

    public static Color getGitPush() {
        return color(ThemeColors.GIT_PUSH, GIT_PUSH);
    }

    public static Color getGitPull() {
        return color(ThemeColors.GIT_PULL, GIT_PULL);
    }

    public static Color getVariableContextColor() {
        return color(ThemeColors.VARIABLE_CONTEXT, VARIABLE_CONTEXT);
    }

    public static Color getVariableIterationDataColor() {
        return color(ThemeColors.VARIABLE_ITERATION_DATA, VARIABLE_ITERATION_DATA);
    }

    public static Color getVariableGroupColor() {
        return color(ThemeColors.VARIABLE_GROUP, VARIABLE_GROUP);
    }

    public static Color getVariableEnvironmentColor() {
        return color(ThemeColors.VARIABLE_ENVIRONMENT, VARIABLE_ENVIRONMENT);
    }

    public static Color getVariableGlobalColor() {
        return color(ThemeColors.VARIABLE_GLOBAL, VARIABLE_GLOBAL);
    }

    public static Color getVariableBuiltInColor() {
        return color(ThemeColors.VARIABLE_BUILT_IN, VARIABLE_BUILT_IN);
    }

    // ==================== HTTP 方法/协议颜色 ====================

    public static Color getHttpMethodGet() {
        return color(ThemeColors.HTTP_METHOD_GET, new Color(0x2E7D32), new Color(0x6AAB73));
    }

    public static Color getHttpMethodPost() {
        return color(ThemeColors.HTTP_METHOD_POST, new Color(0xA16207), new Color(0xD5A945));
    }

    public static Color getHttpMethodPut() {
        return color(ThemeColors.HTTP_METHOD_PUT, new Color(0x1565C0), new Color(0x589DF6));
    }

    public static Color getHttpMethodPatch() {
        return color(ThemeColors.HTTP_METHOD_PATCH, new Color(0x00838F), new Color(0x2AACB8));
    }

    public static Color getHttpMethodDelete() {
        return color(ThemeColors.HTTP_METHOD_DELETE, new Color(0xC62828), new Color(0xF06A6A));
    }

    public static Color getHttpMethodDefault() {
        return color(ThemeColors.HTTP_METHOD_DEFAULT, new Color(0x475569), new Color(0x9AA0AA));
    }

    public static Color getHttpProtocolWs() {
        return color(ThemeColors.HTTP_PROTOCOL_WS, new Color(0x00838F), new Color(0x52C7D9));
    }

    public static Color getHttpProtocolSse() {
        return color(ThemeColors.HTTP_PROTOCOL_SSE, new Color(0x00796B), new Color(0x4DB6AC));
    }

    // ==================== 变量徽标颜色 ====================

    /**
     * 已定义变量的徽标背景色。
     * 保持与 EasyTextField / RequestBodyPanel 现有视觉一致。
     */
    public static Color getDefinedVariableBadgeBackground() {
        return new Color(180, 210, 255, 120);
    }

    /**
     * 已定义变量的徽标边框色。
     */
    public static Color getDefinedVariableBadgeBorder() {
        return new Color(80, 150, 255);
    }

    /**
     * 未定义变量的徽标背景色。
     */
    public static Color getUndefinedVariableBadgeBackground() {
        return new Color(255, 200, 200, 120);
    }

    /**
     * 未定义变量的徽标边框色。
     */
    public static Color getUndefinedVariableBadgeBorder() {
        return new Color(255, 100, 100);
    }

    // ==================== 文字颜色（主题适配）====================

    /**
     * 文字主色 - 根据主题自适应
     * 亮色主题：Slate-900 (深色文字)
     * 暗色主题：Slate-100 (浅色文字)
     */
    public static Color getTextPrimary() {
        return color(ThemeColors.TEXT_PRIMARY, new Color(15, 23, 42), new Color(201, 204, 211));
    }

    /**
     * 文字次要色 - 根据主题自适应
     * 亮色主题：Slate-700
     * 暗色主题：Slate-300
     */
    public static Color getTextSecondary() {
        return color(ThemeColors.TEXT_SECONDARY, new Color(51, 65, 85), new Color(185, 189, 197));
    }

    /**
     * 文字提示色 - 根据主题自适应
     * 亮色主题：Slate-500
     * 暗色主题：Slate-400
     */
    public static Color getTextHint() {
        return color(ThemeColors.TEXT_HINT, new Color(100, 116, 139), new Color(123, 128, 137));
    }

    /**
     * 文字禁用色 - 根据主题自适应
     * 亮色主题：Slate-400
     * 暗色主题：Slate-600
     */
    public static Color getTextDisabled() {
        return color(ThemeColors.TEXT_DISABLED, new Color(148, 163, 184), new Color(86, 91, 100));
    }

    /**
     * 文字反色（用于深色背景）- 主题自适应
     * 两种主题都返回浅色文字
     */
    public static Color getTextInverse() {
        return color(ThemeColors.TEXT_INVERSE, new Color(248, 250, 252));
    }

    // ==================== 背景颜色（主题适配）====================

    /**
     * 获取主背景色 - 根据主题自适应
     * 亮色主题：IDEA-like workspace gray (233, 234, 238)
     * 暗色主题：IDEA-like workspace chrome (38, 40, 44)
     */
    public static Color getBackgroundColor() {
        return color(ThemeColors.BACKGROUND, new Color(233, 234, 238), new Color(38, 40, 44));
    }

    /**
     * 获取卡片/区域背景色 - 根据主题自适应
     * 亮色主题：白色 (255, 255, 255)
     * 暗色主题：IDEA-like inner surface (30, 31, 34)
     */
    public static Color getCardBackgroundColor() {
        return color(ThemeColors.SURFACE, new Color(255, 255, 255), new Color(30, 31, 34));
    }

    /**
     * 获取窗口顶部 chrome 背景色 - 菜单栏和主窗口 TitlePane 共用。
     */
    public static Color getWindowChromeBackgroundColor() {
        return color(ThemeColors.WINDOW_CHROME_BACKGROUND, new Color(233, 234, 238), new Color(38, 40, 44));
    }

    /**
     * 获取 Dialog chrome 背景色 - 仅用于 JDialog 标题栏、背景和 footer。
     */
    public static Color getDialogChromeBackgroundColor() {
        return color(ThemeColors.DIALOG_CHROME_BACKGROUND, new Color(247, 248, 249), new Color(30, 31, 34));
    }

    /**
     * 获取输入框背景色 - 根据主题自适应
     * 亮色主题：白色 (255, 255, 255)
     * 暗色主题：外层灰上的输入区 (43, 45, 48)
     */
    public static Color getInputBackgroundColor() {
        return color(ThemeColors.INPUT_BACKGROUND, new Color(255, 255, 255), new Color(43, 45, 48));
    }

    /**
     * 获取输入框禁用状态背景色。
     * 优先跟随 FlatLaf 文本框自身 token，避免复用按钮禁用色导致表单块过重。
     */
    public static Color getInputDisabledBackgroundColor() {
        return uiColor("TextField.disabledBackground", new Color(241, 243, 246), new Color(40, 42, 46));
    }

    /**
     * 获取 Tab 区域未选中背景色 - 根据主题自适应。
     */
    public static Color getTabBackgroundColor() {
        return color(ThemeColors.TAB_BACKGROUND, new Color(255, 255, 255), new Color(30, 31, 34));
    }

    /**
     * 获取 Tab 选中背景色 - 根据主题自适应。
     */
    public static Color getTabSelectedBackgroundColor() {
        return color(ThemeColors.TAB_SELECTED_BACKGROUND, new Color(226, 235, 254), new Color(43, 45, 48));
    }

    /**
     * 获取 Tab 悬停背景色 - 根据主题自适应。
     */
    public static Color getTabHoverBackgroundColor() {
        return color(ThemeColors.TAB_HOVER_BACKGROUND, new Color(242, 246, 255), new Color(38, 40, 44));
    }

    /**
     * 获取 Tab 区域分割线颜色 - 根据主题自适应。
     */
    public static Color getTabSeparatorColor() {
        return color(ThemeColors.TAB_SEPARATOR, new Color(233, 234, 238), new Color(43, 45, 48));
    }

    /**
     * 获取悬停背景色 - 根据主题自适应
     * 亮色主题：浅蓝灰 hover
     * 暗色主题：比卡片稍亮
     */
    public static Color getHoverBackgroundColor() {
        return color(ThemeColors.HOVER_BACKGROUND, new Color(239, 244, 255), new Color(43, 45, 48));
    }

    /**
     * 获取通用选中背景色 - 根据主题自适应
     */
    public static Color getSelectionBackgroundColor() {
        return color(ThemeColors.SELECTION_BACKGROUND, new Color(212, 227, 255), new Color(43, 67, 113));
    }

    /**
     * 获取表格主体背景色。
     * 优先读取 FlatLaf 标准 Table token，便于不同主题保持一致。
     */
    public static Color getTableBackgroundColor() {
        return uiColor("Table.background", new Color(255, 255, 255), new Color(30, 31, 34));
    }

    /**
     * 获取表头背景色，接近 IntelliJ IDEA 的轻量灰色表头。
     */
    public static Color getTableHeaderBackgroundColor() {
        return uiColor("TableHeader.background", new Color(244, 246, 248), new Color(43, 45, 48));
    }

    /**
     * 获取表格网格/分隔线颜色。
     */
    public static Color getTableGridColor() {
        return uiColor("Table.gridColor", new Color(232, 235, 239), new Color(43, 45, 48));
    }

    /**
     * 获取表格选中背景色。
     */
    public static Color getTableSelectionBackgroundColor() {
        return uiColor("Table.selectionBackground", getSelectionBackgroundColor());
    }

    /**
     * 获取表格选中文字色。
     */
    public static Color getTableSelectionForegroundColor() {
        return uiColor("Table.selectionForeground", getTextPrimary());
    }


    /**
     * 获取按钮按下状态背景 - 根据主题自适应
     * 亮色主题：Slate-200 (226, 232, 240)
     * 暗色主题：比背景更暗 (34, 36, 39)
     */
    public static Color getButtonPressedColor() {
        return color(ThemeColors.BUTTON_PRESSED_BACKGROUND, new Color(220, 233, 255), new Color(36, 38, 42));
    }

    /**
     * 获取按钮禁用状态背景 - 根据主题自适应
     * 亮色主题：Slate-200 (226, 232, 240)
     * 暗色主题：深灰色 (60, 60, 65)
     */
    public static Color getButtonDisabledBackgroundColor() {
        return color(ThemeColors.BUTTON_DISABLED_BACKGROUND, new Color(226, 232, 240), new Color(40, 42, 46));
    }

    // ==================== 边框颜色（主题适配）====================

    /**
     * 获取边框颜色（浅色）- 根据主题自适应
     * 亮色主题：Slate-200 (226, 232, 240)
     * 暗色主题：比背景稍亮 (59, 61, 66) - 对应 @borderColor
     */
    public static Color getBorderLightColor() {
        return color(ThemeColors.BORDER_LIGHT, new Color(211, 218, 230), new Color(50, 52, 56));
    }

    /**
     * 获取边框颜色（中等）- 根据主题自适应
     * 亮色主题：Slate-300 (203, 213, 225)
     * 暗色主题：更明显的边框 (70, 73, 80)
     */
    public static Color getBorderMediumColor() {
        return color(ThemeColors.BORDER_MEDIUM, new Color(190, 199, 213), new Color(58, 61, 66));
    }

    /**
     * 获取分隔线/边框颜色 - 主题适配
     * 用于面板之间的分隔线、边框等
     * 亮色主题：浅灰色 (213, 216, 222)
     * 暗色主题：比背景亮的灰色 (49, 51, 56)
     */
    public static Color getDividerBorderColor() {
        return color(ThemeColors.DIVIDER, new Color(213, 216, 222), new Color(43, 45, 48));
    }

    /**
     * 获取空单元格背景色 - 主题适配
     * 亮色主题：白色（清晰区分）
     * 暗色主题：比有值单元格稍亮的颜色（便于区分空单元格）
     */
    public static Color getEmptyCellBackground() {
        return color(ThemeColors.EMPTY_CELL_BACKGROUND, Color.WHITE, new Color(40, 42, 46));
    }

    // ==================== 滚动条颜色（主题适配）====================

    /**
     * 获取滚动条轨道颜色 - 根据主题自适应
     * 亮色主题：Slate-50 (245, 247, 250)
     * 暗色主题：与内容面相同 (30, 31, 34)
     */
    public static Color getScrollbarTrackColor() {
        return color(ThemeColors.SCROLLBAR_TRACK, new Color(243, 244, 247), new Color(30, 31, 34));
    }

    /**
     * 获取滚动条滑块颜色 - 根据主题自适应
     * 亮色主题：浅灰 (200, 204, 212)
     * 暗色主题：比背景亮的灰色 (70, 72, 78)
     */
    public static Color getScrollbarThumbColor() {
        return color(ThemeColors.SCROLLBAR_THUMB, new Color(200, 204, 212), new Color(63, 66, 72));
    }

    /**
     * 获取滚动条滑块悬停颜色 - 根据主题自适应
     * 亮色主题：中灰 (183, 189, 200)
     * 暗色主题：更亮的灰色 (85, 88, 96)
     */
    public static Color getScrollbarThumbHoverColor() {
        return color(ThemeColors.SCROLLBAR_THUMB_HOVER, new Color(183, 189, 200), new Color(76, 80, 87));
    }

    // ==================== 警告/提示颜色（主题适配）====================

    /**
     * 获取警告背景色 - 根据主题自适应
     * 亮色主题：浅黄色 (255, 243, 205)
     * 暗色主题：暗黄色调 (70, 65, 50)
     */
    public static Color getWarningBackgroundColor() {
        return color(ThemeColors.WARNING_BACKGROUND, new Color(255, 243, 205), new Color(58, 52, 39));
    }

    /**
     * 获取警告边框颜色 - 根据主题自适应
     * 亮色主题：黄色 (255, 193, 7)
     * 暗色主题：较亮的黄色 (120, 100, 60)
     */
    public static Color getWarningBorderColor() {
        return color(ThemeColors.WARNING_BORDER, new Color(255, 193, 7), new Color(98, 84, 50));
    }

    /**
     * 获取搜索普通匹配高亮背景色。
     */
    public static Color getSearchHighlightBackgroundColor() {
        return color(ThemeColors.SEARCH_HIGHLIGHT_BACKGROUND, new Color(255, 241, 168), new Color(91, 75, 0));
    }

    /**
     * 获取搜索当前匹配高亮背景色。
     */
    public static Color getSearchCurrentHighlightBackgroundColor() {
        return color(ThemeColors.SEARCH_CURRENT_HIGHLIGHT_BACKGROUND, new Color(254, 215, 170), new Color(150, 100, 50));
    }

    public static Color getSplashGradientStartColor() {
        return color(ThemeColors.SPLASH_GRADIENT_START, getPrimary(), new Color(65, 67, 69));
    }

    public static Color getSplashGradientEndColor() {
        return color(ThemeColors.SPLASH_GRADIENT_END, getPrimaryLighter(), new Color(30, 31, 34));
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
     * 亮色主题：浅灰色 (243, 244, 247) - #f3f4f7
     * 暗色主题：内容面深灰 (30, 31, 34) - #1e1f22
     */
    public static Color getConsoleTextAreaBg() {
        return color(ThemeColors.CONSOLE_TEXT_AREA_BACKGROUND, new Color(243, 244, 247), new Color(30, 31, 34));
    }

    /**
     * Console 普通文本颜色 - 根据主题自适应
     * 亮色主题：Slate-700 (51, 65, 85)
     * 暗色主题：浅灰 (220, 220, 220)
     */
    public static Color getConsoleText() {
        return color(ThemeColors.CONSOLE_TEXT, new Color(51, 65, 85), new Color(201, 204, 211));
    }

    /**
     * Console INFO 级别颜色 - 根据主题自适应
     * 亮色主题：Blue-700 (37, 99, 235)
     * 暗色主题：Blue-400 (96, 165, 250)
     */
    public static Color getConsoleInfo() {
        return color(ThemeColors.CONSOLE_INFO, new Color(37, 99, 235), new Color(96, 165, 250));
    }

    /**
     * Console DEBUG 级别颜色 - 根据主题自适应
     * 亮色主题：Green-600 (22, 163, 74)
     * 暗色主题：Green-400 (74, 222, 128)
     */
    public static Color getConsoleDebug() {
        return color(ThemeColors.CONSOLE_DEBUG, new Color(22, 163, 74), SUCCESS);
    }

    /**
     * Console WARN 级别颜色 - 根据主题自适应
     * 亮色主题：Orange-600 (234, 88, 12)
     * 暗色主题：Orange-400 (251, 146, 60)
     */
    public static Color getConsoleWarn() {
        return color(ThemeColors.CONSOLE_WARN, new Color(234, 88, 12), WARNING);
    }

    /**
     * Console ERROR 级别颜色 - 根据主题自适应
     * 亮色主题：Red-600 (220, 38, 38)
     * 暗色主题：Red-400 (248, 113, 113)
     */
    public static Color getConsoleError() {
        return color(ThemeColors.CONSOLE_ERROR, new Color(220, 38, 38), ERROR);
    }

    /**
     * Console 类名颜色 - 根据主题自适应
     * 亮色主题：Purple-700 (147, 51, 234)
     * 暗色主题：Purple-400 (192, 132, 252)
     */
    public static Color getConsoleClassName() {
        return color(ThemeColors.CONSOLE_CLASS_NAME, new Color(147, 51, 234), new Color(192, 132, 252));
    }

    /**
     * Console 方法名颜色 - 根据主题自适应
     * 亮色主题：Sky-600 (14, 165, 233)
     * 暗色主题：Sky-400 (56, 189, 248)
     */
    public static Color getConsoleMethodName() {
        return color(ThemeColors.CONSOLE_METHOD_NAME, new Color(14, 165, 233), new Color(56, 189, 248));
    }

    /**
     * Console 工具栏背景色 - 根据主题自适应
     * 亮色主题：Slate-50 (248, 250, 252)
     * 暗色主题：略浅的灰色 (58, 60, 62) - #3a3c3e
     */
    public static Color getConsoleToolbarBg() {
        return color(ThemeColors.CONSOLE_TOOLBAR_BACKGROUND, new Color(248, 250, 252), new Color(38, 40, 44));
    }

    /**
     * Console 工具栏边框色 - 根据主题自适应
     * 亮色主题：Slate-200 (226, 232, 240)
     * 暗色主题：深灰 (60, 60, 70)
     */
    public static Color getConsoleToolbarBorder() {
        return color(ThemeColors.CONSOLE_TOOLBAR_BORDER, new Color(226, 232, 240), new Color(50, 52, 56));
    }

    /**
     * Console 选中文本背景色 - 根据主题自适应
     * 亮色主题：Blue-200 (191, 219, 254)
     * 暗色主题：深蓝 (60, 90, 120)
     */
    public static Color getConsoleSelectionBg() {
        return color(ThemeColors.CONSOLE_SELECTION_BACKGROUND, new Color(191, 219, 254), new Color(43, 67, 113));
    }

    public static Color getNotificationBackground() {
        return color(ThemeColors.NOTIFICATION_BACKGROUND, new Color(252, 252, 253), new Color(38, 40, 44));
    }

    public static Color getNotificationBorder() {
        return color(ThemeColors.NOTIFICATION_BORDER, new Color(205, 208, 214), new Color(50, 52, 56));
    }

    public static Color getNotificationDivider() {
        return color(ThemeColors.NOTIFICATION_DIVIDER, new Color(208, 211, 216), new Color(43, 45, 48));
    }

    public static Color getNotificationBodyForeground() {
        return color(ThemeColors.NOTIFICATION_BODY_FOREGROUND, new Color(44, 46, 50), new Color(201, 204, 211));
    }

    // ==================== 工具方法 ====================

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static String toHtmlColor(Color color) {
        if (color == null) {
            return "#000000";
        }
        return "#%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * 获取带透明度的警告色
     *
     * @param alpha 透明度 (0-255)
     */
    public static Color warningWithAlpha(int alpha) {
        return withAlpha(getWarning(), alpha);
    }

    /**
     * 获取带透明度的主色
     *
     * @param alpha 透明度 (0-255)
     */
    public static Color primaryWithAlpha(int alpha) {
        return withAlpha(getPrimary(), alpha);
    }

    /**
     * 获取带透明度的辅助色
     *
     * @param alpha 透明度 (0-255)
     */
    public static Color secondaryWithAlpha(int alpha) {
        return withAlpha(getSecondary(), alpha);
    }

    /**
     * 获取带透明度的强调色
     *
     * @param alpha 透明度 (0-255)
     */
    public static Color accentWithAlpha(int alpha) {
        return withAlpha(getAccent(), alpha);
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
        return new GradientPaint(0, 0, getPrimary(), width, height, getPrimaryDark());
    }

    /**
     * 创建辅助色渐变
     */
    public static GradientPaint createSecondaryGradient(int width, int height) {
        return new GradientPaint(0, 0, getSecondaryLight(), width, height, getSecondaryDark());
    }

    /**
     * 创建主色到辅助色渐变
     */
    public static GradientPaint createPrimaryToSecondaryGradient(int width, int height) {
        return new GradientPaint(0, 0, getPrimary(), width, height, getSecondary());
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
