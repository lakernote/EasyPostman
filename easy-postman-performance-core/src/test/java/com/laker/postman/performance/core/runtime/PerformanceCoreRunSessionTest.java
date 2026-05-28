package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class PerformanceCoreRunSessionTest {

    @Test(timeOut = 3000)
    public void startShouldRunPlanAndPublishCompletionWithoutAppTypes() throws Exception {
        AtomicBoolean running = new AtomicBoolean(false);
        RecordingEngine engine = new RecordingEngine();
        List<PerformanceRunSummary> summaries = new ArrayList<>();
        PerformanceCoreRunSession session = new PerformanceCoreRunSession(
                running::get,
                running::set,
                engine
        );

        PerformanceRunHandle handle = session.start(
                new PerformanceTestPlan(List.of()),
                new PerformanceCoreResultSink() {
                    @Override
                    public void onComplete(PerformanceRunSummary summary) {
                        summaries.add(summary);
                    }
                }
        );
        handle.join(1000);

        assertFalse(handle.isAlive());
        assertFalse(session.isRunning());
        assertEquals(engine.beginRunCount, 1);
        assertEquals(engine.beginRunSinkCount, 1);
        assertEquals(engine.runCount, 1);
        assertEquals(engine.cancelAllCount, 1);
        assertEquals(engine.endRunCount, 1);
        assertEquals(summaries.size(), 1);
        assertFalse(summaries.get(0).isStopped());
    }

    @Test(timeOut = 3000)
    public void startShouldRejectNewRunUntilStoppedRunThreadCompletes() throws Exception {
        AtomicBoolean running = new AtomicBoolean(false);
        BlockingEngine engine = new BlockingEngine();
        PerformanceCoreRunSession session = new PerformanceCoreRunSession(
                running::get,
                running::set,
                engine
        );

        PerformanceRunHandle firstHandle = session.start(new PerformanceTestPlan(List.of()), PerformanceCoreResultSink.NOOP);
        assertTrue(engine.firstRunStarted.await(1, TimeUnit.SECONDS));

        session.stop();
        PerformanceRunHandle secondHandle = session.start(new PerformanceTestPlan(List.of()), PerformanceCoreResultSink.NOOP);

        assertNull(secondHandle.threadOrNull());
        assertEquals(engine.beginRunCount.get(), 1);
        assertEquals(engine.runCount.get(), 1);

        engine.allowCompletion.countDown();
        firstHandle.join(1_000);
        assertFalse(firstHandle.isAlive());

        PerformanceRunHandle thirdHandle = session.start(new PerformanceTestPlan(List.of()), PerformanceCoreResultSink.NOOP);
        thirdHandle.join(1_000);

        assertFalse(thirdHandle.isAlive());
        assertEquals(engine.beginRunCount.get(), 2);
        assertEquals(engine.runCount.get(), 2);
    }

    private static final class RecordingEngine implements PerformanceCoreRunSession.ExecutionEngine {
        private int beginRunCount;
        private int beginRunSinkCount;
        private int runCount;
        private int cancelAllCount;
        private int endRunCount;

        @Override
        public void beginRun(long startTimeMs) {
            beginRunCount++;
        }

        @Override
        public void beginRun(long startTimeMs, PerformanceCoreResultSink resultSink) {
            beginRunCount++;
            if (resultSink != null) {
                beginRunSinkCount++;
            }
        }

        @Override
        public int getTotalThreads(PerformanceTestPlan plan) {
            return 0;
        }

        @Override
        public void runTestPlan(PerformanceTestPlan plan, int totalThreads) {
            runCount++;
        }

        @Override
        public void cancelAllNetworkCalls() {
            cancelAllCount++;
        }

        @Override
        public void endRun() {
            endRunCount++;
        }
    }

    private static final class BlockingEngine implements PerformanceCoreRunSession.ExecutionEngine {
        private final CountDownLatch firstRunStarted = new CountDownLatch(1);
        private final CountDownLatch allowCompletion = new CountDownLatch(1);
        private final AtomicInteger beginRunCount = new AtomicInteger();
        private final AtomicInteger runCount = new AtomicInteger();

        @Override
        public void beginRun(long startTimeMs, PerformanceCoreResultSink resultSink) {
            beginRunCount.incrementAndGet();
        }

        @Override
        public int getTotalThreads(PerformanceTestPlan plan) {
            return 1;
        }

        @Override
        public void runTestPlan(PerformanceTestPlan plan, int totalThreads) {
            int currentRun = runCount.incrementAndGet();
            if (currentRun == 1) {
                firstRunStarted.countDown();
                while (true) {
                    try {
                        if (allowCompletion.await(50, TimeUnit.MILLISECONDS)) {
                            return;
                        }
                    } catch (InterruptedException ignored) {
                        Thread.interrupted();
                    }
                }
            }
        }

        @Override
        public void cancelAllNetworkCalls() {
        }
    }
}
