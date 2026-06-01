package com.laker.postman.performance.runtime;

import com.laker.postman.performance.core.runtime.PerformanceRunListener;
import com.laker.postman.performance.execution.PerformanceExecutionConfig;
import com.laker.postman.performance.execution.PerformanceNetworkRuntime;
import com.laker.postman.performance.result.PerformanceResultCollector;
import com.laker.postman.http.runtime.model.PreparedRequest;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import org.testng.annotations.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertTrue;

public class PerformanceExecutionEngineNetworkRuntimeTest {

    @Test
    public void cancelAllNetworkCallsShouldDelegateToInjectedRunNetworkRuntime() {
        RecordingNetworkRuntime networkRuntime = new RecordingNetworkRuntime();
        PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                () -> true,
                PerformanceExecutionConfig.DEFAULT,
                new PerformanceResultCollector(PerformanceResultSink.NOOP),
                PerformanceRunListener.NOOP,
                networkRuntime
        );

        engine.cancelAllNetworkCalls();

        assertTrue(networkRuntime.cancelled.get());
    }

    private static final class RecordingNetworkRuntime implements PerformanceNetworkRuntime {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final Set<RealtimeConnectionHandle> sseSources = ConcurrentHashMap.newKeySet();
        private final Set<RealtimeWebSocketConnection> webSockets = ConcurrentHashMap.newKeySet();

        @Override
        public void onCallStarted(Call call) {
        }

        @Override
        public void onCallFinished(Call call) {
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
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public int activeHttpCallCount() {
            return 0;
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
            cancelled.set(true);
        }
    }
}
