package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.RequestResult;
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
}
