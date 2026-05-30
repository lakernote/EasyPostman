package com.laker.postman.performance.core.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PerformanceStatsCollector {

    private final ConcurrentMap<PerformanceProtocol, ConcurrentMap<String, PerformanceSampleAccumulator>> apiStatsByProtocol =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<PerformanceProtocol, PerformanceSampleAccumulator> protocolTotals = new ConcurrentHashMap<>();
    private final PerformanceSampleAccumulator overallStats = new PerformanceSampleAccumulator("", PerformanceProtocol.HTTP);

    public void record(RequestResult result) {
        if (result == null) {
            return;
        }
        PerformanceProtocol protocol = result.protocol == null ? PerformanceProtocol.HTTP : result.protocol;
        String apiId = result.apiId == null ? "" : result.apiId;
        ConcurrentMap<String, PerformanceSampleAccumulator> protocolApiStats =
                apiStatsByProtocol.computeIfAbsent(protocol, ignored -> new ConcurrentHashMap<>());

        protocolApiStats.computeIfAbsent(apiId, ignored -> new PerformanceSampleAccumulator(apiId, protocol)).record(result);
        protocolTotals.computeIfAbsent(protocol, ignored -> new PerformanceSampleAccumulator("", protocol)).record(result);
        overallStats.record(result);
    }

    public PerformanceStatsSnapshot snapshot() {
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
        PerformanceStatsSnapshot.ApiSummary overallSummary = overallStats.toSummary("");

        return new PerformanceStatsSnapshot(
                summaries,
                totals,
                overallSummary.total(),
                overallSummary.success(),
                0
        );
    }

    public PerformanceStatsProgressSnapshot progressSnapshot() {
        return overallStats.toProgressSnapshot();
    }

    public void clear() {
        apiStatsByProtocol.clear();
        protocolTotals.clear();
        overallStats.clear();
    }
}
