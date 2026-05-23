package com.laker.postman.panel.performance.result;

import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.button.SegmentedButtonGroupPanel;
import com.laker.postman.common.component.button.SegmentedToggleButton;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceReportSnapshot;
import com.laker.postman.panel.performance.model.PerformanceStatsSnapshot;
import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import com.laker.postman.util.TimeDisplayUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PerformanceReportPanel extends JPanel {

    private static final int FAIL_COLUMN_INDEX = 3;
    private static final int SUCCESS_RATE_COLUMN_INDEX = 4;
    private static final int API_NAME_MIN_WIDTH = 110;
    private static final int API_NAME_PREFERRED_WIDTH = 160;
    private static final int GENERIC_API_NAME_PREFERRED_WIDTH = 180;

    // 成功率阈值
    private static final double SUCCESS_RATE_EXCELLENT = 99.0;
    private static final double SUCCESS_RATE_GOOD = 90.0;

    private final DefaultTableModel reportTableModel;
    private final DefaultTableModel webSocketReportTableModel;
    private final DefaultTableModel sseReportTableModel;
    private final String[] columns;
    private final String[] webSocketColumns;
    private final String[] sseColumns;
    private final String totalRowName;

    // 单例渲染器，避免重复创建
    private final DefaultTableCellRenderer failRenderer;
    private final DefaultTableCellRenderer rateRenderer;
    private final DefaultTableCellRenderer generalRenderer;

    public PerformanceReportPanel() {
        // Initialize internationalized column names
        this.columns = new String[]{
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_API_NAME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_TOTAL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_FAIL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_QPS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MIN),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MAX),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P90),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P99)
        };
        this.webSocketColumns = new String[]{
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_API_NAME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SESSIONS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_FAIL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_RECEIVED),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MATCHED),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SEND_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_RECEIVE_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_FIRST_MESSAGE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_SESSION),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_SESSION),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_COMPLETION)
        };
        this.sseColumns = new String[]{
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_API_NAME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_STREAMS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_FAIL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_EVENTS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MATCHED),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_EVENT_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MATCHED_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_FIRST_EVENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P90_FIRST_EVENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_FIRST_EVENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P99_FIRST_EVENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_STREAM),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_STREAM),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_COMPLETION)
        };
        this.totalRowName = I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_TOTAL_ROW);

        // 创建单例渲染器
        this.failRenderer = createFailRenderer();
        this.rateRenderer = createRateRenderer();
        this.generalRenderer = createGeneralRenderer();

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        reportTableModel = createTableModel(columns);
        webSocketReportTableModel = createTableModel(webSocketColumns);
        sseReportTableModel = createTableModel(sseColumns);
        JTable reportTable = createReportTable();
        JTable webSocketReportTable = createGenericReportTable(webSocketReportTableModel);
        JTable sseReportTable = createGenericReportTable(sseReportTableModel);

        JPanel reportCards = new JPanel(new CardLayout());
        reportCards.add(createReportScrollPane(reportTable), PerformanceProtocol.HTTP.name());
        reportCards.add(createReportScrollPane(webSocketReportTable), PerformanceProtocol.WEBSOCKET.name());
        reportCards.add(createReportScrollPane(sseReportTable), PerformanceProtocol.SSE.name());
        add(createToolbar(reportCards), BorderLayout.NORTH);
        add(reportCards, BorderLayout.CENTER);
    }

    private JPanel createToolbar(JPanel reportCards) {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        toolbar.add(createProtocolSwitcher(reportCards), BorderLayout.WEST);

        JButton copyReportButton = ModernButtonFactory.createButton(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COPY_MARKDOWN_BUTTON),
                false
        );
        copyReportButton.addActionListener(e -> copyMarkdownReport());
        toolbar.add(copyReportButton, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createProtocolSwitcher(JPanel reportCards) {
        ButtonGroup protocolGroup = new ButtonGroup();
        JPanel switcher = new SegmentedButtonGroupPanel(FlowLayout.LEFT);
        for (PerformanceProtocol protocol : PerformanceProtocol.values()) {
            JToggleButton button = new SegmentedToggleButton(
                    protocol.getDisplayName(),
                    protocol == PerformanceProtocol.HTTP
            );
            button.addActionListener(e -> {
                CardLayout layout = (CardLayout) reportCards.getLayout();
                layout.show(reportCards, protocol.name());
            });
            protocolGroup.add(button);
            switcher.add(button);
        }
        return switcher;
    }

    private DefaultTableModel createTableModel(String[] tableColumns) {
        return new DefaultTableModel(tableColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable createReportTable() {
        JTable table = createTableWithHeaderTooltips(reportTableModel);
        table.setFocusable(false);
        table.setFillsViewportHeight(true);
        configureResizableColumns(table, false);

        configureColumnRenderers(table);
        configureColumnWidths(table);
        table.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD));

        return table;
    }

    private JTable createGenericReportTable(DefaultTableModel model) {
        JTable table = createTableWithHeaderTooltips(model);
        table.setFocusable(false);
        table.setFillsViewportHeight(true);
        configureResizableColumns(table, true);
        table.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD));
        DefaultTableCellRenderer centerRenderer = createCenteredRenderer(model);
        DefaultTableCellRenderer nameRenderer = createNameRenderer(model);
        DefaultTableCellRenderer streamFailRenderer = createFailRenderer(model);
        DefaultTableCellRenderer streamRateRenderer = createRateRenderer(model);
        table.getColumnModel().getColumn(0).setCellRenderer(nameRenderer);
        for (int col = 1; col < model.getColumnCount(); col++) {
            if (col == FAIL_COLUMN_INDEX) {
                table.getColumnModel().getColumn(col).setCellRenderer(streamFailRenderer);
            } else if (col == SUCCESS_RATE_COLUMN_INDEX) {
                table.getColumnModel().getColumn(col).setCellRenderer(streamRateRenderer);
            } else {
                table.getColumnModel().getColumn(col).setCellRenderer(centerRenderer);
            }
        }
        configureStreamReportColumnWidths(table);
        return table;
    }

    private JTable createTableWithHeaderTooltips(DefaultTableModel model) {
        return new JTable(model) {
            @Override
            protected JTableHeader createDefaultTableHeader() {
                return new ReportTableHeader(columnModel);
            }
        };
    }

    private JScrollPane createReportScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    private void configureResizableColumns(JTable table, boolean denseColumns) {
        // Fill the viewport so wide report panels do not leave a blank strip on the right.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getTableHeader().setResizingAllowed(true);
        table.getTableHeader().setReorderingAllowed(false);
    }

    private void configureStreamReportColumnWidths(JTable table) {
        if (table.getColumnModel().getColumnCount() == 0) {
            return;
        }
        int[] widths = table.getColumnCount() == webSocketColumns.length
                ? new int[]{180, 72, 70, 60, 90, 70, 70, 70, 104, 104, 112, 118, 118, 120}
                : new int[]{180, 72, 70, 60, 90, 70, 70, 92, 104, 112, 112, 112, 112, 118, 118, 120};
        for (int col = 0; col < table.getColumnModel().getColumnCount() && col < widths.length; col++) {
            int width = widths[col];
            table.getColumnModel().getColumn(col).setMinWidth(col == 0 ? API_NAME_MIN_WIDTH : 56);
            table.getColumnModel().getColumn(col).setPreferredWidth(width);
        }
    }

    private DefaultTableCellRenderer createCenteredRenderer(DefaultTableModel model) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                if (isTotalRow(model, modelRow)) {
                    applyTotalRowStyle(c);
                } else {
                    applyDefaultCellStyle(c);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createNameRenderer(DefaultTableModel model) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                if (isTotalRow(model, modelRow)) {
                    applyTotalRowStyle(c);
                } else {
                    applyDefaultCellStyle(c);
                }
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        };
    }

    private void configureColumnWidths(JTable table) {
        if (table.getColumnModel().getColumnCount() > 0) {
            // API Name 列
            table.getColumnModel().getColumn(0).setMinWidth(API_NAME_MIN_WIDTH);
            table.getColumnModel().getColumn(0).setPreferredWidth(API_NAME_PREFERRED_WIDTH);

            // Total 列 - 显示 "Total"（5个字符）+ 数字
            table.getColumnModel().getColumn(1).setMinWidth(65);
            table.getColumnModel().getColumn(1).setPreferredWidth(75);

            // Success 列 - 显示 "Success"（7个字符）+ 数字
            table.getColumnModel().getColumn(2).setMinWidth(75);
            table.getColumnModel().getColumn(2).setPreferredWidth(85);

            // Fail 列 - 显示 "Fail"（4个字符）+ 数字
            table.getColumnModel().getColumn(3).setMinWidth(60);
            table.getColumnModel().getColumn(3).setPreferredWidth(65);

            // Success Rate 列 - 显示 "Success Rate"（12个字符）+ 百分比
            table.getColumnModel().getColumn(4).setMinWidth(110);
            table.getColumnModel().getColumn(4).setPreferredWidth(120);

            // QPS 列 - 显示 "QPS"（3个字符）+ 数字
            table.getColumnModel().getColumn(5).setMinWidth(60);
            table.getColumnModel().getColumn(5).setPreferredWidth(70);

            // Avg 列 - 显示 "Avg"（3个字符）+ 时间
            table.getColumnModel().getColumn(6).setMinWidth(65);
            table.getColumnModel().getColumn(6).setPreferredWidth(75);

            // Min 列 - 显示 "Min"（3个字符）+ 时间
            table.getColumnModel().getColumn(7).setMinWidth(65);
            table.getColumnModel().getColumn(7).setPreferredWidth(75);

            // Max 列 - 显示 "Max"（3个字符）+ 时间
            table.getColumnModel().getColumn(8).setMinWidth(65);
            table.getColumnModel().getColumn(8).setPreferredWidth(75);

            // P90 列 - 显示 "P90"（3个字符）+ 时间
            table.getColumnModel().getColumn(9).setMinWidth(65);
            table.getColumnModel().getColumn(9).setPreferredWidth(75);

            // P95 列 - 显示 "P95"（3个字符）+ 时间
            table.getColumnModel().getColumn(10).setMinWidth(65);
            table.getColumnModel().getColumn(10).setPreferredWidth(75);

            // P99 列 - 显示 "P99"（3个字符）+ 时间
            table.getColumnModel().getColumn(11).setMinWidth(65);
            table.getColumnModel().getColumn(11).setPreferredWidth(75);
        }
    }

    private void configureColumnRenderers(JTable table) {
        // 使用单例渲染器，避免重复创建
        // 需要居中的列索引（从第2列到最后一列）
        for (int col = 1; col < columns.length; col++) {
            if (col == FAIL_COLUMN_INDEX) {
                table.getColumnModel().getColumn(col).setCellRenderer(failRenderer);
            } else if (col == SUCCESS_RATE_COLUMN_INDEX) {
                table.getColumnModel().getColumn(col).setCellRenderer(rateRenderer);
            } else {
                table.getColumnModel().getColumn(col).setCellRenderer(generalRenderer);
            }
        }
    }

    private DefaultTableCellRenderer createFailRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = isTotalRow(modelRow);

                if (isTotal) {
                    applyTotalRowStyle(c);
                    applyTotalFailForeground(c, value);
                } else {
                    applyFailCellStyle(c, value);
                }

                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createRateRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = isTotalRow(modelRow);

                if (isTotal) {
                    applyTotalRowStyle(c);
                    applyRateForeground(c, value);
                } else {
                    applyRateCellStyle(c, value);
                }

                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createGeneralRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = isTotalRow(modelRow);

                if (isTotal) {
                    applyTotalRowStyle(c);
                } else {
                    applyDefaultCellStyle(c);
                }

                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createFailRenderer(DefaultTableModel model) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                if (isTotalRow(model, modelRow)) {
                    applyTotalRowStyle(c);
                    applyTotalFailForeground(c, value);
                } else {
                    applyFailCellStyle(c, value);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createRateRenderer(DefaultTableModel model) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                if (isTotalRow(model, modelRow)) {
                    applyTotalRowStyle(c);
                    applyRateForeground(c, value);
                } else {
                    applyRateCellStyle(c, value);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
    }

    private boolean isTotalRow(int modelRow) {
        return isTotalRow(reportTableModel, modelRow);
    }

    private boolean isTotalRow(DefaultTableModel model, int modelRow) {
        Object firstColumnValue = model.getValueAt(modelRow, 0);
        return totalRowName.equals(firstColumnValue);
    }

    private void applyTotalRowStyle(Component c) {
        c.setFont(c.getFont().deriveFont(Font.BOLD));
        c.setForeground(UIManager.getColor("Performance.report.totalForeground"));
        c.setBackground(UIManager.getColor("Performance.report.totalBackground"));
    }

    private void applyFailCellStyle(Component c, Object value) {
        applyFailForeground(c, value);
        c.setBackground(UIManager.getColor("Table.background"));
    }

    private void applyFailForeground(Component c, Object value) {
        try {
            int failCount = Integer.parseInt(value == null ? "0" : value.toString());
            c.setForeground(failCount > 0 ? Color.RED : UIManager.getColor("Table.foreground"));
        } catch (Exception e) {
            c.setForeground(UIManager.getColor("Table.foreground"));
        }
    }

    private void applyTotalFailForeground(Component c, Object value) {
        try {
            int failCount = Integer.parseInt(value == null ? "0" : value.toString());
            if (failCount > 0) {
                c.setForeground(Color.RED);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyRateCellStyle(Component c, Object value) {
        applyRateForeground(c, value);
        c.setBackground(UIManager.getColor("Table.background"));
    }

    private void applyRateForeground(Component c, Object value) {
        String rateStr = value != null ? value.toString() : "";
        if (rateStr.endsWith("%")) {
            try {
                double rate = Double.parseDouble(rateStr.replace("%", ""));
                if (rate >= SUCCESS_RATE_EXCELLENT) {
                    c.setForeground(UIManager.getColor("Performance.report.successColor"));
                } else if (rate >= SUCCESS_RATE_GOOD) {
                    c.setForeground(UIManager.getColor("Performance.report.warningColor"));
                } else {
                    c.setForeground(Color.RED);
                }
            } catch (Exception e) {
                c.setForeground(UIManager.getColor("Table.foreground"));
            }
        } else {
            c.setForeground(UIManager.getColor("Table.foreground"));
        }
    }

    private void applyDefaultCellStyle(Component c) {
        c.setForeground(UIManager.getColor("Table.foreground"));
        c.setBackground(UIManager.getColor("Table.background"));
    }


    public void clearReport() {
        reportTableModel.setRowCount(0);
        webSocketReportTableModel.setRowCount(0);
        sseReportTableModel.setRowCount(0);
    }

    private void addReportRow(DefaultTableModel model, Object[] rowData) {
        if (rowData == null) {
            throw new IllegalArgumentException("Row data cannot be null");
        }
        if (rowData.length != model.getColumnCount()) {
            throw new IllegalArgumentException(
                    String.format("Row data must match the number of columns. Expected: %d, Actual: %d",
                            model.getColumnCount(), rowData.length));
        }
        model.addRow(rowData);
    }

    public void updateReport(Map<String, List<Long>> apiCostMap,
                             Map<String, Integer> apiSuccessMap,
                             Map<String, Integer> apiFailMap,
                             List<RequestResult> allRequestResults) {
        updateReport(PerformanceProtocolReportData.fromResults(allRequestResults, totalRowName));
    }

    public void updateReport(PerformanceStatsSnapshot statsSnapshot) {
        updateReport(PerformanceProtocolReportData.fromStatsSnapshot(statsSnapshot, totalRowName));
    }

    public void updateReport(PerformanceReportSnapshot reportSnapshot) {
        updateReport(PerformanceProtocolReportData.fromReportSnapshot(reportSnapshot, totalRowName));
    }

    private void updateReport(PerformanceProtocolReportData reportData) {
        clearReport();

        for (PerformanceProtocolReportData.HttpReportRow row : reportData.httpRows()) {
            addReportRow(reportTableModel, toHttpRowData(row));
        }
        for (PerformanceProtocolReportData.StreamReportRow row : reportData.webSocketRows()) {
            addReportRow(webSocketReportTableModel, toWebSocketRowData(row));
        }
        for (PerformanceProtocolReportData.StreamReportRow row : reportData.sseRows()) {
            addReportRow(sseReportTableModel, toSseRowData(row));
        }
    }

    private Object[] toHttpRowData(PerformanceProtocolReportData.HttpReportRow row) {
        return new Object[]{
                row.name(),
                row.total(),
                row.success(),
                row.fail(),
                formatPercent(row.successRate()),
                formatDecimal(row.qps()),
                TimeDisplayUtil.formatElapsedTime(row.avg()),
                TimeDisplayUtil.formatElapsedTime(row.min()),
                TimeDisplayUtil.formatElapsedTime(row.max()),
                TimeDisplayUtil.formatElapsedTime(row.p90()),
                TimeDisplayUtil.formatElapsedTime(row.p95()),
                TimeDisplayUtil.formatElapsedTime(row.p99())
        };
    }

    private Object[] toWebSocketRowData(PerformanceProtocolReportData.StreamReportRow row) {
        return new Object[]{
                row.name(),
                row.total(),
                formatStreamCompletedCount(row),
                formatStreamFailedCount(row),
                formatStreamSuccessRate(row),
                row.sentMessages(),
                row.receivedMessages(),
                row.matchedMessages(),
                formatDecimal(row.sendRate()),
                formatDecimal(row.receiveRate()),
                TimeDisplayUtil.formatElapsedTime(row.avgFirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.avgDurationMs()),
                TimeDisplayUtil.formatElapsedTime(row.p95DurationMs()),
                formatCompletionReason(row.topCompletionReason())
        };
    }

    private Object[] toSseRowData(PerformanceProtocolReportData.StreamReportRow row) {
        return new Object[]{
                row.name(),
                row.total(),
                formatStreamCompletedCount(row),
                formatStreamFailedCount(row),
                formatStreamSuccessRate(row),
                row.receivedMessages(),
                row.matchedMessages(),
                formatDecimal(row.receiveRate()),
                formatDecimal(row.matchedRate()),
                TimeDisplayUtil.formatElapsedTime(row.avgFirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.p90FirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.p95FirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.p99FirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.avgDurationMs()),
                TimeDisplayUtil.formatElapsedTime(row.p95DurationMs()),
                formatCompletionReason(row.topCompletionReason())
        };
    }

    private String formatCompletionReason(String reason) {
        if (reason == null || reason.isBlank() || "-".equals(reason)) {
            return "-";
        }
        String key = switch (reason) {
            case "pending" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_PENDING;
            case "closed" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_CLOSED;
            case "interrupted" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_INTERRUPTED;
            case "failure" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_FAILURE;
            case "connect_timeout" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_CONNECT_TIMEOUT;
            case "send_skipped" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_SEND_SKIPPED;
            case "send_pre_script_failed" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_SEND_PRE_SCRIPT_FAILED;
            case "sent" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_SENT;
            case "send_failed" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_SEND_FAILED;
            case "first_message" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_FIRST_MESSAGE;
            case "matched_message" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_MATCHED_MESSAGE;
            case "message_target" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_MESSAGE_TARGET;
            case "hold_complete" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_HOLD_COMPLETE;
            case "message_target_timeout" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_MESSAGE_TARGET_TIMEOUT;
            case "await_timeout" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_AWAIT_TIMEOUT;
            case "first_message_timeout" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_FIRST_MESSAGE_TIMEOUT;
            case "matched_message_timeout" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_MATCHED_MESSAGE_TIMEOUT;
            case "closed_early" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_CLOSED_EARLY;
            case "closed_by_step" -> MessageKeys.PERFORMANCE_REPORT_COMPLETION_CLOSED_BY_STEP;
            default -> null;
        };
        return key == null ? reason : I18nUtil.getMessage(key);
    }

    private String formatPercent(double value) {
        if (Double.isNaN(value)) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.2f%%", value);
    }

    private Object formatStreamCompletedCount(PerformanceProtocolReportData.StreamReportRow row) {
        return row.live() ? "-" : row.success();
    }

    private Object formatStreamFailedCount(PerformanceProtocolReportData.StreamReportRow row) {
        return row.live() ? "-" : row.fail();
    }

    private String formatStreamSuccessRate(PerformanceProtocolReportData.StreamReportRow row) {
        return row.live() ? "-" : formatPercent(row.successRate());
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class ReportTableHeader extends JTableHeader {
        private ReportTableHeader(TableColumnModel columnModel) {
            super(columnModel);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int column = columnAtPoint(event.getPoint());
            if (column < 0) {
                return null;
            }
            Object headerValue = getColumnModel().getColumn(column).getHeaderValue();
            return headerValue == null ? null : headerValue.toString();
        }
    }

    void copyMarkdownReport() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(buildMarkdownReport()), null);
        NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_MARKDOWN_COPIED));
    }

    String buildMarkdownReport() {
        if (reportTableModel.getRowCount() == 0
                && webSocketReportTableModel.getRowCount() == 0
                && sseReportTableModel.getRowCount() == 0) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_MARKDOWN_EMPTY);
        }

        StringBuilder markdown = new StringBuilder(1024);
        markdown.append("# ")
                .append(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_MARKDOWN_TITLE))
                .append("\n\n");
        appendMarkdownTable(markdown, PerformanceProtocol.HTTP.getDisplayName(), reportTableModel);
        appendMarkdownTable(markdown, PerformanceProtocol.WEBSOCKET.getDisplayName(), webSocketReportTableModel);
        appendMarkdownTable(markdown, PerformanceProtocol.SSE.getDisplayName(), sseReportTableModel);
        return markdown.toString();
    }

    private void appendMarkdownTable(StringBuilder markdown, String title, DefaultTableModel model) {
        if (model.getRowCount() == 0) {
            return;
        }
        markdown.append("## ").append(title).append("\n\n");
        for (int col = 0; col < model.getColumnCount(); col++) {
            markdown.append("| ").append(escapeMarkdownCell(model.getColumnName(col))).append(' ');
        }
        markdown.append("|\n");
        for (int col = 0; col < model.getColumnCount(); col++) {
            markdown.append("| --- ");
        }
        markdown.append("|\n");
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                markdown.append("| ").append(escapeMarkdownCell(valueAt(model, row, col))).append(' ');
            }
            markdown.append("|\n");
        }
        markdown.append('\n');
    }

    private String valueAt(DefaultTableModel model, int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private String escapeMarkdownCell(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }
}
