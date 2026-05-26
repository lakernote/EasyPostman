package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SseSampleExecutorTest {
    private static final long SESSION_END_DELAY_MS = 220;

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
            assertFalse(result.response.headers.containsKey("X-Easy-SSE-Completion-Reason"));
            assertEquals(result.response.headers.get("X-Easy-SSE-Message-Count").get(0), "1");
            String firstEventLatency = result.response.headers.get("X-Easy-SSE-First-Event-Latency-Ms").get(0);
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
            assertFalse(result.response.headers.containsKey("X-Easy-SSE-Completion-Reason"));
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
    public void shouldSkipSseResponseBodyRetentionWhenDisabled() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("event: progress\n"
                            + "data: loading\n\n"));
            server.start();

            PreparedRequest request = new PreparedRequest();
            request.method = "GET";
            request.url = server.url("/stream").toString();
            request.headersList = List.of(new HttpHeader(true, "Accept", "text/event-stream"));

            SsePerformanceData cfg = new SsePerformanceData();
            cfg.completionMode = SsePerformanceData.CompletionMode.FIRST_MESSAGE;
            cfg.connectTimeoutMs = 2000;
            cfg.firstMessageTimeoutMs = 2000;

            SseSampleExecutor.Result result = new SseSampleExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet(),
                    new PerformanceRealtimeMetrics(),
                    1024,
                    false
            ).execute(request, cfg);

            assertFalse(result.executionFailed, result.errorMsg);
            assertEquals(result.response.headers.get("X-Easy-SSE-Event-Count").get(0), "1");
            assertEquals(result.response.body, "");
            assertEquals(result.response.bodySize, 0);
        }
    }

    @Test
    public void shouldFailMessageCountModeWhenStreamClosesBeforeTargetCount() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("event: progress\n"
                            + "data: loading\n\n"));
            server.start();

            PreparedRequest request = new PreparedRequest();
            request.method = "GET";
            request.url = server.url("/stream").toString();
            request.headersList = List.of(new HttpHeader(true, "Accept", "text/event-stream"));

            SsePerformanceData cfg = new SsePerformanceData();
            cfg.completionMode = SsePerformanceData.CompletionMode.MESSAGE_COUNT;
            cfg.connectTimeoutMs = 2000;
            cfg.firstMessageTimeoutMs = 2000;
            cfg.holdConnectionMs = 2000;
            cfg.targetMessageCount = 3;

            SseSampleExecutor.Result result = new SseSampleExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet()
            ).execute(request, cfg);

            assertTrue(result.executionFailed);
            assertEquals(result.response.headers.get("X-Easy-SSE-Message-Count").get(0), "1");
            assertTrue(result.errorMsg.contains("closed"), result.errorMsg);
            assertEquals(result.response.headers.get("X-Easy-SSE-Error").get(0), result.errorMsg);
        }
    }

    @Test
    public void shouldExcludeSseCloseCleanupFromReportedCost() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("event: progress\n"
                            + "data: loading\n\n"));
            server.start();

            PreparedRequest request = new PreparedRequest();
            request.method = "GET";
            request.url = server.url("/stream").toString();
            request.headersList = List.of(new HttpHeader(true, "Accept", "text/event-stream"));

            SsePerformanceData cfg = new SsePerformanceData();
            cfg.completionMode = SsePerformanceData.CompletionMode.FIRST_MESSAGE;
            cfg.connectTimeoutMs = 2000;
            cfg.firstMessageTimeoutMs = 2000;

            long wallStart = System.currentTimeMillis();
            SseSampleExecutor.Result result = new SseSampleExecutor(
                    () -> true,
                    throwable -> false,
                    ConcurrentHashMap.newKeySet(),
                    new SlowSseSessionEndMetrics()
            ).execute(request, cfg);
            long wallElapsed = System.currentTimeMillis() - wallStart;

            assertFalse(result.executionFailed, result.errorMsg);
            assertTrue(wallElapsed - result.response.costMs >= SESSION_END_DELAY_MS - 50,
                    "reported cost should exclude close cleanup delay, wallElapsed="
                            + wallElapsed + ", costMs=" + result.response.costMs);
        }
    }

    @Test
    public void shouldIgnoreEventFilterForFixedDurationMode() throws Exception {
        SsePerformanceData cfg = new SsePerformanceData();
        cfg.completionMode = SsePerformanceData.CompletionMode.FIXED_DURATION;
        cfg.eventNameFilter = "done";

        assertTrue(SseSampleMatcher.matchesEvent(cfg, "message"));
    }

    @Test
    public void shouldFormatSseEventBodyWithMultilineData() {
        BoundedTextAccumulator buffer = new BoundedTextAccumulator(1024);

        SseEventFormatter.appendEvent(buffer, "42", "done", "a\nb\r\nc");

        assertEquals(buffer.value(), "id: 42\nevent: done\ndata: a\ndata: b\ndata: c\n\n");
    }

    @Test
    public void shouldAddSseSummaryHeaders() {
        HttpResponse response = new HttpResponse();
        SsePerformanceData cfg = new SsePerformanceData();
        cfg.completionMode = SsePerformanceData.CompletionMode.MATCHED_MESSAGE;
        cfg.eventNameFilter = "done";
        cfg.messageFilter = "status";

        SseSampleResponseBuilder.addSummaryHeaders(
                response,
                cfg,
                5,
                2,
                31,
                "event-1",
                "done",
                "boom"
        );

        assertEquals(response.headers.get("X-Easy-SSE-Mode").get(0), "MATCHED_MESSAGE");
        assertEquals(response.headers.get("X-Easy-SSE-Event-Filter").get(0), "done");
        assertEquals(response.headers.get("X-Easy-SSE-Message-Filter").get(0), "status");
        assertEquals(response.headers.get("X-Easy-SSE-Event-Count").get(0), "5");
        assertEquals(response.headers.get("X-Easy-SSE-Message-Count").get(0), "2");
        assertEquals(response.headers.get("X-Easy-SSE-First-Event-Latency-Ms").get(0), "31");
        assertEquals(response.headers.get("X-Easy-SSE-Event-Id").get(0), "event-1");
        assertEquals(response.headers.get("X-Easy-SSE-Event-Type").get(0), "done");
        assertEquals(response.headers.get("X-Easy-SSE-Error").get(0), "boom");
    }

    private static final class SlowSseSessionEndMetrics extends PerformanceRealtimeMetrics {
        @Override
        public void recordSseSessionEnd(Object session) {
            sleepSessionEndDelay();
            super.recordSseSessionEnd(session);
        }
    }

    private static void sleepSessionEndDelay() {
        try {
            Thread.sleep(SESSION_END_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
