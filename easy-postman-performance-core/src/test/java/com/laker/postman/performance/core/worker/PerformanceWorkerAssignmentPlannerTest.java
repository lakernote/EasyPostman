package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocumentCompiler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceWorkerAssignmentPlannerTest {

    @Test
    public void shouldSplitFixedThreadGroupsAcrossWorkersAndApplyOffsets() {
        PerformanceRunPlan runPlan = PerformanceRunPlan.builder()
                .testPlan(documentWithFixedThreadGroup(5))
                .build();
        List<PerformanceWorkerEndpoint> endpoints = List.of(
                new PerformanceWorkerEndpoint("127.0.0.1", 19090),
                new PerformanceWorkerEndpoint("127.0.0.1", 19091)
        );

        List<PerformanceWorkerAssignment> assignments = new PerformanceWorkerAssignmentPlanner()
                .plan(runPlan, endpoints, "run-1");

        assertEquals(assignments.size(), 2);
        assertEquals(assignments.get(0).getThreadGroups().get(0).getFirstVirtualUserIndex(), 0);
        assertEquals(assignments.get(0).getThreadGroups().get(0).getVirtualUserCount(), 3);
        assertEquals(assignments.get(1).getThreadGroups().get(0).getFirstVirtualUserIndex(), 3);
        assertEquals(assignments.get(1).getThreadGroups().get(0).getVirtualUserCount(), 2);

        PerformanceTestPlan workerPlan = new PerformanceWorkerExecutionPlanPartitioner().apply(
                PerformanceCorePlanDocumentCompiler.compile(runPlan.getTestPlan()),
                assignments.get(1)
        );

        assertEquals(workerPlan.getThreadGroups().size(), 1);
        assertEquals(workerPlan.getThreadGroups().get(0).getThreadGroupData().numThreads, 2);
        assertEquals(workerPlan.getThreadGroups().get(0).getVirtualUserIndexOffset(), 3);
    }

    @Test
    public void shouldProduceEmptyWorkerPlanWhenAssignmentHasNoVirtualUsers() {
        PerformanceRunPlan runPlan = PerformanceRunPlan.builder()
                .testPlan(documentWithFixedThreadGroup(1))
                .build();
        List<PerformanceWorkerEndpoint> endpoints = List.of(
                new PerformanceWorkerEndpoint("127.0.0.1", 19090),
                new PerformanceWorkerEndpoint("127.0.0.1", 19091)
        );

        List<PerformanceWorkerAssignment> assignments = new PerformanceWorkerAssignmentPlanner()
                .plan(runPlan, endpoints, "run-empty-assignment");
        PerformanceTestPlan workerPlan = new PerformanceWorkerExecutionPlanPartitioner().apply(
                PerformanceCorePlanDocumentCompiler.compile(runPlan.getTestPlan()),
                assignments.get(1)
        );

        assertEquals(assignments.get(1).getThreadGroups().size(), 0);
        assertEquals(workerPlan.getThreadGroups().size(), 0);
    }

    private static PerformanceCorePlanDocument documentWithFixedThreadGroup(int users) {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = users;
        threadGroupData.useTime = false;
        threadGroupData.loops = 1;
        return new PerformanceCorePlanDocument(PerformanceCorePlanNode.builder()
                .name("run plan")
                .type(NodeType.ROOT)
                .children(List.of(PerformanceCorePlanNode.builder()
                        .name("users")
                        .type(NodeType.THREAD_GROUP)
                        .threadGroupData(threadGroupData)
                        .build()))
                .build());
    }
}
