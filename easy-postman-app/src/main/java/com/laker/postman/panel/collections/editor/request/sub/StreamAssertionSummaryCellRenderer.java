package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

final class StreamAssertionSummaryCellRenderer extends DefaultTableCellRenderer {
    private final Icon passedIcon = IconUtil.create("icons/pass.svg", 16, 16);
    private final Icon failedIcon = IconUtil.create("icons/fail.svg", 16, 16);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof StreamAssertionSummary summary) {
            Color statusColor = summary.passed() ? ModernColors.getSuccess() : ModernColors.getError();
            setText(summary.toString());
            setIcon(summary.passed() ? passedIcon : failedIcon);
            setForeground(isSelected ? table.getSelectionForeground() : statusColor);
            setToolTipText(I18nUtil.getMessage(MessageKeys.STREAM_ASSERTION_TOOLTIP));
            if (!isSelected) {
                setBackground(ModernColors.withAlpha(statusColor, ModernColors.isDarkTheme() ? 44 : 24));
            }
        } else {
            setText("");
            setIcon(null);
            setToolTipText(null);
        }
        setHorizontalAlignment(CENTER);
        setIconTextGap(5);
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        return this;
    }
}
