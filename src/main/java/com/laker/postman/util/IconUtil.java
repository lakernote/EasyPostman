package com.laker.postman.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;

/**
 * 图标工具类
 * 统一管理项目中所有 SVG 图标的创建和颜色配置
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 创建主题适配的图标（推荐，适用于所有场景）
 * FlatSVGIcon icon = IconUtil.createThemed("icons/save.svg", 20, 20);
 *
 * // 创建使用主色调的图标（用于选中/激活状态）
 * FlatSVGIcon icon = IconUtil.createPrimary("icons/check.svg", 16, 16);
 *
 * // 创建自定义颜色的图标
 * FlatSVGIcon icon = IconUtil.createColored("icons/error.svg", 16, 16, Color.RED);
 *
 * // 使用尺寸常量
 * FlatSVGIcon icon = IconUtil.createThemed("icons/save.svg", SIZE_MEDIUM, SIZE_MEDIUM);
 * }</pre>
 */
@UtilityClass
public class IconUtil {

    // ==================== 核心创建方法 ====================

    /**
     * 创建基本的 SVG 图标（不设置颜色过滤器）
     *
     * @param iconPath 图标路径（如 "icons/save.svg"）
     * @param width    图标宽度
     * @param height   图标高度
     * @return SVG 图标
     */
    public static FlatSVGIcon create(String iconPath, int width, int height) {
        return new FlatSVGIcon(iconPath, width, height);
    }

    /**
     * 创建主题适配的图标（推荐使用）
     * <p>图标颜色会自动跟随当前主题的文本颜色：
     * <ul>
     *   <li>亮色主题：深色图标</li>
     *   <li>暗色主题：浅色图标</li>
     * </ul>
     * <p>适用于按钮、标签、面板等所有常规场景
     *
     * @param iconPath 图标路径（如 "icons/save.svg"）
     * @param width    图标宽度
     * @param height   图标高度
     * @return 配置了颜色过滤器的 SVG 图标
     */
    public static FlatSVGIcon createThemed(String iconPath, int width, int height) {
        return new FlatSVGIcon(iconPath, width, height)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color ->
                        UIManager.getColor("Label.foreground")));
    }

    /**
     * 创建使用主色调的图标
     * <p>适用于需要突出显示的图标，例如：
     * <ul>
     *   <li>选中状态的图标</li>
     *   <li>激活状态的标签页图标</li>
     *   <li>需要强调的功能图标</li>
     * </ul>
     *
     * @param iconPath 图标路径（如 "icons/check.svg"）
     * @param width    图标宽度
     * @param height   图标高度
     * @return 配置了颜色过滤器的 SVG 图标
     */
    public static FlatSVGIcon createPrimary(String iconPath, int width, int height) {
        return new FlatSVGIcon(iconPath, width, height)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.PRIMARY));
    }

    /**
     * 创建使用自定义颜色的图标
     * <p>用于需要特定颜色的场景，例如：
     * <ul>
     *   <li>成功图标（绿色）</li>
     *   <li>警告图标（橙色）</li>
     *   <li>错误图标（红色）</li>
     * </ul>
     *
     * @param iconPath    图标路径（如 "icons/error.svg"）
     * @param width       图标宽度
     * @param height      图标高度
     * @param customColor 自定义颜色
     * @return 配置了颜色过滤器的 SVG 图标
     */
    public static FlatSVGIcon createColored(String iconPath, int width, int height, Color customColor) {
        return new FlatSVGIcon(iconPath, width, height)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> customColor));
    }

    // ==================== 常用图标尺寸常量 ====================

    /**
     * 小图标尺寸（16x16）
     * <p>适用于：列表项图标、工具栏小图标、眼睛图标等
     */
    public static final int SIZE_SMALL = 16;

    /**
     * 中等图标尺寸（20x20）
     * <p>适用于：按钮图标、保存/导出按钮等
     */
    public static final int SIZE_MEDIUM = 20;

    /**
     * 大图标尺寸（24x24）
     * <p>适用于：协议图标（HTTP/WebSocket/SSE）、大按钮图标等
     */
    public static final int SIZE_LARGE = 24;

    /**
     * 标签栏图标尺寸（22x22）
     * <p>适用于：侧边栏标签页图标
     */
    public static final int SIZE_TAB = 22;
}

