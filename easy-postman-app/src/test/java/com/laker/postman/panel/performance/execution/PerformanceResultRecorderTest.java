package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.model.PerformanceStatsSnapshot;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import org.testng.annotations.Test;

import java.util.List;
import java.util.LinkedHashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceResultRecorderTest {

    @Test
    public void shouldOnlyRecordSlowOrFailedResultsInEfficientMode() {
        assertFalse(PerformanceResultRecorder.shouldRecordResult(true, true, 1200, 3000));
        assertTrue(PerformanceResultRecorder.shouldRecordResult(true, true, 3000, 3000));
        assertTrue(PerformanceResultRecorder.shouldRecordResult(true, false, 100, 3000));
        assertTrue(PerformanceResultRecorder.shouldRecordResult(false, true, 100, 3000));
        assertFalse(PerformanceResultRecorder.shouldRecordResult(true, true, 5000, 0));
    }

    @Test
    public void shouldExtractWebSocketStreamMetricsFromResponseHeaders() {
        HttpResponse response = new HttpResponse();
        response.headers = new LinkedHashMap<>();
        response.costMs = 5000;
        response.endTime = 6000;
        response.addHeader("X-Easy-WS-Sent-Count", List.of("2"));
        response.addHeader("X-Easy-WS-Received-Count", List.of("4"));
        response.addHeader("X-Easy-WS-Message-Count", List.of("3"));
        response.addHeader("X-Easy-WS-First-Message-Latency-Ms", List.of("120"));
        response.addHeader("X-Easy-WS-Completion-Reason", List.of("matched_message"));

        PerformanceRequestExecutionResult executionResult = new PerformanceRequestExecutionResult(
                "api-ws",
                "WS API",
                new PreparedRequest(),
                response,
                "",
                List.of(),
                false,
                false,
                PerformanceProtocol.WEBSOCKET,
                1000L,
                0L
        );

        RequestResult result = PerformanceResultRecorder.toRequestResult(executionResult, 5000, 6000, true);

        assertEquals(result.protocol, PerformanceProtocol.WEBSOCKET);
        assertEquals(result.sentMessages, 2);
        assertEquals(result.receivedMessages, 4);
        assertEquals(result.matchedMessages, 3);
        assertEquals(result.firstMessageLatencyMs, 120L);
        assertEquals(result.completionReason, "matched_message");
    }

    @Test
    public void shouldExtractSseFirstEventLatencyFromResponseHeaders() {
        HttpResponse response = new HttpResponse();
        response.headers = new LinkedHashMap<>();
        response.costMs = 5000;
        response.endTime = 6000;
        response.addHeader("X-Easy-SSE-Event-Count", List.of("4"));
        response.addHeader("X-Easy-SSE-Message-Count", List.of("1"));
        response.addHeader("X-Easy-SSE-First-Event-Latency-Ms", List.of("90"));
        response.addHeader("X-Easy-SSE-Completion-Reason", List.of("first_message"));

        PerformanceRequestExecutionResult executionResult = new PerformanceRequestExecutionResult(
                "api-sse",
                "SSE API",
                new PreparedRequest(),
                response,
                "",
                List.of(),
                false,
                false,
                PerformanceProtocol.SSE,
                1000L,
                0L
        );

        RequestResult result = PerformanceResultRecorder.toRequestResult(executionResult, 5000, 6000, true);

        assertEquals(result.protocol, PerformanceProtocol.SSE);
        assertEquals(result.receivedMessages, 4);
        assertEquals(result.matchedMessages, 1);
        assertEquals(result.firstMessageLatencyMs, 90L);
        assertEquals(result.completionReason, "first_message");
    }

    @Test
    public void shouldAggregateFastSuccessesWithoutRecordingDetailsInEfficientMode() {
        PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
        RecordingResultTablePanel tablePanel = new RecordingResultTablePanel();
        try {
            PerformanceResultRecorder recorder = new PerformanceResultRecorder(
                    statsCollector,
                    tablePanel,
                    () -> 3_000
            );

            for (int i = 0; i < 10_000; i++) {
                HttpResponse response = new HttpResponse();
                response.code = 200;
                response.costMs = 100;
                response.endTime = i + 100L;

                recorder.record(new PerformanceRequestExecutionResult(
                        "api",
                        "API",
                        new PreparedRequest(),
                        response,
                        "",
                        List.of(),
                        false,
                        false,
                        PerformanceProtocol.HTTP,
                        i,
                        0
                ), true);
            }

            PerformanceStatsSnapshot snapshot = statsCollector.snapshot();

            assertEquals(snapshot.totalRequests(), 10_000L);
            assertEquals(snapshot.successRequests(), 10_000L);
            assertEquals(snapshot.retainedRequestResultCount(), 0L);
            assertEquals(tablePanel.recordedRows, 0);
        } finally {
            tablePanel.dispose();
        }
    }

    private static final class RecordingResultTablePanel extends PerformanceResultTablePanel {
        private int recordedRows;

        @Override
        public void addResult(ResultNodeInfo info) {
            recordedRows++;
        }
    }
}
