package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SseSampleExecutorTest {

    @Test
    public void shouldFinishOnFirstSseMessageMatchingEventAndPayloadFilter() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("event: progress\n"
                            + "data: loading\n\n"
                            + "event: done\n"
                            + "data: {\"status\":\"done\"}\n\n"));
            server.start();

            PreparedRequest request = new PreparedRequest();
            request.method = "GET";
            request.url = server.url("/stream").toString();
            request.headersList = List.of(new HttpHeader(true, "Accept", "text/event-stream"));

            SsePerformanceData cfg = new SsePerformanceData();
            cfg.completionMode = SsePerformanceData.CompletionMode.MATCHED_MESSAGE;
            cfg.connectTimeoutMs = 2000;
            cfg.firstMessageTimeoutMs = 2000;
            cfg.eventNameFilter = "done";
            cfg.messageFilter = "status";

            PerformanceRealtimeMetrics realtimeMetrics = new PerformanceRealtimeMetrics();
            realtimeMetrics.reset(System.currentTimeMillis());
            SseSampleExecutor.Result result = new SseSampleExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet(),
                    realtimeMetrics
            ).execute(request, cfg);
            PerformanceRealtimeMetrics.Sample sample = realtimeMetrics.sample(System.currentTimeMillis());

            assertFalse(result.executionFailed, result.errorMsg);
            assertEquals(result.response.headers.get("X-Easy-SSE-Completion-Reason").get(0), "matched_message");
            assertEquals(result.response.headers.get("X-Easy-SSE-Message-Count").get(0), "1");
            String firstEventLatency = result.response.headers.get("X-Easy-SSE-First-Message-Latency-Ms").get(0);
            assertFalse(firstEventLatency.isBlank());
            assertTrue(Long.parseLong(firstEventLatency) >= 0);
            assertTrue(result.response.body.contains("event: done"), result.response.body);
            assertTrue(result.response.body.contains("status"), result.response.body.replace("\n", "\\n"));
            assertFalse(result.response.body.contains("loading"));
            assertTrue(sample.sseReceivedRate() > 0, "SSE received rate should be recorded in real time");
            assertTrue(sample.sseMatchedRate() > 0, "SSE matched rate should be recorded in real time");
        }
    }

    @Test
    public void shouldFinishOnPhysicalFirstSseEventIgnoringFiltersInFirstMessageMode() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("event: progress\n"
                            + "data: loading\n\n"
                            + "event: done\n"
                            + "data: {\"status\":\"done\"}\n\n"));
            server.start();

            PreparedRequest request = new PreparedRequest();
            request.method = "GET";
            request.url = server.url("/stream").toString();
            request.headersList = List.of(new HttpHeader(true, "Accept", "text/event-stream"));

            SsePerformanceData cfg = new SsePerformanceData();
            cfg.completionMode = SsePerformanceData.CompletionMode.FIRST_MESSAGE;
            cfg.connectTimeoutMs = 2000;
            cfg.firstMessageTimeoutMs = 2000;
            cfg.eventNameFilter = "done";
            cfg.messageFilter = "status";

            SseSampleExecutor.Result result = new SseSampleExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet()
            ).execute(request, cfg);

            assertFalse(result.executionFailed, result.errorMsg);
            assertEquals(result.response.headers.get("X-Easy-SSE-Completion-Reason").get(0), "first_message");
            assertEquals(result.response.headers.get("X-Easy-SSE-Message-Count").get(0), "1");
            String firstEventLatency = result.response.headers.get("X-Easy-SSE-First-Event-Latency-Ms").get(0);
            assertFalse(firstEventLatency.isBlank());
            assertTrue(Long.parseLong(firstEventLatency) >= 0);
            assertTrue(result.response.body.contains("event: progress"), result.response.body);
            assertTrue(result.response.body.contains("loading"), result.response.body.replace("\n", "\\n"));
            assertFalse(result.response.body.contains("event: done"), result.response.body);
        }
    }

    @Test
    public void shouldIgnoreEventFilterForFixedDurationMode() throws Exception {
        SsePerformanceData cfg = new SsePerformanceData();
        cfg.completionMode = SsePerformanceData.CompletionMode.FIXED_DURATION;
        cfg.eventNameFilter = "done";
        SseSampleExecutor executor = new SseSampleExecutor(
                () -> true,
                throwable -> false,
                ConcurrentHashMap.newKeySet()
        );

        Method matchesEvent = SseSampleExecutor.class.getDeclaredMethod("matchesEvent", SsePerformanceData.class, String.class);
        matchesEvent.setAccessible(true);

        assertTrue((Boolean) matchesEvent.invoke(executor, cfg, "message"));
    }
}
