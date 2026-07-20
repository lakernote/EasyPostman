package com.laker.postman.performance.master;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import com.laker.postman.performance.output.PerformanceCommandLinePathOption;
import com.laker.postman.performance.output.PerformanceCommandPathValidator;
import com.laker.postman.performance.output.PerformanceCommandReportFactory;
import com.laker.postman.performance.output.PerformanceCommandReportOutput;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMasterRunCommand {
    private static final long CONSOLE_PROGRESS_INTERVAL_MS = 1_000L;

    private final PerformanceMasterRunExecutor executor;

    public PerformanceMasterRunCommand() {
        this(new PerformanceMasterRunExecutor());
    }

    PerformanceMasterRunCommand(PerformanceMasterRunExecutor executor) {
        this.executor = executor == null ? new PerformanceMasterRunExecutor() : executor;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        long commandStartTimeMs = System.currentTimeMillis();
        Path fallbackPlanPath = PerformanceCommandLinePathOption.find(args, 3, "--plan");
        Path fallbackOutPath = PerformanceCommandLinePathOption.find(args, 3, "--out");
        PerformanceCommandReportOutput reportOutput = new PerformanceCommandReportOutput(
                PerformanceCommandPathValidator.refersToSameFile(fallbackPlanPath, fallbackOutPath)
                        ? null
                        : fallbackOutPath,
                err
        );
        try {
            PerformanceMasterOptions options = PerformanceMasterOptions.parse(args);
            if (options.isHelp()) {
                printUsage(out);
                return 0;
            }
            if (options.getPlanPath() == null) {
                throw new IllegalArgumentException("--plan is required");
            }
            if (options.getWorkers().isEmpty()) {
                throw new IllegalArgumentException("--workers is required");
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

            PerformanceCommandReportOutput activeOutput = reportOutput;
            AtomicLong lastConsoleProgressTimeMs = new AtomicLong(0L);
            PerformanceJsonReport report = executor.execute(options, progress -> {
                activeOutput.writeProgress(progress.report());
                printProgress(out, progress, lastConsoleProgressTimeMs);
            });
            activeOutput.write(report);
            out.printf(
                    "Performance master run completed: status=%s workers=%d total=%d success=%d failed=%d elapsedMs=%d%n",
                    report.getMetadata().getStatus(),
                    options.getWorkers().size(),
                    report.getSummary().getTotalRequests(),
                    report.getSummary().getSuccessRequests(),
                    report.getSummary().getFailedRequests(),
                    report.getMetadata().getElapsedTimeMs()
            );
            return PerformanceRunStatus.SUCCESS.equals(report.getMetadata().getStatus()) ? 0 : 1;
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
            err.println("Performance master run failed: " + describe(ex));
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
                "master",
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

    private static void printProgress(PrintStream out,
                                      PerformanceMasterRunProgress progress,
                                      AtomicLong lastPrintTimeMs) {
        if (out == null || progress == null || progress.report() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long previous = lastPrintTimeMs.get();
        if (previous > 0L && now - previous < CONSOLE_PROGRESS_INTERVAL_MS) {
            return;
        }
        lastPrintTimeMs.set(now);
        out.printf(
                "Performance master progress: workers=%d/%d users=%d/%d total=%d success=%d failed=%d qps=%.2f%n",
                progress.completedWorkers(),
                progress.totalWorkers(),
                progress.activeUsers(),
                progress.totalUsers(),
                progress.report().getSummary().getTotalRequests(),
                progress.report().getSummary().getSuccessRequests(),
                progress.report().getSummary().getFailedRequests(),
                progress.qps()
        );
        out.flush();
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: performance master run --plan <plan.json> --workers host:port[,host:port] [--out <result.json>] [--timeout-sec <seconds>] [--poll-interval-ms <ms>]");
    }

    private static String describe(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
