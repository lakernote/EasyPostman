package com.laker.postman.panel.performance.model;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.TestResult;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceSampleResultTest {

    @Test
    public void shouldConvertExecutionResultIntoProtocolNeutralSampleResult() {
        HttpResponse response = new HttpResponse();
        response.headers = new LinkedHashMap<>();
        response.code = 101;
        response.costMs = 2500;
        response.endTime = 3500;
        response.addHeader("X-Easy-WS-Sent-Count", List.of("3"));
        response.addHeader("X-Easy-WS-Received-Count", List.of("5"));
        response.addHeader("X-Easy-WS-Message-Count", List.of("4"));
        response.addHeader("X-Easy-WS-First-Message-Latency-Ms", List.of("88"));

        PerformanceRequestExecutionResult executionResult = new PerformanceRequestExecutionResult(
                "api-ws",
                "WS API",
                new PreparedRequest(),
                response,
                "",
                List.of(new TestResult("status", true, null)),
                false,
                false,
                PerformanceProtocol.WEBSOCKET,
                1000L,
                0L
        );

        PerformanceSampleResult sampleResult = PerformanceSampleResult.fromExecutionResult(executionResult);

        assertEquals(sampleResult.getApiId(), "api-ws");
        assertEquals(sampleResult.getApiName(), "WS API");
        assertEquals(sampleResult.getProtocol(), PerformanceProtocol.WEBSOCKET);
        assertEquals(sampleResult.getStartTimeMs(), 1000L);
        assertEquals(sampleResult.getEndTimeMs(), 3500L);
        assertEquals(sampleResult.getElapsedTimeMs(), 2500L);
        assertEquals(sampleResult.getResponseCode(), 101);
        assertEquals(sampleResult.getSentMessages(), 3);
        assertEquals(sampleResult.getReceivedMessages(), 5);
        assertEquals(sampleResult.getMatchedMessages(), 4);
        assertEquals(sampleResult.getFirstMessageLatencyMs(), 88L);
        assertTrue(sampleResult.isSuccessful());
        assertFalse(sampleResult.isExecutionFailed());
    }

    @Test
    public void shouldMapHttpByteMetricsIntoCoreSampleRecord() {
        HttpEventInfo eventInfo = new HttpEventInfo();
        eventInfo.setHeaderBytesSent(120L);
        eventInfo.setBodyBytesSent(380L);
        eventInfo.setHeaderBytesReceived(240L);
        eventInfo.setBodyBytesReceived(760L);
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.costMs = 20;
        response.endTime = 120;
        response.httpEventInfo = eventInfo;

        PerformanceRequestExecutionResult executionResult = new PerformanceRequestExecutionResult(
                "api",
                "API",
                new PreparedRequest(),
                response,
                "",
                List.of(),
                false,
                false,
                PerformanceProtocol.HTTP,
                100L,
                0L
        );

        PerformanceSampleResult sampleResult = PerformanceSampleResult.fromExecutionResult(executionResult);

        assertEquals(sampleResult.getSentBytes(), 500L);
        assertEquals(sampleResult.getReceivedBytes(), 1_000L);
        assertEquals(sampleResult.toRequestResult().sentBytes, 500L);
        assertEquals(sampleResult.toRequestResult().receivedBytes, 1_000L);
    }

    @Test
    public void shouldExposeFailedAssertionAsFailedSample() {
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.costMs = 10;
        response.endTime = 110;

        PerformanceRequestExecutionResult executionResult = new PerformanceRequestExecutionResult(
                "api",
                "API",
                new PreparedRequest(),
                response,
                "",
                List.of(new TestResult("contains", false, "missing text")),
                false,
                false,
                PerformanceProtocol.HTTP,
                100L,
                0L
        );

        PerformanceSampleResult sampleResult = PerformanceSampleResult.fromExecutionResult(executionResult);

        assertFalse(sampleResult.isSuccessful());
        assertEquals(sampleResult.getAssertionResults().size(), 1);
        assertFalse(sampleResult.getAssertionResults().get(0).isPassed());
    }

    @Test
    public void shouldTreatInterruptedResponseAsFailedSample() {
        HttpResponse response = new HttpResponse();
        response.code = 101;
        response.costMs = 60_000;
        response.endTime = 61_000;

        PerformanceRequestExecutionResult executionResult = new PerformanceRequestExecutionResult(
                "api-ws",
                "WS API",
                new PreparedRequest(),
                response,
                "",
                List.of(),
                false,
                true,
                PerformanceProtocol.WEBSOCKET,
                1000L,
                0L
        );

        PerformanceSampleResult sampleResult = PerformanceSampleResult.fromExecutionResult(executionResult);

        assertTrue(sampleResult.isInterrupted());
        assertFalse(sampleResult.isSuccessful());
    }
}
