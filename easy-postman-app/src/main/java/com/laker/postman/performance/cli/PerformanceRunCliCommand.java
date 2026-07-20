package com.laker.postman.performance.cli;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import com.laker.postman.performance.output.PerformanceCommandLinePathOption;
import com.laker.postman.performance.output.PerformanceCommandPathValidator;
import com.laker.postman.performance.output.PerformanceCommandReportFactory;
import com.laker.postman.performance.output.PerformanceCommandReportOutput;
import com.laker.postman.startup.HeadlessStartupBootstrap;
import com.laker.postman.performance.runtime.PerformanceRunExecutionResult;
import com.laker.postman.performance.runtime.PerformanceRunPlanExecutor;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PerformanceRunCliCommand {
    private final RuntimeBootstrap runtimeBootstrap;
    private final PerformanceRunPlanExecutor executor;

    public PerformanceRunCliCommand() {
        this(HeadlessStartupBootstrap::initRuntime);
    }

    public PerformanceRunCliCommand(RuntimeBootstrap runtimeBootstrap) {
        this(runtimeBootstrap, new PerformanceRunPlanExecutor());
    }

    PerformanceRunCliCommand(RuntimeBootstrap runtimeBootstrap, PerformanceRunPlanExecutor executor) {
        this.runtimeBootstrap = runtimeBootstrap == null ? () -> {
        } : runtimeBootstrap;
        this.executor = executor == null ? new PerformanceRunPlanExecutor() : executor;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        long commandStartTimeMs = System.currentTimeMillis();
        Path fallbackPlanPath = PerformanceCommandLinePathOption.find(args, 2, "--plan");
        Path fallbackOutPath = PerformanceCommandLinePathOption.find(args, 2, "--out");
        PerformanceCommandReportOutput reportOutput = new PerformanceCommandReportOutput(
                PerformanceCommandPathValidator.refersToSameFile(fallbackPlanPath, fallbackOutPath)
                        ? null
                        : fallbackOutPath,
                err
        );
        try {
            PerformanceRunCliOptions options = PerformanceRunCliOptions.parse(args);
            if (options.isHelp()) {
                printUsage(out);
                return 0;
            }
            if (options.getPlanPath() == null) {
                throw new IllegalArgumentException("--plan is required");
            }
            fallbackPlanPath = options.getPlanPath();
            PerformanceCommandPathValidator.requireDistinctPlanAndOutput(
                    options.getPlanPath(),
                    options.getOutPath()
            );
            reportOutput = new PerformanceCommandReportOutput(options.getOutPath(), err);
            if (!Files.isRegularFile(options.getPlanPath())) {
                throw new IllegalArgumentException("Plan file does not exist: " + options.getPlanPath());
            }

            reportOutput.write(lifecycleReport(
                    PerformanceRunStatus.PENDING,
                    options.getPlanPath(),
                    commandStartTimeMs,
                    System.currentTimeMillis(),
                    ""
            ));

            runtimeBootstrap.init();
            PerformanceCommandReportOutput activeOutput = reportOutput;
            PerformanceRunExecutionResult result = executor.execute(
                    options.getPlanPath(),
                    out,
                    report -> {
                        activeOutput.writeProgress(report);
                        printProgress(out, report);
                    }
            );
            activeOutput.write(finalReport(result, options.getPlanPath(), commandStartTimeMs));
            printSummary(out, result);
            return result.isSuccess() ? 0 : 1;
        } catch (IllegalArgumentException ex) {
            reportOutput.writeFailure(lifecycleReport(
                    PerformanceRunStatus.FAILED,
                    fallbackPlanPath,
                    commandStartTimeMs,
                    System.currentTimeMillis(),
                    describe(ex)
            ), ex);
            err.println(ex.getMessage());
            printUsage(err);
            return 2;
        } catch (Exception ex) {
            reportOutput.writeFailure(lifecycleReport(
                    PerformanceRunStatus.FAILED,
                    fallbackPlanPath,
                    commandStartTimeMs,
                    System.currentTimeMillis(),
                    describe(ex)
            ), ex);
            err.println("Performance run failed: " + describe(ex));
            return 1;
        }
    }

    private static PerformanceJsonReport lifecycleReport(String status,
                                                          Path planPath,
                                                          long startTimeMs,
                                                          long endTimeMs,
                                                          String error) {
        return PerformanceCommandReportFactory.snapshot(
                "",
                "local",
                status,
                planPath == null ? "" : planPath.toString(),
                startTimeMs,
                endTimeMs,
                error,
                0L,
                0L,
                0L
        );
    }

    private static PerformanceJsonReport finalReport(PerformanceRunExecutionResult result,
                                                     Path planPath,
                                                     long startTimeMs) {
        if (result != null && result.getReport() != null) {
            return result.getReport();
        }
        long endTimeMs = System.currentTimeMillis();
        return PerformanceCommandReportFactory.snapshot(
                "",
                "local",
                result == null ? PerformanceRunStatus.FAILED : result.getStatus(),
                planPath == null ? "" : planPath.toString(),
                result == null ? startTimeMs : result.getStartTimeMs(),
                result == null ? endTimeMs : result.getEndTimeMs(),
                result == null ? "Performance run returned no result" : result.getError(),
                result == null ? 0L : result.getTotalRequests(),
                result == null ? 0L : result.getSuccessRequests(),
                result == null ? 0L : result.getFailedRequests()
        );
    }

    private static void printProgress(PrintStream out, PerformanceJsonReport report) {
        if (out == null || report == null) {
            return;
        }
        PerformanceJsonReportMetadata metadata = report.getMetadata();
        out.printf(
                "Performance run progress: status=%s total=%d success=%d failed=%d elapsedMs=%d%n",
                metadata.getStatus(),
                report.getSummary().getTotalRequests(),
                report.getSummary().getSuccessRequests(),
                report.getSummary().getFailedRequests(),
                metadata.getElapsedTimeMs()
        );
        out.flush();
    }

    private static void printSummary(PrintStream out, PerformanceRunExecutionResult result) {
        if (result == null) {
            out.println("Performance run completed: status=FAILED total=0 success=0 failed=0 elapsedMs=0");
            return;
        }
        out.printf(
                "Performance run completed: status=%s total=%d success=%d failed=%d elapsedMs=%d%n",
                result.getStatus(),
                result.getTotalRequests(),
                result.getSuccessRequests(),
                result.getFailedRequests(),
                result.getElapsedTimeMs()
        );
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: performance run --plan <plan.json> [--out <result.json>]");
    }

    private static String describe(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        return message == null || message.isBlank()
                ? failure == null ? "unknown error" : failure.getClass().getSimpleName()
                : message;
    }

    @FunctionalInterface
    public interface RuntimeBootstrap {
        void init() throws Exception;
    }
}
