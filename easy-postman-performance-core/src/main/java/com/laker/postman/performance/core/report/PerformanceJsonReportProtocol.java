package com.laker.postman.performance.core.report;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
public class PerformanceJsonReportProtocol {
    String protocol;
    PerformanceJsonReportApi total;
    List<PerformanceJsonReportApi> apis;

    @Builder
    public PerformanceJsonReportProtocol(String protocol,
                                         PerformanceJsonReportApi total,
                                         List<PerformanceJsonReportApi> apis) {
        this.protocol = protocol == null ? "" : protocol;
        this.total = total == null
                ? PerformanceJsonReportApi.builder().protocol(this.protocol).build()
                : total;
        this.apis = copyApis(apis);
    }

    private static List<PerformanceJsonReportApi> copyApis(List<PerformanceJsonReportApi> apis) {
        List<PerformanceJsonReportApi> copy = new ArrayList<>();
        if (apis == null) {
            return List.of();
        }
        for (PerformanceJsonReportApi api : apis) {
            if (api != null) {
                copy.add(api);
            }
        }
        return List.copyOf(copy);
    }
}
