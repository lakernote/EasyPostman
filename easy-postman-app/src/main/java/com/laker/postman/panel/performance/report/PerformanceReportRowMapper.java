package com.laker.postman.panel.performance.report;

import com.laker.postman.util.TimeDisplayUtil;
import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
public class PerformanceReportRowMapper {

    public Object[] toHttpRowData(PerformanceProtocolReportData.HttpReportRow row) {
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

    public Object[] toWebSocketRowData(PerformanceProtocolReportData.StreamReportRow row) {
        return new Object[]{
                row.name(),
                row.total(),
                row.success(),
                row.fail(),
                formatPercent(row.successRate()),
                row.sentMessages(),
                row.receivedMessages(),
                row.matchedMessages(),
                formatDecimal(row.sendRate()),
                formatDecimal(row.receiveRate()),
                TimeDisplayUtil.formatElapsedTime(row.avgFirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.avgDurationMs()),
                TimeDisplayUtil.formatElapsedTime(row.p95DurationMs())
        };
    }

    public Object[] toSseRowData(PerformanceProtocolReportData.StreamReportRow row) {
        return new Object[]{
                row.name(),
                row.total(),
                row.success(),
                row.fail(),
                formatPercent(row.successRate()),
                row.receivedMessages(),
                row.matchedMessages(),
                formatDecimal(row.receiveRate()),
                formatDecimal(row.matchedRate()),
                TimeDisplayUtil.formatElapsedTime(row.avgFirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.p90FirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.p95FirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.p99FirstMessageLatencyMs()),
                TimeDisplayUtil.formatElapsedTime(row.avgDurationMs()),
                TimeDisplayUtil.formatElapsedTime(row.p95DurationMs())
        };
    }

    public String formatPercent(double value) {
        if (Double.isNaN(value)) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.2f%%", value);
    }

    public String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
