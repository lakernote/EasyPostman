package com.laker.postman.performance.core.worker;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

public class PerformanceWorkerAssignmentJsonStorageTest {

    @Test
    public void shouldRoundTripWorkerAssignmentJson() throws Exception {
        PerformanceWorkerAssignment assignment = PerformanceWorkerAssignment.builder()
                .runId("run-1")
                .workerId("worker-a")
                .assignmentId("assignment-a")
                .endpoint(new PerformanceWorkerEndpoint("127.0.0.1", 19090))
                .threadGroups(List.of(new PerformanceWorkerThreadGroupAssignment(
                        "0",
                        0,
                        10,
                        25
                )))
                .build();
        PerformanceWorkerAssignmentJsonStorage storage = new PerformanceWorkerAssignmentJsonStorage();

        String json = storage.toJson(assignment);
        PerformanceWorkerAssignment loaded = storage.fromJson(json);

        assertEquals(loaded.getSchemaVersion(), PerformanceWorkerAssignmentJsonStorage.FORMAT_VERSION);
        assertEquals(loaded.getRunId(), "run-1");
        assertEquals(loaded.getWorkerId(), "worker-a");
        assertEquals(loaded.getAssignmentId(), "assignment-a");
        assertEquals(loaded.getEndpoint().getHost(), "127.0.0.1");
        assertEquals(loaded.getEndpoint().getPort(), 19090);
        assertEquals(loaded.getThreadGroups().get(0).getThreadGroupPath(), "0");
        assertEquals(loaded.getThreadGroups().get(0).getFirstVirtualUserIndex(), 10);
        assertEquals(loaded.getThreadGroups().get(0).getVirtualUserCount(), 25);
    }

    @Test
    public void shouldDefensivelyCopyThreadGroupAssignments() {
        List<PerformanceWorkerThreadGroupAssignment> threadGroups = new java.util.ArrayList<>();
        threadGroups.add(new PerformanceWorkerThreadGroupAssignment("0", 0, 0, 1));

        PerformanceWorkerAssignment assignment = PerformanceWorkerAssignment.builder()
                .threadGroups(threadGroups)
                .build();
        threadGroups.clear();

        assertEquals(assignment.getThreadGroups().size(), 1);
        assertNotSame(assignment.getThreadGroups(), threadGroups);
    }

    @Test
    public void shouldSaveAndLoadAssignmentFile() throws Exception {
        Path assignmentPath = Files.createTempDirectory("ep-worker-assignment").resolve("assignment.json");
        PerformanceWorkerAssignment assignment = PerformanceWorkerAssignment.builder()
                .runId("run-file")
                .workerId("worker-file")
                .threadGroups(List.of(new PerformanceWorkerThreadGroupAssignment("1", 1, 0, 4)))
                .build();
        PerformanceWorkerAssignmentJsonStorage storage = new PerformanceWorkerAssignmentJsonStorage();

        storage.save(assignmentPath, assignment);
        PerformanceWorkerAssignment loaded = storage.load(assignmentPath);

        assertEquals(loaded.getRunId(), "run-file");
        assertEquals(loaded.getThreadGroups().get(0).getThreadGroupIndex(), 1);
    }
}
