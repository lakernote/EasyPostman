package com.laker.postman.panel.performance.threadgroup;

import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class PerformanceThreadGroupPlannerTest {

    @Test
    public void plannerApisShouldNotExposeSwingTreeCompatibilityMethods() {
        assertFalse(hasDefaultMutableTreeNodeParameter(PerformanceThreadGroupPlanner.class));
    }

    @Test
    public void estimateTotalRequestsShouldUseControllerContract() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 1;
        threadGroupData.useTime = false;
        threadGroupData.loops = 1;
        PerformanceRequestSampler sampler = new PerformanceRequestSampler("request", null, null, List.of());
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

    private static boolean hasDefaultMutableTreeNodeParameter(Class<?> type) {
        return java.util.Arrays.stream(type.getMethods())
                .flatMap(method -> java.util.Arrays.stream(method.getParameterTypes()))
                .anyMatch(DefaultMutableTreeNode.class::equals);
    }
}
