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
import static org.testng.Assert.assertTrue;

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

    @Test
    public void totalThreadsShouldSaturateWhenLargeThreadCountsOverflowIntRange() {
        ThreadGroupData firstGroup = new ThreadGroupData();
        firstGroup.threadMode = ThreadGroupData.ThreadMode.FIXED;
        firstGroup.numThreads = Integer.MAX_VALUE;
        ThreadGroupData secondGroup = new ThreadGroupData();
        secondGroup.threadMode = ThreadGroupData.ThreadMode.FIXED;
        secondGroup.numThreads = 1;
        PerformanceTestPlan plan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan("first", firstGroup, List.of()),
                new PerformanceThreadGroupPlan("second", secondGroup, List.of())
        ));

        assertEquals(new PerformanceCoreThreadGroupPlanner().getTotalThreads(plan), Integer.MAX_VALUE);
    }

    @Test
    public void estimateTotalRequestsShouldNotOverflowWhenAveragingLargeThreadCounts() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.RAMP_UP;
        threadGroupData.rampUpStartThreads = Integer.MAX_VALUE;
        threadGroupData.rampUpEndThreads = Integer.MAX_VALUE;
        threadGroupData.rampUpDuration = 1;
        PerformanceTestPlan plan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan("group", threadGroupData, List.of(
                        new PerformanceCoreRequestSampler(
                                "request",
                                PerformanceRequestSnapshot.empty(),
                                null,
                                List.of()
                        )
                ))
        ));

        assertTrue(new PerformanceCoreThreadGroupPlanner().estimateTotalRequests(plan) > 0L);
    }
}
