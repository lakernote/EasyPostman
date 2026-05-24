package com.laker.postman.panel.performance.report;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceReportMarkdownBuilderTest {

    @Test
    public void shouldBuildMarkdownFromReportTablesAndEscapeCells() {
        PerformanceReportMarkdownBuilder.ReportTable table = new PerformanceReportMarkdownBuilder.ReportTable(
                "HTTP",
                List.of("API", "QPS"),
                Collections.singletonList(new Object[]{"Search|API", "1.00\n2.00"})
        );

        String markdown = PerformanceReportMarkdownBuilder.build("Load Test Report", "No data", List.of(table));

        assertEquals(markdown, """
                # Load Test Report

                ## HTTP

                | API | QPS |
                | --- | --- |
                | Search\\|API | 1.00 2.00 |

                """);
    }

    @Test
    public void shouldReturnEmptyTextWhenAllTablesAreEmpty() {
        PerformanceReportMarkdownBuilder.ReportTable emptyTable = new PerformanceReportMarkdownBuilder.ReportTable(
                "HTTP",
                List.of("API"),
                List.of()
        );

        assertEquals(PerformanceReportMarkdownBuilder.build("Report", "No data", List.of(emptyTable)), "No data");
    }
}
