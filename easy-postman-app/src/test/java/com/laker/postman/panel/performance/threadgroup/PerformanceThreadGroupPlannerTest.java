package com.laker.postman.panel.performance.threadgroup;

import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.plan.PerformanceController;
import com.laker.postman.panel.performance.plan.PerformancePlanElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.plan.PerformanceTestPlan;
import com.laker.postman.panel.performance.plan.PerformanceThreadGroupPlan;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceThreadGroupPlannerTest {

    @Test
    public void estimateTotalRequestsShouldUseControllerContract() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 1;
        threadGroupData.useTime = false;
        threadGroupData.loops = 1;
        PerformanceRequestSampler sampler = new PerformanceRequestSampler("request", null, null, null, List.of());
        PerformanceController controller = new TestController("controller", 3, List.of(sampler));
        PerformanceTestPlan plan = new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan("group", threadGroupData, List.of(controller))
        ));

        assertEquals(new PerformanceThreadGroupPlanner().estimateTotalRequests(plan), 3L);
    }

    private record TestController(String name,
                                  int iterationCount,
                                  List<PerformancePlanElement> elements) implements PerformanceController {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public NodeType getType() {
            return NodeType.LOOP;
        }

        @Override
        public int getIterationCount() {
            return iterationCount;
        }

        @Override
        public List<PerformancePlanElement> getElements() {
            return elements;
        }
    }
}
