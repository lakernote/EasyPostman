package com.laker.postman.common.component.table;

import com.laker.postman.common.constants.EasyPostManColors;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * 表格UI常量类
 * 集中管理表格相关的UI常量，便于统一修改和维护
 */
public class TableUIConstants {
    // 文本常量
    public static final String SELECT_FILE_TEXT = "Select File";
    public static final String FILE_TYPE = "File";

    // 颜色常量 - 基础色
    public static final Color PRIMARY_COLOR = new Color(66, 133, 244);  // 主题色

    // 背景色
    public static final Color ZEBRA_LIGHT = new Color(250, 252, 255);
    public static final Color ZEBRA_DARK = Color.WHITE;

    // 文本颜色
    public static final Color TEXT_SECONDARY = new Color(95, 99, 104);   // 次要文本
    public static final Color TEXT_DISABLED = new Color(155, 155, 155);  // 禁用文本

    // 边框和交互颜色
    public static final Color BORDER_COLOR = new Color(220, 225, 230);
    public static final Color HOVER_COLOR = new Color(230, 240, 255);

    // 特定用途颜色
    public static final Color FILE_BUTTON_TEXT_COLOR = PRIMARY_COLOR;
    public static final Color FILE_SELECTED_TEXT_COLOR = new Color(76, 130, 206);
    public static final Color FILE_EMPTY_TEXT_COLOR = TEXT_SECONDARY;

    // 图标大小
    public static final int ICON_SIZE = 14;

    // 边距
    public static final int PADDING_LEFT = 8;
    public static final int PADDING_RIGHT = 8;
    public static final int PADDING_TOP = 2;
    public static final int PADDING_BOTTOM = 2;

    /**
     * 创建标准按钮边框
     */
    public static Border createButtonBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
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
    public static Color getCellBackground(boolean isSelected, boolean isHovered, boolean isEmpty, int row,
                                          JTable table) {
        if (isSelected) {
            return table.getSelectionBackground();
        } else if (isHovered) {
            return HOVER_COLOR;
        } else if (isEmpty) {
            return EasyPostManColors.EMPTY_CELL;
        } else {
            return table.getBackground();
        }
    }
}
