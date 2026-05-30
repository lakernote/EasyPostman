package com.laker.postman.performance.master;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportJsonStorage;
import com.laker.postman.performance.core.run.PerformanceRunStatus;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PerformanceMasterRunCommand {
    private final PerformanceMasterRunExecutor executor;
    private final PerformanceJsonReportJsonStorage reportJsonStorage = new PerformanceJsonReportJsonStorage();

    public PerformanceMasterRunCommand() {
        this(new PerformanceMasterRunExecutor());
    }

    PerformanceMasterRunCommand(PerformanceMasterRunExecutor executor) {
        this.executor = executor == null ? new PerformanceMasterRunExecutor() : executor;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            PerformanceMasterOptions options = PerformanceMasterOptions.parse(args);
            if (options.isHelp()) {
                printUsage(out);
                return 0;
            }
            if (options.getPlanPath() == null) {
                err.println("--plan is required");
                printUsage(err);
                return 2;
            }
            if (options.getWorkers().isEmpty()) {
                err.println("--workers is required");
                printUsage(err);
                return 2;
            }
            PerformanceJsonReport report = executor.execute(options);
            if (options.getOutPath() != null) {
                save(options.getOutPath(), report);
            }
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
            err.println(ex.getMessage());
            printUsage(err);
            return 2;
        } catch (Exception ex) {
            err.println("Performance master run failed: " + describe(ex));
            return 1;
        }
    }

    private void save(Path path, PerformanceJsonReport report) throws Exception {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, reportJsonStorage.toJson(report), StandardCharsets.UTF_8);
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage: performance master run --plan <plan.json> --workers host:port[,host:port] [--out <result.json>] [--timeout-sec <seconds>] [--poll-interval-ms <ms>]");
    }

    private static String describe(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
