package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.editor.request.StreamMessageUiMetadata;
import com.laker.postman.stream.MessageType;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Locale;
import java.util.function.IntFunction;

final class StreamMessageTableSupport {
    private static final int HEADER_HORIZONTAL_PADDING = 30;

    private StreamMessageTableSupport() {
    }

    static void configureBaseTable(JTable table, IntFunction<MessageType> rowTypeProvider) {
        table.setRowHeight(24);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(ModernColors.getTableGridColor());
        table.setShowGrid(true);
        table.setBorder(BorderFactory.createEmptyBorder());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        JTableHeader header = table.getTableHeader();
        header.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        table.setDefaultRenderer(Object.class, new StatusAwareCellRenderer(rowTypeProvider));
    }

    static void configureTypeColumn(JTable table, int columnIndex) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setMinWidth(44);
        column.setPreferredWidth(52);
        column.setMaxWidth(60);
        column.setCellRenderer(new StreamMessageTypeCellRenderer());
    }

    static void configureTimeColumn(JTable table, int columnIndex, IntFunction<MessageType> rowTypeProvider) {
        int headerWidth = headerWidth(table, table.getColumnName(columnIndex), "Time", "时间", "23:59:59.999");
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setMinWidth(Math.max(118, headerWidth));
        column.setPreferredWidth(Math.max(128, headerWidth));
        column.setMaxWidth(Math.max(170, headerWidth));

        DefaultTableCellRenderer centerRenderer = new StatusAwareCellRenderer(rowTypeProvider);
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        column.setCellRenderer(centerRenderer);
    }

    static void configureIntervalColumn(JTable table, int columnIndex, IntFunction<MessageType> rowTypeProvider) {
        int headerWidth = headerWidth(table, table.getColumnName(columnIndex), "Delta", "间隔", "+999 ms", "+14.69 s");
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setMinWidth(Math.max(86, headerWidth));
        column.setPreferredWidth(Math.max(96, headerWidth));
        column.setMaxWidth(Math.max(130, headerWidth));

        DefaultTableCellRenderer rightRenderer = new StatusAwareCellRenderer(rowTypeProvider);
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        column.setCellRenderer(rightRenderer);
    }

    static void configureContentColumn(JTable table, int columnIndex, int preferredWidth) {
        int headerWidth = headerWidth(table, table.getColumnName(columnIndex), "Content", "内容");
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setMinWidth(Math.max(180, headerWidth));
        column.setPreferredWidth(Math.max(preferredWidth, headerWidth));
    }

    static void configureAssertionColumn(JTable table, int columnIndex) {
        int headerWidth = headerWidth(table, table.getColumnName(columnIndex), "Assertion", "断言");
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setMinWidth(Math.max(96, headerWidth));
        column.setPreferredWidth(Math.max(112, headerWidth));
        column.setMaxWidth(Math.max(140, headerWidth));
        column.setCellRenderer(new StreamAssertionSummaryCellRenderer());
    }

    static boolean matchesTypeFilter(MessageType type, String typeFilter) {
        if (I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_ALL).equals(typeFilter)) {
            return true;
        }
        if (I18nUtil.getMessage(MessageKeys.STREAM_FILTER_MESSAGES).equals(typeFilter)) {
            return isMessageType(type);
        }
        if (I18nUtil.getMessage(MessageKeys.STREAM_FILTER_STATUS).equals(typeFilter)) {
            return isStatusType(type);
        }
        return StreamMessageUiMetadata.display(type).equals(typeFilter);
    }

    static String formatInterval(Long intervalMs) {
        if (intervalMs == null) {
            return "-";
        }
        long normalized = Math.max(0L, intervalMs);
        if (normalized < 1_000) {
            return "+" + normalized + " ms";
        }
        if (normalized < 60_000) {
            return "+" + String.format(Locale.US, "%.2f s", normalized / 1_000.0);
        }
        long minutes = normalized / 60_000;
        long seconds = (normalized % 60_000) / 1_000;
        return "+" + minutes + " min " + seconds + " s";
    }

    static boolean isMessageType(MessageType type) {
        return type == MessageType.SENT || type == MessageType.RECEIVED || type == MessageType.BINARY;
    }

    static boolean isStatusType(MessageType type) {
        return type == MessageType.CONNECTED
                || type == MessageType.CLOSED
                || type == MessageType.WARNING
                || type == MessageType.INFO;
    }

    static String sourceDisplay(MessageType type) {
        if (type == MessageType.SENT) {
            return I18nUtil.getMessage(MessageKeys.STREAM_SOURCE_CLIENT);
        }
        if (type == MessageType.RECEIVED || type == MessageType.BINARY) {
            return I18nUtil.getMessage(MessageKeys.STREAM_SOURCE_SERVER);
        }
        return I18nUtil.getMessage(MessageKeys.STREAM_SOURCE_STATUS);
    }

    private static int headerWidth(JTable table, String... labels) {
        Font font = table.getTableHeader() != null ? table.getTableHeader().getFont() : table.getFont();
        if (font == null) {
            font = FontsUtil.getDefaultFont(Font.BOLD);
        }
        FontMetrics metrics = table.getFontMetrics(font);
        int width = 0;
        for (String label : labels) {
            if (label != null) {
                width = Math.max(width, metrics.stringWidth(label));
            }
        }
        return width + HEADER_HORIZONTAL_PADDING;
    }

    private static MessageType rowType(JTable table, IntFunction<MessageType> rowTypeProvider, int viewRow) {
        if (rowTypeProvider == null || viewRow < 0 || viewRow >= table.getRowCount()) {
            return null;
        }
        return rowTypeProvider.apply(viewRow);
    }

    private static final class StatusAwareCellRenderer extends DefaultTableCellRenderer {
        private final IntFunction<MessageType> rowTypeProvider;
        private final Font plainFont = FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0);
        private final Font boldFont = FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0);

        private StatusAwareCellRenderer(IntFunction<MessageType> rowTypeProvider) {
            this.rowTypeProvider = rowTypeProvider;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            MessageType type = rowType(table, rowTypeProvider, row);
            setFont(isSelected ? boldFont : plainFont);
            if (!isSelected && isStatusType(type)) {
                setForeground(type == MessageType.WARNING
                        ? ModernColors.getWarning()
                        : ModernColors.getTextSecondary());
            }
            setToolTipText(value == null ? null : I18nUtil.getMessage(MessageKeys.STREAM_TOOLTIP_OPEN_DETAIL));
            return this;
        }
    }
}
