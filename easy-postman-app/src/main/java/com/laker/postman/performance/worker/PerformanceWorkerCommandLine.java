package com.laker.postman.performance.worker;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceWorkerCommandLine {

    public PerformanceWorkerOptions parse(String[] args) {
        PerformanceWorkerOptions.PerformanceWorkerOptionsBuilder builder = PerformanceWorkerOptions.builder();
        String[] safeArgs = args == null ? new String[0] : args;
        for (int i = 2; i < safeArgs.length; i++) {
            String arg = safeArgs[i];
            if ("--help".equals(arg) || "-h".equals(arg)) {
                builder.help(true);
                continue;
            }
            if ("--host".equals(arg)) {
                builder.host(requiredValue(safeArgs, ++i, "--host"));
                continue;
            }
            if ("--port".equals(arg)) {
                builder.port(parsePort(requiredValue(safeArgs, ++i, "--port")));
                continue;
            }
            if ("--progress-interval".equals(arg)) {
                builder.progressIntervalMs(parseProgressIntervalSeconds(requiredValue(
                        safeArgs,
                        ++i,
                        "--progress-interval"
                )));
                continue;
            }
            if ("--no-progress".equals(arg)) {
                builder.progressIntervalMs(0L);
                continue;
            }
            throw new IllegalArgumentException("Unknown option: " + arg);
        }
        return builder.build();
    }

    private String requiredValue(String[] args, int index, String optionName) {
        if (index >= args.length || args[index] == null || args[index].isBlank()) {
            throw new IllegalArgumentException(optionName + " requires a value");
        }
        return args[index];
    }

    private int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("--port must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("--port must be a number");
        }
    }

    private long parseProgressIntervalSeconds(String value) {
        try {
            long seconds = Long.parseLong(value);
            if (seconds < 0) {
                throw new IllegalArgumentException("--progress-interval must be >= 0");
            }
            return seconds * 1000L;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("--progress-interval must be a number");
        }
    }
}
