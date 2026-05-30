package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.RequestResult;
import com.laker.postman.util.JsonUtil;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceJsonReportMapperTest {

    @Test
    public void shouldMapStatsSnapshotToMachineReadableJsonReport() {
        PerformanceStatsCollector collector = new PerformanceStatsCollector();
        collector.record(new RequestResult(1_000, 1_100, true, "login", "Login", PerformanceProtocol.HTTP));
        collector.record(new RequestResult(2_000, 2_250, false, "login", "Login", PerformanceProtocol.HTTP));
        RequestResult stream = new RequestResult(3_000, 6_000, true, "events", "Events", PerformanceProtocol.SSE);
        stream.receivedMessages = 8;
        stream.matchedMessages = 7;
        stream.firstMessageLatencyMs = 120;
        collector.record(stream);

        PerformanceJsonReport report = PerformanceJsonReportMapper.fromStatsSnapshot(
                PerformanceJsonReportMetadata.builder()
                        .runId("run-1")
                        .source("local")
                        .status("FAILED")
                        .planPath("/tmp/plan.json")
                        .startTimeMs(1_000L)
                        .endTimeMs(6_000L)
                        .build(),
                collector.snapshot()
        );
        String json = new PerformanceJsonReportJsonStorage().toJson(report);
        Map<String, Object> root = objectMap(JsonUtil.convertValue(JsonUtil.readTree(json), Map.class));

        assertEquals(root.get("schemaVersion"), PerformanceJsonReportJsonStorage.FORMAT_VERSION);
        assertEquals(root.get("runId"), "run-1");
        assertEquals(root.get("source"), "local");
        assertEquals(objectMap(root.get("summary")).get("totalRequests"), 3);
        assertEquals(objectMap(root.get("summary")).get("failedRequests"), 1);

        Map<String, Object> protocols = objectMap(root.get("protocols"));
        Map<String, Object> http = objectMap(protocols.get("HTTP"));
        Map<String, Object> httpTotal = objectMap(http.get("total"));
        assertEquals(httpTotal.get("total"), 2);
        assertEquals(httpTotal.get("failed"), 1);
        assertTrue((Double) httpTotal.get("successRate") < 100.0);

        Map<String, Object> login = objectMap(listValue(http.get("apis")).get(0));
        assertEquals(login.get("apiId"), "login");
        assertEquals(login.get("name"), "Login");
        assertEquals(objectMap(login.get("durationMs")).get("p95"), 250);

        Map<String, Object> sse = objectMap(protocols.get("SSE"));
        Map<String, Object> sseApi = objectMap(listValue(sse.get("apis")).get(0));
        assertEquals(objectMap(sseApi.get("stream")).get("receivedMessages"), 8);
        assertEquals(objectMap(sseApi.get("firstMessageLatencyMs")).get("avg"), 120);
    }

    @Test
    public void shouldMergeWorkerReportsWithoutDroppingProtocolDetails() {
        PerformanceStatsCollector left = new PerformanceStatsCollector();
        left.record(new RequestResult(1_000, 1_100, true, "login", "Login", PerformanceProtocol.HTTP));
        PerformanceStatsCollector right = new PerformanceStatsCollector();
        right.record(new RequestResult(2_000, 2_300, false, "login", "Login", PerformanceProtocol.HTTP));
        PerformanceJsonReport leftReport = PerformanceJsonReportMapper.fromStatsSnapshot(
                PerformanceJsonReportMetadata.builder().source("worker-a").status("SUCCESS").build(),
                left.snapshot()
        );
        PerformanceJsonReport rightReport = PerformanceJsonReportMapper.fromStatsSnapshot(
                PerformanceJsonReportMetadata.builder().source("worker-b").status("FAILED").build(),
                right.snapshot()
        );

        PerformanceJsonReport merged = PerformanceJsonReportSummaryMapper.merge(
                "run-1",
                "master",
                "FAILED",
                "/tmp/plan.json",
                List.of(leftReport, rightReport)
        );

        assertEquals(merged.getSummary().getTotalRequests(), 2L);
        assertEquals(merged.getProtocols().get("HTTP").getTotal().getTotal(), 2L);
        assertEquals(merged.getProtocols().get("HTTP").getTotal().getFailed(), 1L);
        assertEquals(merged.getProtocols().get("HTTP").getTotal().getSuccessRate(), 50.0);
        assertEquals(merged.getProtocols().get("HTTP").getApis().get(0).getApiId(), "login");
        assertEquals(merged.getProtocols().get("HTTP").getApis().get(0).getTotal(), 2L);
        assertEquals(merged.getProtocols().get("HTTP").getApis().get(0).getSuccessRate(), 50.0);
        assertEquals(merged.getProtocols().get("HTTP").getApis().get(0).getDurationMs().getMax(), 300L);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> listValue(Object value) {
        return (List<Object>) value;
    }
}
