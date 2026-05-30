package com.laker.postman.performance.worker;

import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;
import com.laker.postman.performance.core.worker.PerformanceWorkerThreadGroupAssignment;
import com.laker.postman.startup.HeadlessStartupBootstrap;

import java.io.PrintStream;
import java.util.stream.Collectors;

public class PerformanceWorkerCommand {

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            PerformanceWorkerOptions options = PerformanceWorkerCommandLine.parse(args);
            if (options.isHelp()) {
                printUsage(out);
                return 0;
            }
            HeadlessStartupBootstrap.initRuntime();
            try (PerformanceWorkerServer server = new PerformanceWorkerServer(
                    options,
                    new DefaultPerformanceWorkerRunExecutor(),
                    consoleListener(out)
            )) {
                server.start();
                out.printf("Performance worker listening on %s:%d%n", options.getHost(), server.getPort());
                out.flush();
                server.awaitShutdown();
            }
            return 0;
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            printUsage(err);
            return 2;
        } catch (Exception ex) {
            err.println("Performance worker failed: " + ex.getMessage());
            return 1;
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: performance worker [--host <host>] [--port <port>] "
                + "[--progress-interval <seconds>] [--no-progress]");
    }

    private static PerformanceWorkerServerListener consoleListener(PrintStream out) {
        return new PerformanceWorkerServerListener() {
            @Override
            public void onRunAccepted(String runId, String workerId, PerformanceWorkerAssignment assignment) {
                out.printf("Performance worker accepted run: runId=%s worker=%s assignment=%s%n",
                        runId,
                        workerId,
                        assignmentSummary(assignment));
                out.flush();
            }

            @Override
            public void onRunStarted(String runId, String workerId) {
                out.printf("Performance worker started run: runId=%s worker=%s%n", runId, workerId);
                out.flush();
            }

            @Override
            public void onRunProgress(String runId,
                                      String workerId,
                                      String status,
                                      int activeUsers,
                                      int totalUsers,
                                      long totalRequests,
                                      long successRequests,
                                      long failedRequests,
                                      double qps) {
                out.printf(
                        "Performance worker progress: runId=%s worker=%s status=%s users=%d/%d total=%d success=%d failed=%d qps=%.2f%n",
                        runId,
                        workerId,
                        status,
                        activeUsers,
                        totalUsers,
                        totalRequests,
                        successRequests,
                        failedRequests,
                        qps
                );
                out.flush();
            }

            @Override
            public void onRunCompleted(String runId,
                                       String workerId,
                                       String status,
                                       PerformanceJsonReportSummary summary,
                                       long elapsedMs,
                                       String error) {
                long total = summary == null ? 0L : summary.getTotalRequests();
                long success = summary == null ? 0L : summary.getSuccessRequests();
                long failed = summary == null ? 0L : summary.getFailedRequests();
                out.printf(
                        "Performance worker completed run: runId=%s worker=%s status=%s total=%d success=%d failed=%d elapsedMs=%d",
                        runId,
                        workerId,
                        status,
                        total,
                        success,
                        failed,
                        elapsedMs
                );
                if (error != null && !error.isBlank()) {
                    out.printf(" error=%s", error);
                }
                out.println();
                out.flush();
            }
        };
    }

    static String assignmentSummary(PerformanceWorkerAssignment assignment) {
        if (assignment == null || assignment.getThreadGroups().isEmpty()) {
            return "[]";
        }
        return assignment.getThreadGroups().stream()
                .map(PerformanceWorkerCommand::threadGroupAssignmentSummary)
                .collect(Collectors.joining(";", "[", "]"));
    }

    private static String threadGroupAssignmentSummary(PerformanceWorkerThreadGroupAssignment assignment) {
        int first = assignment.getFirstVirtualUserIndex();
        int count = assignment.getVirtualUserCount();
        return "groupIndex=" + assignment.getThreadGroupIndex()
                + ",first=" + first
                + ",count=" + count
                // CSV 不做物理切片，按全局虚拟用户编号取行；这里把绑定范围直接打到日志里，方便验收分片是否重复。
                + ",csvGlobalUsers=" + csvGlobalUserRange(first, count);
    }

    private static String csvGlobalUserRange(int firstVirtualUserIndex, int virtualUserCount) {
        if (virtualUserCount <= 0) {
            return "none";
        }
        return firstVirtualUserIndex + "-" + (firstVirtualUserIndex + virtualUserCount - 1);
    }
}
