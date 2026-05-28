package com.laker.postman.panel.performance.result;

import com.laker.postman.common.component.button.SegmentedButtonGroupPanel;
import com.laker.postman.common.component.button.SegmentedToggleButton;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceProtocolLabels;
import com.laker.postman.performance.core.model.RequestResult;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceReportPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldKeepQpsPrecisionToTwoDecimals() throws Exception {
        List<RequestResult> results = List.of(
                new RequestResult(1_000, 1_100, true, "search", "Search API", PerformanceProtocol.HTTP),
                new RequestResult(2_000, 2_100, true, "search", "Search API", PerformanceProtocol.HTTP),
                new RequestResult(2_900, 3_000, true, "search", "Search API", PerformanceProtocol.HTTP)
        );

        PerformanceReportPanel panel = new PerformanceReportPanel();
        panel.updateReport(
                Map.of("search", List.of(100L, 100L, 100L)),
                Map.of("search", 3),
                Map.of(),
                results
        );

        DefaultTableModel model = getReportTableModel(panel);
        assertEquals(model.getValueAt(0, 5).toString(), "1.50");
        assertEquals(model.getValueAt(1, 5).toString(), "1.50");
    }

    @Test
    public void shouldBuildMarkdownReportFromCurrentTable() {
        List<RequestResult> results = List.of(
                new RequestResult(1_000, 1_100, true, "search", "Search API", PerformanceProtocol.HTTP),
                new RequestResult(2_900, 3_000, true, "search", "Search API", PerformanceProtocol.HTTP)
        );

        PerformanceReportPanel panel = new PerformanceReportPanel();
        panel.updateReport(
                Map.of("search", List.of(100L, 100L)),
                Map.of("search", 2),
                Map.of(),
                results
        );

        String markdown = panel.buildMarkdownReport();

        assertTrue(markdown.contains("#"), markdown);
        assertTrue(markdown.contains("Search API"), markdown);
        assertTrue(markdown.contains("| QPS |"), markdown);
        assertTrue(markdown.contains("1.00"), markdown);
    }

    @Test
    public void shouldShowCopyDataButtonOnReportToolbar() {
        PerformanceReportPanel panel = new PerformanceReportPanel();

        List<JButton> buttons = findAll(panel, JButton.class);

        assertFalse(buttons.isEmpty());
        assertTrue(buttons.stream().anyMatch(button ->
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COPY_MARKDOWN_BUTTON).equals(button.getText())));
    }

    @Test
    public void shouldUseCompactProtocolSwitcherInsteadOfNestedTabs() {
        PerformanceReportPanel panel = new PerformanceReportPanel();

        List<SegmentedToggleButton> protocolButtons = findAll(panel, SegmentedToggleButton.class);

        assertTrue(findAll(panel, JTabbedPane.class).isEmpty());
        assertEquals(findAll(panel, SegmentedButtonGroupPanel.class).size(), 1);
        assertEquals(protocolButtons.size(), PerformanceProtocol.values().length);
        assertTrue(protocolButtons.stream().anyMatch(button ->
                PerformanceProtocolLabels.displayName(PerformanceProtocol.HTTP).equals(button.getText()) && button.isSelected()));
        assertTrue(protocolButtons.stream().anyMatch(button ->
                PerformanceProtocolLabels.displayName(PerformanceProtocol.WEBSOCKET).equals(button.getText())));
        assertTrue(protocolButtons.stream().anyMatch(button ->
                PerformanceProtocolLabels.displayName(PerformanceProtocol.SSE).equals(button.getText())));
    }

    @Test
    public void shouldKeepReportColumnsResizableWithCompactApiNameWidth() {
        PerformanceReportPanel panel = new PerformanceReportPanel();

        List<JTable> tables = findAll(panel, JTable.class);

        assertEquals(tables.size(), PerformanceProtocol.values().length);
        for (JTable table : tables) {
            assertTrue(table.getTableHeader().getResizingAllowed());
            assertFalse(table.getTableHeader().getReorderingAllowed());
            assertTrue(table.getColumnModel().getColumn(0).getPreferredWidth() <= 180);
            for (int col = 0; col < table.getColumnModel().getColumnCount(); col++) {
                assertEquals(table.getColumnModel().getColumn(col).getMaxWidth(), Integer.MAX_VALUE);
            }
        }

        JTable httpTable = findTableByColumnCount(tables, 12);
        JTable webSocketTable = findTableByColumnCount(tables, 13);
        JTable sseTable = findTableByColumnCount(tables, 15);

        assertEquals(httpTable.getAutoResizeMode(), JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        assertEquals(webSocketTable.getAutoResizeMode(), JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        assertEquals(sseTable.getAutoResizeMode(), JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        assertTrue(sseTable.getColumnModel().getColumn(13).getPreferredWidth() >= 110);
        assertTrue(sseTable.getColumnModel().getColumn(14).getPreferredWidth() >= 110);
        assertEquals(headerTooltipAt(sseTable, 13), sseTable.getColumnName(13));
    }

    @Test
    public void shouldRenderSseFirstEventPercentileColumns() throws Exception {
        List<RequestResult> results = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            RequestResult result = new RequestResult(i * 1_000L, i * 1_000L + 2_000L,
                    true, "stream", "Stream API", PerformanceProtocol.SSE);
            result.firstMessageLatencyMs = i * 100L;
            result.receivedMessages = 1;
            result.matchedMessages = 1;
            results.add(result);
        }

        PerformanceReportPanel panel = new PerformanceReportPanel();
        panel.updateReport(Map.of(), Map.of(), Map.of(), results);

        DefaultTableModel model = getSseReportTableModel(panel);
        assertEquals(model.getColumnCount(), 15);
        assertEquals(model.getColumnName(9),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_FIRST_EVENT));
        assertEquals(model.getColumnName(10),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P90_FIRST_EVENT));
        assertEquals(model.getColumnName(11),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_FIRST_EVENT));
        assertEquals(model.getColumnName(12),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P99_FIRST_EVENT));
        assertEquals(model.getValueAt(0, 9), "550 ms");
        assertEquals(model.getValueAt(0, 10), "910 ms");
        assertEquals(model.getValueAt(0, 11), "955 ms");
        assertEquals(model.getValueAt(0, 12), "991 ms");
    }

    @Test
    public void shouldNotExposeStreamCompletionReasonColumns() throws Exception {
        RequestResult ws = new RequestResult(1_000, 2_000, true, "ws", "WebSocket API", PerformanceProtocol.WEBSOCKET);
        RequestResult sse = new RequestResult(1_000, 2_000, true, "sse", "SSE API", PerformanceProtocol.SSE);

        PerformanceReportPanel panel = new PerformanceReportPanel();
        panel.updateReport(Map.of(), Map.of(), Map.of(), List.of(ws, sse));

        DefaultTableModel webSocketModel = getWebSocketReportTableModel(panel);
        DefaultTableModel sseModel = getSseReportTableModel(panel);

        assertEquals(webSocketModel.getColumnCount(), 13);
        assertEquals(sseModel.getColumnCount(), 15);
        assertFalse(hasColumn(webSocketModel, "完成原因"));
        assertFalse(hasColumn(webSocketModel, "Completion"));
        assertFalse(hasColumn(sseModel, "完成原因"));
        assertFalse(hasColumn(sseModel, "Completion"));
        assertFalse(panel.buildMarkdownReport().contains("完成原因"));
        assertFalse(panel.buildMarkdownReport().contains("| Completion |"));
    }

    @Test
    public void shouldColorStreamSuccessRateWhenFailuresExist() {
        RequestResult success = new RequestResult(1_000, 2_000, true, "ws", "WebSocket API", PerformanceProtocol.WEBSOCKET);
        RequestResult failure = new RequestResult(2_000, 3_000, false, "ws", "WebSocket API", PerformanceProtocol.WEBSOCKET);

        PerformanceReportPanel panel = new PerformanceReportPanel();
        panel.updateReport(Map.of(), Map.of(), Map.of(), List.of(success, failure));

        JTable webSocketTable = findTableByColumnCount(findAll(panel, JTable.class), 13);
        Component apiRateCell = webSocketTable.getCellRenderer(0, 4)
                .getTableCellRendererComponent(webSocketTable, webSocketTable.getValueAt(0, 4), false, false, 0, 4);
        Component totalRateCell = webSocketTable.getCellRenderer(1, 4)
                .getTableCellRendererComponent(webSocketTable, webSocketTable.getValueAt(1, 4), false, false, 1, 4);

        assertEquals(apiRateCell.getForeground(), Color.RED);
        assertEquals(totalRateCell.getForeground(), Color.RED);
    }

    @Test
    public void shouldDescribeStreamReportDurationAsSampleDuration() {
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.CHINESE);
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_SESSION), "平均样本耗时");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_STREAM), "平均样本耗时");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_SESSION), "P95 样本耗时");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_STREAM), "P95 样本耗时");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_TREND_SESSION_DURATION_MS), "活跃会话时长 (ms)");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_TREND_STREAM_DURATION_MS), "活跃流时长 (ms)");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_TREND_ACTIVE_WS), "会话数");

        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        assertEquals(en.getString(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_SESSION), "Avg Sample Duration");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_STREAM), "Avg Sample Duration");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_SESSION), "P95 Sample Duration");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_STREAM), "P95 Sample Duration");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_TREND_SESSION_DURATION_MS), "Active Session Duration (ms)");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_TREND_STREAM_DURATION_MS), "Active Stream Duration (ms)");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_TREND_ACTIVE_WS), "Sessions");
    }

    private static DefaultTableModel getReportTableModel(PerformanceReportPanel panel) throws Exception {
        Field field = PerformanceReportPanel.class.getDeclaredField("reportTableModel");
        field.setAccessible(true);
        return (DefaultTableModel) field.get(panel);
    }

    private static DefaultTableModel getSseReportTableModel(PerformanceReportPanel panel) throws Exception {
        Field field = PerformanceReportPanel.class.getDeclaredField("sseReportTableModel");
        field.setAccessible(true);
        return (DefaultTableModel) field.get(panel);
    }

    private static DefaultTableModel getWebSocketReportTableModel(PerformanceReportPanel panel) throws Exception {
        Field field = PerformanceReportPanel.class.getDeclaredField("webSocketReportTableModel");
        field.setAccessible(true);
        return (DefaultTableModel) field.get(panel);
    }

    private static JTable findTableByColumnCount(List<JTable> tables, int columnCount) {
        for (JTable table : tables) {
            if (table.getColumnCount() == columnCount) {
                return table;
            }
        }
        throw new AssertionError("Table not found for column count: " + columnCount);
    }

    private static boolean hasColumn(DefaultTableModel model, String columnName) {
        for (int i = 0; i < model.getColumnCount(); i++) {
            if (columnName.equals(model.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    private static String headerTooltipAt(JTable table, int column) {
        JTableHeader header = table.getTableHeader();
        MouseEvent event = new MouseEvent(
                header,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                header.getHeaderRect(column).x + 1,
                header.getHeaderRect(column).y + 1,
                0,
                false
        );
        return header.getToolTipText(event);
    }

    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> result = new ArrayList<>();
        collect(root, type, result);
        return result;
    }

    private static <T extends Component> void collect(Component component, Class<T> type, List<T> result) {
        if (type.isInstance(component)) {
            result.add(type.cast(component));
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, type, result);
            }
        }
    }
}
