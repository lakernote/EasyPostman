package com.laker.postman.panel.performance.result;

import com.laker.postman.panel.performance.model.ApiMetadata;
import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
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

    private static DefaultTableModel getReportTableModel(PerformanceReportPanel panel) throws Exception {
        Field field = PerformanceReportPanel.class.getDeclaredField("reportTableModel");
        field.setAccessible(true);
        return (DefaultTableModel) field.get(panel);
    }
}
