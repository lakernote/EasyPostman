package com.laker.postman.performance.master;

import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpointParser;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.util.List;

@Value
public class PerformanceMasterOptions {
    boolean help;
    Path planPath;
    Path outPath;
    List<PerformanceWorkerEndpoint> workers;
    long timeoutMs;
    long pollIntervalMs;

    @Builder
    public PerformanceMasterOptions(Boolean help,
                                    Path planPath,
                                    Path outPath,
                                    List<PerformanceWorkerEndpoint> workers,
                                    Long timeoutMs,
                                    Long pollIntervalMs) {
        this.help = help != null && help;
        this.planPath = planPath;
        this.outPath = outPath;
        this.workers = workers == null ? List.of() : List.copyOf(workers);
        this.timeoutMs = Math.max(1_000L, timeoutMs == null ? 86_400_000L : timeoutMs);
        this.pollIntervalMs = Math.max(50L, pollIntervalMs == null ? 500L : pollIntervalMs);
    }

    public static PerformanceMasterOptions parse(String[] args) {
        boolean help = false;
        Path planPath = null;
        Path outPath = null;
        List<PerformanceWorkerEndpoint> workers = List.of();
        long timeoutMs = 86_400_000L;
        long pollIntervalMs = 500L;
        String[] safeArgs = args == null ? new String[0] : args;
        for (int i = 3; i < safeArgs.length; i++) {
            String arg = safeArgs[i];
            if ("--help".equals(arg) || "-h".equals(arg)) {
                help = true;
                continue;
            }
            if ("--plan".equals(arg)) {
                planPath = Path.of(requiredValue(safeArgs, ++i, "--plan"));
                continue;
            }
            if ("--workers".equals(arg)) {
                workers = PerformanceWorkerEndpointParser.parse(requiredValue(safeArgs, ++i, "--workers"));
                continue;
            }
            if ("--out".equals(arg)) {
                outPath = Path.of(requiredValue(safeArgs, ++i, "--out"));
                continue;
            }
            if ("--timeout-sec".equals(arg)) {
                timeoutMs = parsePositiveLong(requiredValue(safeArgs, ++i, "--timeout-sec"), "--timeout-sec") * 1_000L;
                continue;
            }
            if ("--poll-interval-ms".equals(arg)) {
                pollIntervalMs = parsePositiveLong(requiredValue(safeArgs, ++i, "--poll-interval-ms"), "--poll-interval-ms");
                continue;
            }
            throw new IllegalArgumentException("Unknown option: " + arg);
        }
        return PerformanceMasterOptions.builder()
                .help(help)
                .planPath(planPath)
                .outPath(outPath)
                .workers(workers)
                .timeoutMs(timeoutMs)
                .pollIntervalMs(pollIntervalMs)
                .build();
    }

    private static String requiredValue(String[] args, int index, String optionName) {
        if (index >= args.length || args[index] == null || args[index].isBlank()) {
            throw new IllegalArgumentException(optionName + " requires a value");
        }
        return args[index];
    }

    private static long parsePositiveLong(String value, String optionName) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(optionName + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(optionName + " must be a number");
        }
    }
}
