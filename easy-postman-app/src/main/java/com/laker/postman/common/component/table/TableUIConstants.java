package com.laker.postman.common.component.table;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
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
        return ModernColors.getTableGridColor();
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
     * 创建表格编辑态外框。
     * 直接返回编辑器组件时使用，替代 FlatTextField 默认 focus/underline 效果。
     */
    public static Border createCellEditorBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getPrimaryLight()),
                BorderFactory.createEmptyBorder(0, PADDING_LEFT - 1, 0, PADDING_RIGHT - 1)
        );
    }

    /**
     * 创建容器内部输入组件的内边距。
     */
    public static Border createCellEditorInnerBorder() {
        return BorderFactory.createEmptyBorder(0, PADDING_LEFT, 0, PADDING_RIGHT);
    }

    /**
     * 创建 form-data Key/Type 分组表头边框。
     * 该表头是表格结构强调，不应在具体面板里散落 legacy Swing border 组合。
     */
    public static Border createFormDataGroupedHeaderBorder(Color gridColor,
                                                          boolean rightBoundary,
                                                          int leftPadding,
                                                          int rightPadding) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, rightBoundary ? 1 : 0, gridColor),
                BorderFactory.createEmptyBorder(0, leftPadding, 0, rightPadding)
        );
    }

    /**
     * 获取指定行的非选中背景色，保持编辑态和渲染态斑马纹一致。
     */
    public static Color getRowBackground(JTable table, int row) {
        Color base = table.getBackground();
        return row % 2 == 1 ? EasyTextFieldCellRenderer.stripeBackground(base) : base;
    }

    /**
     * 获取指定单元格背景色，包含选中、悬停、空值和斑马纹状态。
     */
    public static Color getCellBackground(boolean isSelected, boolean isHovered, boolean isEmpty,
                                          JTable table, int row) {
        if (isSelected) {
            return table.getSelectionBackground();
        } else if (isHovered) {
            return getHoverColor();
        } else if (isEmpty) {
            return ModernColors.getEmptyCellBackground();
        } else if (row >= 0) {
            return getRowBackground(table, row);
        } else {
            return table.getBackground();
        }
    }

    /**
     * 编辑器直接作为 cell editor 返回时使用。
     */
    public static void styleTextCellEditor(JTextComponent editor, JTable table, int row) {
        styleTextCellEditor(editor, table, row, createCellEditorBorder());
    }

    /**
     * 编辑器放在带外框的容器内时使用。
     */
    public static void styleContainedTextCellEditor(JTextComponent editor, JTable table, int row) {
        styleTextCellEditor(editor, table, row, createCellEditorInnerBorder());
    }

    /**
     * 容器型 cell editor 使用，例如智能多行 editor 和文件 editor。
     */
    public static void styleCellEditorContainer(JComponent container, JTable table, int row) {
        Color background = getRowBackground(table, row);
        container.setOpaque(true);
        container.setBackground(background);
        container.setBorder(createCellEditorBorder());
    }

    /**
     * JScrollPane 放在 cell editor 内部时，需要同步 viewport 背景，否则暗色主题会露白。
     */
    public static void styleEditorScrollPane(JScrollPane scrollPane, JTable table, int row) {
        Color background = getRowBackground(table, row);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(background);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        if (scrollPane.getViewport() != null) {
            scrollPane.getViewport().setOpaque(true);
            scrollPane.getViewport().setBackground(background);
        }
    }

    private static void styleTextCellEditor(JTextComponent editor, JTable table, int row, Border border) {
        Color background = getRowBackground(table, row);
        editor.setOpaque(true);
        editor.setBackground(background);
        editor.setForeground(table.getForeground());
        editor.setCaretColor(table.getForeground());
        editor.setBorder(border);
        editor.putClientProperty("JComponent.outline", null);
        editor.putClientProperty("FlatLaf.style", null);
    }

    /**
     * 获取单元格背景色
     */
    public static Color getCellBackground(boolean isSelected, boolean isHovered, boolean isEmpty,
                                          JTable table) {
        return getCellBackground(isSelected, isHovered, isEmpty, table, -1);
    }
}
