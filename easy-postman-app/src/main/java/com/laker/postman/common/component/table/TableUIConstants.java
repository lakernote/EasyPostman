package com.laker.postman.common.component.table;

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

    // 图标大小
    public static final int ICON_SIZE = 14;

    // 边距
    public static final int PADDING_LEFT = 8;
    public static final int PADDING_RIGHT = 8;
    public static final int PADDING_TOP = 2;
    public static final int PADDING_BOTTOM = 2;

    /**
     * 获取边框颜色 - 主题适配
     */
    public static Color getBorderColor() {
        return ModernColors.getBorderLightColor();
    }

    /**
     * 获取悬停颜色 - 主题适配
     */
    public static Color getHoverColor() {
        return ModernColors.getHoverBackgroundColor();
    }

    /**
     * 获取文件按钮文字颜色 - 主题适配
     */
    public static Color getFileButtonTextColor() {
        // 主题色在两种模式下都保持一致
        return ModernColors.getPrimary();
    }

    /**
     * 获取文件选中文字颜色 - 主题适配
     */
    public static Color getFileSelectedTextColor() {
        return ModernColors.getPrimaryLight();
    }

    /**
     * 获取文件空状态文字颜色 - 主题适配
     */
    public static Color getFileEmptyTextColor() {
        return ModernColors.getTextHint();
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
