package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class EasyTextFieldCellRenderer extends EasyTextField implements TableCellRenderer {

    public EasyTextFieldCellRenderer() {
        super(1);
        setBorder(null);
        setOpaque(true);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // 主题切换后重新设置边框为null，避免显示边框
        setBorder(null);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        String text = value == null ? "" : value.toString();

        // 动态计算可显示的最大字符数
        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        int maxDisplayLength = calculateMaxDisplayLength(text, columnWidth);

        // 设置显示文本（如果过长则截断）
        if (text != null && text.length() > maxDisplayLength && maxDisplayLength > 0) {
            setText(text.substring(0, maxDisplayLength) + "...");
        } else {
            setText(text != null ? text : "");
        }


        if (value == null || value.toString().isEmpty()) {
            // 空单元格：使用主题适配的空单元格背景色（有区分度）
            setBackground(ModernColors.getEmptyCellBackground());
        } else if (isSelected) {
            // 选中状态：使用选中背景色
            setBackground(table.getSelectionBackground());
        } else {
            // 有值且非选中：使用表格背景色
            setBackground(table.getBackground());
        }

        return this;
    }

    /**
     * 动态计算基于列宽度和字体的可显示字符数
     *
     * @param text        要显示的文本
     * @param columnWidth 列宽度（像素）
     * @return 可显示的最大字符数
     */
    private int calculateMaxDisplayLength(String text, int columnWidth) {
        if (text == null || text.isEmpty() || columnWidth <= 0) {
            return 0;
        }

        // 获取当前字体
        Font font = getFont();
        if (font == null) {
            return text.length(); // 如果无法获取字体，不截断
        }

        // 获取 FontMetrics
        FontMetrics fm = getFontMetrics(font);
        if (fm == null) {
            return text.length();
        }

        // 预留空间：左右边距 + "..." 的宽度
        int ellipsisWidth = fm.stringWidth("...");
        int availableWidth = columnWidth - 10 - ellipsisWidth; // 10px 为边距预留

        if (availableWidth <= 0) {
            return 0;
        }

        // 计算文本实际宽度
        int textWidth = fm.stringWidth(text);

        // 如果文本宽度小于可用宽度，不需要截断
        if (textWidth <= availableWidth) {
            return text.length();
        }

        // 二分查找最佳截断位置
        int left = 0;
        int right = text.length();
        int bestLength = 0;

        while (left <= right) {
            int mid = (left + right) / 2;
            String substring = text.substring(0, mid);
            int substringWidth = fm.stringWidth(substring);

            if (substringWidth <= availableWidth) {
                bestLength = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return Math.max(0, bestLength);
    }
}