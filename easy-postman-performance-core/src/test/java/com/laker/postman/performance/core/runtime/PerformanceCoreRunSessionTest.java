package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

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
}
