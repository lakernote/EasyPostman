package com.laker.postman.performance.core.threadgroup;

import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceLoopController;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceCoreThreadGroupPlannerTest {

    @Test
    public void estimateTotalRequestsShouldUseCoreSamplerContract() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 1;
        threadGroupData.useTime = false;
        threadGroupData.loops = 1;
        LoopData loopData = new LoopData();
        loopData.iterations = 3;
        PerformanceTestPlan plan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan("group", threadGroupData, List.of(
                        new PerformanceLoopController("loop", loopData, List.of(
                                new PerformanceCoreRequestSampler(
                                        "request",
                                        PerformanceRequestSnapshot.empty(),
                                        null,
                                        List.of()
                                )
                        ))
                ))
        ));

        assertEquals(new PerformanceCoreThreadGroupPlanner().estimateTotalRequests(plan), 3L);
    }

    @Test
    public void totalThreadsShouldUseMaximumThreadCountForSpikeMode() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.SPIKE;
        threadGroupData.spikeMinThreads = 1;
        threadGroupData.spikeMaxThreads = 9;
        PerformanceTestPlan plan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan("group", threadGroupData, List.of())
        ));

        assertEquals(new PerformanceCoreThreadGroupPlanner().getTotalThreads(plan), 9);
    }
}
