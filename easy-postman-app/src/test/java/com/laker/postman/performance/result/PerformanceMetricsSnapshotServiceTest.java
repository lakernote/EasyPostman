package com.laker.postman.performance.result;

import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceReportSnapshot;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.model.PerformanceTrendWindowCollector;
import com.laker.postman.performance.core.model.RequestResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class PerformanceMetricsSnapshotServiceTest {

    @Test
    public void reportSnapshotShouldNormalizeMissingInputs() {
        PerformanceMetricsSnapshotService service = new PerformanceMetricsSnapshotService(
                null,
                null,
                () -> 0,
                () -> 0,
                () -> 0,
                () -> 1000L,
                now -> null,
                now -> null
        );

        PerformanceReportSnapshot snapshot = service.reportSnapshot(1234);

        assertEquals(snapshot.completedStats().totalRequests(), 0);
        assertEquals(snapshot.liveSnapshot().webSocket().activeSessions(), 0);
        assertEquals(snapshot.liveSnapshot().sse().activeSessions(), 0);
    }

    @Test
    public void drainTrendWindowSnapshotShouldUseRealtimePeakSessionCountsOverActiveSuppliers() {
        PerformanceRealtimeMetrics.Sample sample = new PerformanceRealtimeMetrics.Sample(
                1.0,
                2.0,
                3.0,
                4.0,
                5,
                6.0,
                7.0,
                8.0,
                9.0,
                4,
                10.0
        );
        PerformanceMetricsSnapshotService service = new PerformanceMetricsSnapshotService(
                new PerformanceStatsCollector(),
                new PerformanceTrendWindowCollector(),
                () -> 2,
                () -> 1,
                () -> 3,
                () -> 1000L,
                now -> sample,
                now -> PerformanceRealtimeMetrics.LiveSnapshot.empty()
        );

        PerformanceTrendSnapshot snapshot = service.drainTrendWindowSnapshot(2000);

        assertEquals(snapshot.activeUsers(), 2);
        assertEquals(snapshot.activeWebSocketConnections(), 5);
        assertEquals(snapshot.activeSseStreams(), 4);
        assertEquals(snapshot.webSocket().sentRate(), 1.0);
        assertEquals(snapshot.sse().receivedRate(), 7.0);
    }

    @Test
    public void drainTrendWindowSnapshotShouldUseActualElapsedTimeSincePreviousDrain() {
        PerformanceTrendWindowCollector trendWindowCollector = new PerformanceTrendWindowCollector();
        PerformanceMetricsSnapshotService service = new PerformanceMetricsSnapshotService(
                new PerformanceStatsCollector(),
                trendWindowCollector,
                () -> 2,
                () -> 0,
                () -> 0,
                () -> 1_000L,
                now -> PerformanceRealtimeMetrics.Sample.empty(),
                now -> PerformanceRealtimeMetrics.LiveSnapshot.empty()
        );
        service.resetTrendWindow(0L);
        trendWindowCollector.record(new RequestResult(100, 200, true, "api", PerformanceProtocol.HTTP));
        trendWindowCollector.record(new RequestResult(300, 400, true, "api", PerformanceProtocol.HTTP));

        PerformanceTrendSnapshot snapshot = service.drainTrendWindowSnapshot(2_000L);

        assertEquals(snapshot.http().samples(), 2);
        assertEquals(snapshot.http().sampleRate(), 1.0);
    }
}
