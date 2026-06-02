package com.laker.postman.performance.core.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PerformanceStatsCollector {

    // 最终报表的唯一权威来源。趋势图和实时报告可以弱一致，但不能反向写入这里。
    private final ConcurrentMap<PerformanceProtocol, ConcurrentMap<String, PerformanceSampleMeterSet>> apiStatsByProtocol =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<PerformanceProtocol, PerformanceSampleMeterSet> protocolTotals = new ConcurrentHashMap<>();
    private final PerformanceSampleMeterSet overallStats = new PerformanceSampleMeterSet("", PerformanceProtocol.HTTP);

    public void record(RequestResult result) {
        if (result == null) {
            return;
        }
        PerformanceProtocol protocol = result.protocol == null ? PerformanceProtocol.HTTP : result.protocol;
        String apiId = result.apiId == null ? "" : result.apiId;
        ConcurrentMap<String, PerformanceSampleMeterSet> protocolApiStats =
                apiStatsByProtocol.computeIfAbsent(protocol, ignored -> new ConcurrentHashMap<>());

        protocolApiStats.computeIfAbsent(apiId, ignored -> new PerformanceSampleMeterSet(apiId, protocol)).record(result);
        protocolTotals.computeIfAbsent(protocol, ignored -> new PerformanceSampleMeterSet("", protocol)).record(result);
        overallStats.record(result);
    }

    public PerformanceStatsSnapshot snapshot() {
        // Meter 内部使用 LongAdder/CAS；运行中快照允许弱一致，压测完成后采集线程退出时收敛为最终准确值。
        List<PerformanceStatsSnapshot.ApiSummary> summaries = new ArrayList<>();
        for (Map<String, PerformanceSampleMeterSet> statsByApi : apiStatsByProtocol.values()) {
            for (PerformanceSampleMeterSet stats : statsByApi.values()) {
                summaries.add(stats.toSummary(stats.apiName()));
            }
        }

        EnumMap<PerformanceProtocol, PerformanceStatsSnapshot.ApiSummary> totals = new EnumMap<>(PerformanceProtocol.class);
        for (Map.Entry<PerformanceProtocol, PerformanceSampleMeterSet> entry : protocolTotals.entrySet()) {
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
