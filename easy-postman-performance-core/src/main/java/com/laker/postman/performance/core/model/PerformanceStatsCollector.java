package com.laker.postman.performance.core.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PerformanceStatsCollector {

    private final EnumMap<PerformanceProtocol, Map<String, PerformanceSampleAccumulator>> apiStatsByProtocol =
            new EnumMap<>(PerformanceProtocol.class);
    private final EnumMap<PerformanceProtocol, PerformanceSampleAccumulator> protocolTotals = new EnumMap<>(PerformanceProtocol.class);
    private final PerformanceSampleAccumulator overallStats = new PerformanceSampleAccumulator("", PerformanceProtocol.HTTP);

    public synchronized void record(RequestResult result) {
        if (result == null) {
            return;
        }
        PerformanceProtocol protocol = result.protocol == null ? PerformanceProtocol.HTTP : result.protocol;
        String apiId = result.apiId == null ? "" : result.apiId;
        Map<String, PerformanceSampleAccumulator> protocolApiStats =
                apiStatsByProtocol.computeIfAbsent(protocol, ignored -> new HashMap<>());

        protocolApiStats.computeIfAbsent(apiId, ignored -> new PerformanceSampleAccumulator(apiId, protocol)).record(result);
        protocolTotals.computeIfAbsent(protocol, ignored -> new PerformanceSampleAccumulator("", protocol)).record(result);
        overallStats.record(result);
    }

    public synchronized PerformanceStatsSnapshot snapshot() {
        List<PerformanceStatsSnapshot.ApiSummary> summaries = new ArrayList<>();
        for (Map<String, PerformanceSampleAccumulator> statsByApi : apiStatsByProtocol.values()) {
            for (PerformanceSampleAccumulator stats : statsByApi.values()) {
                summaries.add(stats.toSummary(stats.apiName()));
            }
        }

        EnumMap<PerformanceProtocol, PerformanceStatsSnapshot.ApiSummary> totals = new EnumMap<>(PerformanceProtocol.class);
        for (Map.Entry<PerformanceProtocol, PerformanceSampleAccumulator> entry : protocolTotals.entrySet()) {
            totals.put(entry.getKey(), entry.getValue().toSummary(""));
        }

        return new PerformanceStatsSnapshot(
                summaries,
                totals,
                overallStats.total(),
                overallStats.success(),
                0
        );
    }

    public synchronized void clear() {
        apiStatsByProtocol.clear();
        protocolTotals.clear();
        overallStats.clear();
    }
}
