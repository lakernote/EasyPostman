package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.control.PerformanceTimerManager;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.assertTrue;

public class PerformanceTimerManagerTest extends AbstractSwingUiTest {

    @Test(timeOut = 3000)
    public void stopAllShouldNotWaitForSchedulerTerminationOnEdt() throws Exception {
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        PerformanceTimerManager manager = new PerformanceTimerManager(() -> true);
        manager.setTrendSamplingCallback(() -> {
        });
        manager.setReportRefreshCallback(() -> {
            callbackEntered.countDown();
            try {
                releaseCallback.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            manager.setReportRefreshEnabled(true);
            manager.startAll();
            assertTrue(callbackEntered.await(1, TimeUnit.SECONDS));

            AtomicLong elapsedMs = new AtomicLong();
            SwingUtilities.invokeAndWait(() -> {
                long start = System.nanoTime();
                manager.stopAll();
                elapsedMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            });

            assertTrue(elapsedMs.get() < 500, "stopAll blocked EDT for " + elapsedMs.get() + "ms");
        } finally {
            releaseCallback.countDown();
            manager.dispose();
        }
    }
}
