package com.laker.postman.performance.core.threadgroup;

import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.plan.PerformanceConditionController;
import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceLoopController;
import com.laker.postman.performance.core.plan.PerformanceOnceOnlyController;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import org.testng.annotations.Test;

import java.util.Arrays;
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
    public void estimateTotalRequestsShouldCountOnceOnlyOncePerVirtualUser() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 2;
        threadGroupData.useTime = false;
        threadGroupData.loops = 3;
        LoopData loopData = new LoopData();
        loopData.iterations = 4;
        PerformanceTestPlan plan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan("group", threadGroupData, List.of(
                        new PerformanceLoopController("loop", loopData, List.of(
                                new PerformanceOnceOnlyController("once only", List.of(request("login")))
                        ))
                ))
        ));

        PerformanceRequestEstimate estimate = new PerformanceCoreThreadGroupPlanner().estimateRequestCount(plan);

        assertEquals(estimate.estimatedRequests(), 2L);
        assertEquals(estimate.dynamic(), false);
    }

    @Test
    public void estimateRequestCountShouldMarkConditionSubtreeAsDynamicUpperBound() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 2;
        threadGroupData.useTime = false;
        threadGroupData.loops = 3;
        PerformanceTestPlan plan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan("group", threadGroupData, List.of(
                        new PerformanceConditionController("condition", null, List.of(request("conditional")))
                ))
        ));

        PerformanceRequestEstimate estimate = new PerformanceCoreThreadGroupPlanner().estimateRequestCount(plan);

        assertEquals(estimate.estimatedRequests(), 6L);
        assertEquals(estimate.dynamic(), true);
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

    @Test
    public void estimatesShouldIgnoreNullThreadGroups() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 1;
        threadGroupData.useTime = false;
        threadGroupData.loops = 1;
        PerformanceTestPlan plan = new PerformanceTestPlan(Arrays.asList(
                null,
                new PerformanceThreadGroupPlan("group", threadGroupData, List.of(request("request")))
        ));

        PerformanceCoreThreadGroupPlanner planner = new PerformanceCoreThreadGroupPlanner();

        assertEquals(planner.getTotalThreads(plan), 1);
        assertEquals(planner.estimateTotalRequests(plan), 1L);
    }

    private static PerformanceCoreRequestSampler request(String name) {
        return new PerformanceCoreRequestSampler(
                name,
                PerformanceRequestSnapshot.empty(),
                null,
                List.of()
        );
    }
}
