package com.laker.postman.panel.performance.result;

import com.laker.postman.panel.performance.model.ApiMetadata;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.model.RequestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceProtocolReportDataTest {

    @AfterMethod
    public void tearDown() {
        ApiMetadata.clear();
    }

    @Test
    public void shouldBuildSeparateProtocolRows() {
        ApiMetadata.register("http-api", "HTTP API");
        ApiMetadata.register("ws-api", "WS API");
        ApiMetadata.register("sse-api", "SSE API");

        RequestResult http = new RequestResult(1_000, 1_100, true, "http-api", PerformanceProtocol.HTTP);
        RequestResult ws = new RequestResult(2_000, 2_500, true, "ws-api", PerformanceProtocol.WEBSOCKET);
        ws.sentMessages = 2;
        ws.receivedMessages = 5;
        ws.matchedMessages = 3;
        ws.firstMessageLatencyMs = 90;
        ws.completionReason = "matched_message";
        RequestResult sse = new RequestResult(3_000, 4_000, false, "sse-api", PerformanceProtocol.SSE);
        sse.receivedMessages = 7;
        sse.matchedMessages = 4;
        sse.firstMessageLatencyMs = 140;
        sse.completionReason = "message_target_timeout";

        PerformanceProtocolReportData reportData = PerformanceProtocolReportData.fromResults(
                List.of(http, ws, sse),
                "Total"
        );

        assertEquals(reportData.httpRows().get(0).name(), "HTTP API");
        assertEquals(reportData.httpRows().get(0).qps(), 10.0);
        assertEquals(reportData.webSocketRows().get(0).name(), "WS API");
        assertEquals(reportData.webSocketRows().get(0).sentMessages(), 2);
        assertEquals(reportData.webSocketRows().get(0).receivedMessages(), 5);
        assertEquals(reportData.webSocketRows().get(0).topCompletionReason(), "matched_message");
        assertEquals(reportData.sseRows().get(0).name(), "SSE API");
        assertEquals(reportData.sseRows().get(0).receivedMessages(), 7);
        assertEquals(reportData.sseRows().get(0).matchedMessages(), 4);
        assertEquals(reportData.sseRows().get(0).topCompletionReason(), "message_target_timeout");
    }

    @Test
    public void shouldBuildRowsFromAggregatedStatsSnapshot() {
        ApiMetadata.register("http-api", "HTTP API");
        PerformanceStatsCollector collector = new PerformanceStatsCollector();
        collector.record(new RequestResult(1_000, 1_100, true, "http-api", PerformanceProtocol.HTTP));
        collector.record(new RequestResult(1_100, 1_300, false, "http-api", PerformanceProtocol.HTTP));

        PerformanceProtocolReportData reportData = PerformanceProtocolReportData.fromStatsSnapshot(
                collector.snapshot(),
                "Total"
        );

        assertEquals(reportData.httpRows().get(0).name(), "HTTP API");
        assertEquals(reportData.httpRows().get(0).total(), 2L);
        assertEquals(reportData.httpRows().get(0).success(), 1L);
        assertEquals(reportData.httpRows().get(0).fail(), 1L);
        assertEquals(reportData.httpRows().get(1).name(), "Total");
        assertEquals(reportData.httpRows().get(1).total(), 2L);
    }
}
