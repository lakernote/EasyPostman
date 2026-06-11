package com.laker.postman.performance.execution;

import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.http.request.PreparedRequestFactory;
import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class HttpSamplerExecutorTest {

    @BeforeMethod
    public void isolateRuntimeSettings() {
        HttpRuntimeSettingsProvider.reset();
        OkHttpClientManager.clearClientCache();
    }

    @AfterMethod
    public void tearDownRuntimeSettings() {
        OkHttpClientManager.clearClientCache();
        HttpRuntimeSettingsProvider.reset();
    }

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
            PreparedRequest request = PreparedRequestFactory.build(item);
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
        private final Set<RealtimeConnectionHandle> sseSources = ConcurrentHashMap.newKeySet();
        private final Set<RealtimeWebSocketConnection> webSockets = ConcurrentHashMap.newKeySet();

        @Override
        public void onCallStarted(Call call) {
            started.incrementAndGet();
        }

        @Override
        public void onCallFinished(Call call) {
            finished.incrementAndGet();
        }

        @Override
        public Set<RealtimeConnectionHandle> activeSseSources() {
            return sseSources;
        }

        @Override
        public Set<RealtimeWebSocketConnection> activeWebSockets() {
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
