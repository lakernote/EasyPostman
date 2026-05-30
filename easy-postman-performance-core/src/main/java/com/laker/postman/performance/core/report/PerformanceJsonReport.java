package com.laker.postman.performance.core.report;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Value
public class PerformanceJsonReport {
    PerformanceJsonReportMetadata metadata;
    PerformanceJsonReportSummary summary;
    Map<String, PerformanceJsonReportProtocol> protocols;

    @Builder
    public PerformanceJsonReport(PerformanceJsonReportMetadata metadata,
                                 PerformanceJsonReportSummary summary,
                                 Map<String, PerformanceJsonReportProtocol> protocols) {
        this.metadata = metadata == null ? PerformanceJsonReportMetadata.builder().build() : metadata;
        this.summary = summary == null ? PerformanceJsonReportSummary.builder().build() : summary;
        this.protocols = copyProtocols(protocols);
    }

    private static Map<String, PerformanceJsonReportProtocol> copyProtocols(
            Map<String, PerformanceJsonReportProtocol> protocols) {
        Map<String, PerformanceJsonReportProtocol> copy = new LinkedHashMap<>();
        if (protocols == null) {
            return Collections.unmodifiableMap(copy);
        }
        for (Map.Entry<String, PerformanceJsonReportProtocol> entry : protocols.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(copy);
    }
}
