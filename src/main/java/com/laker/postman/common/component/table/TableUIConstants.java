package com.laker.postman.common.component.table;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * 表格UI常量类
 * 集中管理表格相关的UI常量，便于统一修改和维护
 * 支持亮色和暗色主题自适应
 */
public class TableUIConstants {
    // 文本常量
    public static final String SELECT_FILE_TEXT = "Select File";
    public static final String FILE_TYPE = "File";

    // 颜色常量 - 基础色（保留向后兼容）
    public static final Color PRIMARY_COLOR = new Color(66, 133, 244);  // 主题色

    // 图标大小
    public static final int ICON_SIZE = 14;

    // 边距
    public static final int PADDING_LEFT = 8;
    public static final int PADDING_RIGHT = 8;
    public static final int PADDING_TOP = 2;
    public static final int PADDING_BOTTOM = 2;

    /**
     * 检查当前是否为暗色主题
     */
    private static boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取边框颜色 - 主题适配
     */
    public static Color getBorderColor() {
        return isDarkTheme() ? new Color(80, 80, 85) : new Color(220, 225, 230);
    }

    /**
     * 获取悬停颜色 - 主题适配
     */
    public static Color getHoverColor() {
        return isDarkTheme() ? new Color(60, 63, 65) : new Color(230, 240, 255);
    }

    /**
     * 获取文件按钮文字颜色 - 主题适配
     */
    public static Color getFileButtonTextColor() {
        // 主题色在两种模式下都保持一致
        return PRIMARY_COLOR;
    }

    /**
     * 获取文件选中文字颜色 - 主题适配
     */
    public static Color getFileSelectedTextColor() {
        return isDarkTheme() ? new Color(100, 150, 230) : new Color(76, 130, 206);
    }

    /**
     * 获取文件空状态文字颜色 - 主题适配
     */
    public static Color getFileEmptyTextColor() {
        return isDarkTheme() ? new Color(140, 140, 145) : new Color(95, 99, 104);
    }

    /**
     * 创建标准按钮边框
     */
    public static Border createButtonBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getBorderColor()),
                BorderFactory.createEmptyBorder(
                        PADDING_TOP, PADDING_LEFT, PADDING_BOTTOM, PADDING_RIGHT));
    }

    /**
     * 创建标准标签边框
     */
    public static Border createLabelBorder() {
        return BorderFactory.createEmptyBorder(
                PADDING_TOP, PADDING_LEFT, PADDING_BOTTOM, PADDING_RIGHT);
    }

    /**
     * 获取单元格背景色
     */
    public static Color getCellBackground(boolean isSelected, boolean isHovered, boolean isEmpty,
                                          JTable table) {
        if (isSelected) {
            return table.getSelectionBackground();
        } else if (isHovered) {
            return getHoverColor();
        } else if (isEmpty) {
            return ModernColors.getEmptyCellBackground();
        } else {
            return table.getBackground();
        }
    }
}
