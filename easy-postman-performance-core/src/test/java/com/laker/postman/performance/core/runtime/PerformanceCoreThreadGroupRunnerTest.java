package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceCoreThreadGroupRunnerTest {

    @Test(timeOut = 3000)
    public void shouldRunFixedThreadGroupWithGenericIterationContext() {
        PerformanceVirtualUserCoordinator virtualUsers = new PerformanceVirtualUserCoordinator();
        List<String> executions = new CopyOnWriteArrayList<>();
        List<PerformanceRunProgress> progressEvents = new CopyOnWriteArrayList<>();
        PerformanceCoreThreadGroupRunner<String> runner = new PerformanceCoreThreadGroupRunner<>(
                () -> true,
                System::currentTimeMillis,
                () -> {
                },
                virtualUsers,
                (groupPlan, iterationCount) -> "ctx:" + groupPlan.getName() + ":" + iterationCount,
                (groupPlan, iterationContext) -> executions.add(iterationContext),
                () -> new PerformanceCoreResultSink() {
                    @Override
                    public void onProgress(PerformanceRunProgress progress) {
                        progressEvents.add(progress);
                    }
                }
        );

        runner.run(new PerformanceTestPlan(List.of(fixedGroup("group", 2))), 1);

        assertEquals(executions, List.of("ctx:group:2", "ctx:group:2"));
        assertEquals(virtualUsers.getActiveThreads(), 0);
        assertTrue(progressEvents.stream().anyMatch(progress ->
                progress.getActiveThreads() == 1 && progress.getTotalThreads() == 1));
        assertEquals(progressEvents.get(progressEvents.size() - 1).getActiveThreads(), 0);
    }

    @Test(timeOut = 3000)
    public void adjustSpikeThreadCountShouldIgnoreStaleThreadEndEntries() throws Exception {
        PerformanceCoreThreadGroupRunner<String> runner = new PerformanceCoreThreadGroupRunner<>(
                () -> true,
                () -> 0L,
                () -> {
                },
                new PerformanceVirtualUserCoordinator(),
                (groupPlan, iterationCount) -> "ctx",
                (groupPlan, iterationContext) -> {
                },
                noopSink()
        );
        ThreadGroupData threadGroupData = new ThreadGroupData();
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker.start();

        ConcurrentHashMap<Thread, Long> staleThreadEndTimes = new ConcurrentHashMap<>() {
            @Override
            public Long get(Object key) {
                if (key == worker) {
                    return null;
                }
                return super.get(key);
            }
        };
        staleThreadEndTimes.put(worker, Long.MAX_VALUE);

        try {
            runner.adjustSpikeThreadCount(
                    null,
                    threadGroupData,
                    new AtomicInteger(1),
                    0,
                    1,
                    (BiConsumer<Integer, Integer>) (active, total) -> {
                    },
                    1,
                    staleThreadEndTimes
            );
        } finally {
            worker.interrupt();
            worker.join(1_000);
        }
    }

    private static Supplier<PerformanceCoreResultSink> noopSink() {
        return () -> PerformanceCoreResultSink.NOOP;
    }

    private static PerformanceThreadGroupPlan fixedGroup(String name, int loops) {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 1;
        threadGroupData.useTime = false;
        threadGroupData.loops = loops;
        return new PerformanceThreadGroupPlan(name, threadGroupData, List.of());
    }
}
