package com.laker.postman.performance.worker;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.runtime.PerformanceRunExecutionControl;
import com.laker.postman.performance.runtime.PerformanceRunExecutionResult;
import com.laker.postman.performance.runtime.PerformanceRunPlanExecutor;
import org.testng.annotations.Test;

import java.io.PrintStream;

import static org.testng.Assert.assertEquals;

public class DefaultPerformanceWorkerRunExecutorTest {

    @Test
    public void shouldStampWorkerRunMetadataOnFinalReport() throws Exception {
        DefaultPerformanceWorkerRunExecutor executor = new DefaultPerformanceWorkerRunExecutor(
                new PerformanceRunPlanExecutor() {
                    @Override
                    public PerformanceRunExecutionResult execute(PerformanceRunPlan runPlan,
                                                                 String planPath,
                                                                 PerformanceWorkerAssignment assignment,
                                                                 PrintStream scriptOutput,
                                                                 PerformanceRunExecutionControl control) {
                        return PerformanceRunExecutionResult.builder()
                                .report(PerformanceJsonReport.builder()
                                        .metadata(PerformanceJsonReportMetadata.builder()
                                                .runId("local-run")
                                                .source("local")
                                                .status("SUCCESS")
                                                .planPath(planPath)
                                                .startTimeMs(10L)
                                                .endTimeMs(40L)
                                                .elapsedTimeMs(30L)
                                                .build())
                                        .summary(PerformanceJsonReportSummary.builder()
                                                .totalRequests(2L)
                                                .successRequests(2L)
                                                .build())
                                        .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                                        .build())
                                .build();
                    }
                }
        );
        PerformanceWorkerRunRequest request = PerformanceWorkerRunRequest.builder()
                .runId("run-meta")
                .plan(emptyPlan())
                .assignment(PerformanceWorkerAssignment.builder()
                        .runId("run-meta")
                        .workerId("worker-a")
                        .endpoint(new PerformanceWorkerEndpoint("127.0.0.1", 19091))
                        .build())
                .build();

        PerformanceJsonReport report = executor.execute(request, new PerformanceRunExecutionControl());

        assertEquals(report.getMetadata().getRunId(), "run-meta");
        assertEquals(report.getMetadata().getSource(), "127.0.0.1:19091");
        assertEquals(report.getMetadata().getPlanPath(), "worker:run-meta");
        assertEquals(report.getSummary().getTotalRequests(), 2L);
    }

    private static PerformanceRunPlan emptyPlan() {
        return PerformanceRunPlan.builder()
                .testPlan(new PerformanceCorePlanDocument(PerformanceCorePlanNode.builder()
                        .name("run plan")
                        .type(NodeType.ROOT)
                        .build()))
                .build();
    }
}
