package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocumentCompiler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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

    @Test
    public void shouldKeepCsvRowsInWorkerVirtualUserRanges() {
        PerformanceRunPlan runPlan = PerformanceRunPlan.builder()
                .testPlan(documentWithFixedThreadGroupAndCsv(100, 100))
                .build();
        List<PerformanceWorkerEndpoint> endpoints = List.of(
                new PerformanceWorkerEndpoint("127.0.0.1", 19090),
                new PerformanceWorkerEndpoint("127.0.0.1", 19091)
        );

        List<PerformanceWorkerAssignment> assignments = new PerformanceWorkerAssignmentPlanner()
                .plan(runPlan, endpoints, "run-csv-ranges");
        PerformanceTestPlan compiledPlan = PerformanceCorePlanDocumentCompiler.compile(runPlan.getTestPlan());
        PerformanceWorkerExecutionPlanPartitioner partitioner = new PerformanceWorkerExecutionPlanPartitioner();
        PerformanceThreadGroupPlan firstWorkerGroup = partitioner.apply(compiledPlan, assignments.get(0))
                .getThreadGroups()
                .get(0);
        PerformanceThreadGroupPlan secondWorkerGroup = partitioner.apply(compiledPlan, assignments.get(1))
                .getThreadGroups()
                .get(0);

        assertEquals(assignments.get(0).getThreadGroups().get(0).getFirstVirtualUserIndex(), 0);
        assertEquals(assignments.get(0).getThreadGroups().get(0).getVirtualUserCount(), 50);
        assertEquals(assignments.get(1).getThreadGroups().get(0).getFirstVirtualUserIndex(), 50);
        assertEquals(assignments.get(1).getThreadGroups().get(0).getVirtualUserCount(), 50);
        assertEquals(firstWorkerGroup.getThreadGroupData().numThreads, 50);
        assertEquals(firstWorkerGroup.getVirtualUserIndexOffset(), 0);
        assertEquals(secondWorkerGroup.getThreadGroupData().numThreads, 50);
        assertEquals(secondWorkerGroup.getVirtualUserIndexOffset(), 50);
        assertEquals(firstWorkerGroup.csvRowForVirtualUser(0).get("userId"), "u000");
        assertEquals(firstWorkerGroup.csvRowForVirtualUser(49).get("userId"), "u049");
        assertEquals(secondWorkerGroup.csvRowForVirtualUser(0).get("userId"), "u050");
        assertEquals(secondWorkerGroup.csvRowForVirtualUser(49).get("userId"), "u099");

        Set<String> assignedUserIds = new LinkedHashSet<>();
        for (int i = 0; i < 50; i++) {
            assignedUserIds.add(firstWorkerGroup.csvRowForVirtualUser(i).get("userId"));
            assignedUserIds.add(secondWorkerGroup.csvRowForVirtualUser(i).get("userId"));
        }
        assertEquals(assignedUserIds.size(), 100);
        assertTrue(assignedUserIds.contains("u000"));
        assertTrue(assignedUserIds.contains("u099"));
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

    private static PerformanceCorePlanDocument documentWithFixedThreadGroupAndCsv(int users, int rows) {
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
                        .children(List.of(PerformanceCorePlanNode.builder()
                                .name("csv users")
                                .type(NodeType.CSV_DATA_SET)
                                .csvDataSetData(csvData(rows))
                                .build()))
                        .build()))
                .build());
    }

    private static CsvDataSetData csvData(int rows) {
        List<Map<String, String>> csvRows = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("userId", String.format("u%03d", i));
            csvRows.add(row);
        }
        return new CsvDataSetData("users.csv", List.of("userId"), csvRows);
    }
}
