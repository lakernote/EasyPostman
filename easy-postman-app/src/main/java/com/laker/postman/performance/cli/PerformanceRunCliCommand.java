package com.laker.postman.performance.cli;

import com.laker.postman.startup.HeadlessStartupBootstrap;
import com.laker.postman.performance.runtime.PerformanceRunExecutionResult;
import com.laker.postman.performance.runtime.PerformanceRunPlanExecutor;

import java.io.PrintStream;

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
        try {
            PerformanceRunCliOptions options = PerformanceRunCliOptions.parse(args);
            if (options.isHelp()) {
                printUsage(out);
                return 0;
            }
            if (options.getPlanPath() == null) {
                err.println("--plan is required");
                printUsage(err);
                return 2;
            }

            runtimeBootstrap.init();
            PerformanceRunExecutionResult result = executor.execute(options.getPlanPath(), out);
            if (options.getOutPath() != null) {
                new PerformanceRunResultJsonStorage().save(options.getOutPath(), result);
            }
            printSummary(out, result);
            return result.isSuccess() ? 0 : 1;
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            printUsage(err);
            return 2;
        } catch (Exception ex) {
            err.println("Performance run failed: " + ex.getMessage());
            return 1;
        }
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

    @FunctionalInterface
    public interface RuntimeBootstrap {
        void init() throws Exception;
    }
}
