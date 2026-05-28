package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.http.PreparedRequestBuilder;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.sse.EventSource;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class HttpSamplerExecutorTest {

    @Test
    public void executeShouldTrackHttpCallsWithRunNetworkRuntime() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("ok"));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("tracked-http-call");
            item.setName("Tracked HTTP Call");
            item.setMethod("GET");
            item.setUrl(server.url("/tracked").toString());
            PreparedRequest request = PreparedRequestBuilder.build(item);
            RecordingNetworkRuntime networkRuntime = new RecordingNetworkRuntime();
            PerformanceRequestSampler sampler = new PerformanceRequestSampler(item.getName(), item, null, List.of());

            ProtocolExecutionResult result = new HttpSamplerExecutor(networkRuntime).execute(
                    new PerformanceProtocolSamplerContext(
                            request,
                            sampler,
                            sampler.getRequestSnapshot(),
                            request.body,
                            null,
                            PerformanceResponseCapturePlan.resolve(false, null, false, false, "")
                    )
            );

            assertFalse(result.executionFailed(), result.errorMsg());
            assertEquals(server.getRequestCount(), 1);
            assertEquals(networkRuntime.started.get(), 1);
            assertEquals(networkRuntime.finished.get(), 1);
        }
    }

    private static final class RecordingNetworkRuntime implements PerformanceNetworkRuntime {
        private final AtomicInteger started = new AtomicInteger();
        private final AtomicInteger finished = new AtomicInteger();
        private final Set<EventSource> sseSources = ConcurrentHashMap.newKeySet();
        private final Set<WebSocket> webSockets = ConcurrentHashMap.newKeySet();

        @Override
        public void onCallStarted(Call call) {
            started.incrementAndGet();
        }

        @Override
        public void onCallFinished(Call call) {
            finished.incrementAndGet();
        }

        @Override
        public Set<EventSource> activeSseSources() {
            return sseSources;
        }

        @Override
        public Set<WebSocket> activeWebSockets() {
            return webSockets;
        }

        @Override
        public OkHttpClient getBaseClient(PreparedRequest request) {
            return new DefaultPerformanceNetworkRuntime().getBaseClient(request);
        }

        @Override
        public int activeHttpCallCount() {
            return started.get() - finished.get();
        }

        @Override
        public int activeSseCount() {
            return sseSources.size();
        }

        @Override
        public int activeWebSocketCount() {
            return webSockets.size();
        }

        @Override
        public void cancelAll() {
        }
    }
}
