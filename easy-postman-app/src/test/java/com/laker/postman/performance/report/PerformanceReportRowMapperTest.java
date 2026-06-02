package com.laker.postman.performance.report;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PerformanceReportRowMapperTest {

    @Test
    public void shouldFormatSseFirstEventPercentilesWithSharedElapsedTimeRules() {
        PerformanceProtocolReportData.StreamReportRow row = new PerformanceProtocolReportData.StreamReportRow(
                "Stream API",
                10,
                10,
                0,
                100.0,
                0,
                10,
                10,
                0.0,
                10.0,
                10.0,
                550,
                900,
                1_000,
                1_000,
                2_000,
                3_000
        );

        Object[] values = PerformanceReportRowMapper.toSseRowData(row);

        assertEquals(values[9], "550 ms");
        assertEquals(values[10], "900 ms");
        assertEquals(values[11], "1.00 s");
        assertEquals(values[12], "1.00 s");
    }
}
