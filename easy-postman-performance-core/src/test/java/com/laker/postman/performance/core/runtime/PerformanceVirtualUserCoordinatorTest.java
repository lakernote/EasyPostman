package com.laker.postman.performance.core.runtime;

import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceVirtualUserCoordinatorTest {

    @Test
    public void shouldSamplePeakActiveThreadsForTrendWindow() throws Exception {
        PerformanceVirtualUserCoordinator coordinator = new PerformanceVirtualUserCoordinator();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread worker = coordinator.newThread("vu-test", (active, total) -> {
        }, 1, () -> {
            started.countDown();
            await(release);
        });
        worker.start();

        assertTrue(started.await(1, TimeUnit.SECONDS));
        assertEquals(coordinator.sampleWindowPeakActiveThreads(), 1);

        release.countDown();
        worker.join(1_000);

        assertEquals(coordinator.sampleWindowPeakActiveThreads(), 1);
        assertEquals(coordinator.sampleWindowPeakActiveThreads(), 0);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
