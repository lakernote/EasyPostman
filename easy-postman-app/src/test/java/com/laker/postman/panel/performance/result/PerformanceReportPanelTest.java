package com.laker.postman.panel.performance.result;

import com.laker.postman.panel.performance.model.ApiMetadata;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceReportPanelTest extends AbstractSwingUiTest {

    @AfterMethod
    public void tearDown() {
        ApiMetadata.clear();
    }

    @Test
    public void shouldKeepQpsPrecisionToTwoDecimals() throws Exception {
        ApiMetadata.register("search", "Search API");
        List<RequestResult> results = List.of(
                new RequestResult(1_000, 1_100, true, "search"),
                new RequestResult(2_000, 2_100, true, "search"),
                new RequestResult(2_900, 3_000, true, "search")
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
        ApiMetadata.register("search", "Search API");
        List<RequestResult> results = List.of(
                new RequestResult(1_000, 1_100, true, "search"),
                new RequestResult(2_900, 3_000, true, "search")
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

        List<JToggleButton> protocolButtons = findAll(panel, JToggleButton.class);

        assertTrue(findAll(panel, JTabbedPane.class).isEmpty());
        assertEquals(protocolButtons.size(), PerformanceProtocol.values().length);
        assertTrue(protocolButtons.stream().anyMatch(button ->
                PerformanceProtocol.HTTP.getDisplayName().equals(button.getText()) && button.isSelected()));
        assertTrue(protocolButtons.stream().anyMatch(button ->
                PerformanceProtocol.WEBSOCKET.getDisplayName().equals(button.getText())));
        assertTrue(protocolButtons.stream().anyMatch(button ->
                PerformanceProtocol.SSE.getDisplayName().equals(button.getText())));
    }

    private static DefaultTableModel getReportTableModel(PerformanceReportPanel panel) throws Exception {
        Field field = PerformanceReportPanel.class.getDeclaredField("reportTableModel");
        field.setAccessible(true);
        return (DefaultTableModel) field.get(panel);
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
