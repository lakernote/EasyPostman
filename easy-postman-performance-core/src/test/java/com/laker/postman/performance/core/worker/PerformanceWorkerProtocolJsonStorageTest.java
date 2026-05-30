package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PerformanceWorkerProtocolJsonStorageTest {

    @Test
    public void shouldRoundTripWorkerRunRequestAndResponses() {
        PerformanceWorkerRunRequest request = PerformanceWorkerRunRequest.builder()
                .runId("run-1")
                .plan(emptyPlan())
                .assignment(PerformanceWorkerAssignment.builder()
                        .runId("run-1")
                        .workerId("worker-a")
                        .assignmentId("assignment-a")
                        .endpoint(new PerformanceWorkerEndpoint("127.0.0.1", 19090))
                        .threadGroups(List.of(new PerformanceWorkerThreadGroupAssignment("0", 0, 2, 3)))
                        .build())
                .build();
        PerformanceWorkerProtocolJsonStorage storage = new PerformanceWorkerProtocolJsonStorage();

        PerformanceWorkerRunRequest loaded = storage.runRequestFromJson(storage.toJson(request));

        assertEquals(loaded.getRunId(), "run-1");
        assertEquals(loaded.getAssignment().getWorkerId(), "worker-a");
        assertEquals(loaded.getAssignment().getThreadGroups().get(0).getFirstVirtualUserIndex(), 2);
        assertEquals(loaded.getPlan().getTestPlan().getRoot().getName(), "run plan");

        PerformanceJsonReport report = PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder().runId("run-1").status(PerformanceRunStatus.SUCCESS).build())
                .summary(PerformanceJsonReportSummary.builder().totalRequests(4L).successRequests(4L).build())
                .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                .build();
        PerformanceWorkerRunResultResponse result = PerformanceWorkerRunResultResponse.builder()
                .runId(report.getMetadata().getRunId())
                .workerId(report.getMetadata().getSource())
                .status(report.getMetadata().getStatus())
                .report(report)
                .error(report.getMetadata().getError())
                .build();
        PerformanceWorkerRunResultResponse loadedResult = storage.resultResponseFromJson(storage.toJson(result));

        assertEquals(loadedResult.getStatus(), PerformanceRunStatus.SUCCESS);
        assertEquals(loadedResult.getReport().getSummary().getTotalRequests(), 4L);

        PerformanceWorkerRunStatusResponse status = PerformanceWorkerRunStatusResponse.builder()
                .runId("run-1")
                .workerId("worker-a")
                .status(PerformanceRunStatus.RUNNING)
                .activeUsers(2)
                .totalUsers(7)
                .totalRequests(10L)
                .successRequests(9L)
                .failedRequests(1L)
                .qps(12.5)
                .report(report)
                .build();
        PerformanceWorkerRunStatusResponse loadedStatus = storage.statusResponseFromJson(storage.toJson(status));

        assertEquals(loadedStatus.getActiveUsers(), 2);
        assertEquals(loadedStatus.getTotalUsers(), 7);
        assertEquals(loadedStatus.getTotalRequests(), 10L);
        assertEquals(loadedStatus.getSuccessRequests(), 9L);
        assertEquals(loadedStatus.getFailedRequests(), 1L);
        assertEquals(loadedStatus.getQps(), 12.5);
        assertEquals(loadedStatus.getReport().getSummary().getTotalRequests(), 4L);

        PerformanceWorkerRunAcceptedResponse accepted = PerformanceWorkerRunAcceptedResponse.builder()
                .runId("run-1")
                .workerId("worker-a")
                .build();
        PerformanceWorkerRunAcceptedResponse loadedAccepted = storage.acceptedResponseFromJson(storage.toJson(accepted));

        assertEquals(loadedAccepted.getRunId(), "run-1");
        assertEquals(loadedAccepted.getWorkerId(), "worker-a");
        assertEquals(loadedAccepted.getStatus(), PerformanceRunStatus.ACCEPTED);
    }

    private static PerformanceRunPlan emptyPlan() {
        return PerformanceRunPlan.builder()
                .generatedBy("test")
                .testPlan(new PerformanceCorePlanDocument(PerformanceCorePlanNode.builder()
                        .name("run plan")
                        .type(NodeType.ROOT)
                        .build()))
                .build();
    }
}
