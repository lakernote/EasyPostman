package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class EasyTextFieldCellRenderer extends EasyTextField implements TableCellRenderer {

    // 交替行的微弱斑马纹颜色（相对表格背景稍深/浅）
    private static final float STRIPE_ALPHA = 0.04f;

    public EasyTextFieldCellRenderer() {
        super(1);
        setBorder(TableUIConstants.createLabelBorder());
        setOpaque(true);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBorder(TableUIConstants.createLabelBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        // value 可能为 null，统一转为空字符串
        String text = (value == null) ? "" : value.toString();

        // 动态计算可显示的最大字符数
        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        int maxDisplayLength = calculateMaxDisplayLength(text, columnWidth);

        // 设置显示文本（如果过长则截断）
        boolean truncated = false;
        if (maxDisplayLength > 0 && text.length() > maxDisplayLength) {
            setText(text.substring(0, maxDisplayLength) + "...");
            truncated = true;
        } else {
            setText(text);
        }

        // Tooltip：截断时显示完整文本，方便查看
        setToolTipText(truncated ? text : null);

        int hoveredRow = getHoveredRow(table);
        setBackground(TableUIConstants.getCellBackground(isSelected, row == hoveredRow, false, table, row));
        setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());

        return this;
    }

    /**
     * 从 JTable 向上遍历父容器层级，找到 AbstractTablePanel 并返回 hoveredRow。
     * 使用循环遍历而非固定深度，适配不同的容器嵌套结构。
     */
    private static int getHoveredRow(JTable table) {
        java.awt.Container parent = table.getParent();
        while (parent != null) {
            if (parent instanceof AbstractTablePanel<?> panel) {
                return panel.hoveredRow;
            }
            parent = parent.getParent();
        }
        return -1;
    }

    /**
     * 混合两种颜色
     *
     * @param base   基础色
     * @param blend  混入色
     * @param alpha  混入比例 0.0–1.0
     */
    private static Color blendColor(Color base, Color blend, float alpha) {
        int r = Math.min(255, Math.max(0, Math.round(base.getRed()   * (1 - alpha) + blend.getRed()   * alpha)));
        int g = Math.min(255, Math.max(0, Math.round(base.getGreen() * (1 - alpha) + blend.getGreen() * alpha)));
        int b = Math.min(255, Math.max(0, Math.round(base.getBlue()  * (1 - alpha) + blend.getBlue()  * alpha)));
        return new Color(r, g, b);
    }

    static Color stripeBackground(Color base) {
        return blendColor(base, ModernColors.getTextPrimary(), STRIPE_ALPHA);
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
        int availableWidth = columnWidth - TableUIConstants.PADDING_LEFT - TableUIConstants.PADDING_RIGHT - ellipsisWidth;

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
