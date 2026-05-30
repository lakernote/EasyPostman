package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceLoopController;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformancePlanScriptUsageDetectorTest {

    @Test
    public void plainHttpPlanShouldNotUseScripts() {
        PerformanceTestPlan plan = planWith(new PerformanceCoreRequestSampler(
                "plain",
                PerformanceRequestSnapshot.builder().url("http://localhost").build(),
                null,
                List.of()
        ));

        assertFalse(PerformancePlanScriptUsageDetector.usesScripts(plan));
    }

    @Test
    public void requestPreScriptShouldUseScripts() {
        PerformanceTestPlan plan = planWith(new PerformanceCoreRequestSampler(
                "script",
                PerformanceRequestSnapshot.builder()
                        .url("http://localhost")
                        .prescript("pm.variables.set('a', '1')")
                        .build(),
                null,
                List.of()
        ));

        assertTrue(PerformancePlanScriptUsageDetector.usesScripts(plan));
    }

    @Test
    public void nestedWebSocketSendScriptShouldUseScripts() {
        WebSocketPerformanceData webSocketData = new WebSocketPerformanceData();
        webSocketData.sendPreScript = "pm.variables.set('message', 'hello')";
        PerformanceTestPlan plan = planWith(new PerformanceLoopController(
                "loop",
                null,
                List.of(new PerformanceCoreRequestSampler(
                        "websocket",
                        PerformanceRequestSnapshot.builder().url("ws://localhost").build(),
                        webSocketData,
                        List.of()
                ))
        ));

        assertTrue(PerformancePlanScriptUsageDetector.usesScripts(plan));
    }

    private static PerformanceTestPlan planWith(com.laker.postman.performance.core.plan.PerformancePlanElement element) {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.numThreads = 1;
        return new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan("thread group", threadGroupData, List.of(element))
        ));
    }
}
