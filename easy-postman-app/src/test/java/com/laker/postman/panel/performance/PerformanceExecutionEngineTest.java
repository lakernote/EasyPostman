package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.RequestResult;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.assertEquals;

public class PerformanceExecutionEngineTest {

    @Test
    public void shouldKeepPeakVirtualUsersForFinalTrendSample() throws Exception {
        PerformanceExecutionEngine engine = newEngine();
        engine.beginRun(1_000L);

        invokeActiveThreadStarted(engine);
        invokeActiveThreadStarted(engine);
        invokeActiveThreadStarted(engine);
        assertEquals(engine.getTrendVirtualUsers(), 3);

        invokeActiveThreadFinished(engine);
        invokeActiveThreadFinished(engine);
        invokeActiveThreadFinished(engine);
        assertEquals(engine.getTrendVirtualUsers(), 3);

        engine.resetVirtualUsers();
        assertEquals(engine.getTrendVirtualUsers(), 0);
    }

    private static PerformanceExecutionEngine newEngine() {
        return new PerformanceExecutionEngine(
                null,
                () -> true,
                () -> true,
                null,
                new ArrayList<RequestResult>(),
                new ConcurrentHashMap<String, List<Long>>(),
                new ConcurrentHashMap<String, Integer>(),
                new ConcurrentHashMap<String, Integer>(),
                new Object(),
                null
        );
    }

    private static void invokeActiveThreadStarted(PerformanceExecutionEngine engine) throws Exception {
        Method method = PerformanceExecutionEngine.class.getDeclaredMethod("incrementActiveThreads");
        method.setAccessible(true);
        method.invoke(engine);
    }

    private static void invokeActiveThreadFinished(PerformanceExecutionEngine engine) throws Exception {
        Method method = PerformanceExecutionEngine.class.getDeclaredMethod("decrementActiveThreads");
        method.setAccessible(true);
        method.invoke(engine);
    }
}
