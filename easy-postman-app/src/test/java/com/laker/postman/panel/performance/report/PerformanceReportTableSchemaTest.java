package com.laker.postman.panel.performance.report;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceReportTableSchemaTest {

    @Test
    public void streamReportWidthsShouldMatchColumnCounts() {
        assertEquals(
                PerformanceReportTableSchema.webSocketColumnWidths().length,
                PerformanceReportTableSchema.webSocketColumns().length
        );
        assertEquals(
                PerformanceReportTableSchema.sseColumnWidths().length,
                PerformanceReportTableSchema.sseColumns().length
        );
    }

    @Test
    public void highlightedColumnIndexesShouldExistForAllReportTables() {
        assertHighlightedIndexesExist(PerformanceReportTableSchema.httpColumns());
        assertHighlightedIndexesExist(PerformanceReportTableSchema.webSocketColumns());
        assertHighlightedIndexesExist(PerformanceReportTableSchema.sseColumns());
    }

    private static void assertHighlightedIndexesExist(String[] columns) {
        assertTrue(PerformanceReportTableSchema.FAIL_COLUMN_INDEX < columns.length);
        assertTrue(PerformanceReportTableSchema.SUCCESS_RATE_COLUMN_INDEX < columns.length);
    }
}
