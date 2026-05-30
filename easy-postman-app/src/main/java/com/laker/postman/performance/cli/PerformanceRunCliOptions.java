package com.laker.postman.performance.cli;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Value
public class PerformanceRunCliOptions {
    boolean help;
    Path planPath;
    Path outPath;

    @Builder
    public PerformanceRunCliOptions(Boolean help, Path planPath, Path outPath) {
        this.help = help != null && help;
        this.planPath = planPath;
        this.outPath = outPath;
    }

    public static PerformanceRunCliOptions parse(String[] args) {
        Path planPath = null;
        Path outPath = null;
        boolean help = false;
        String[] safeArgs = args == null ? new String[0] : args;
        for (int i = 2; i < safeArgs.length; i++) {
            String arg = safeArgs[i];
            if ("--help".equals(arg) || "-h".equals(arg)) {
                help = true;
                continue;
            }
            if ("--plan".equals(arg)) {
                planPath = Path.of(requiredValue(safeArgs, ++i, "--plan"));
                continue;
            }
            if ("--out".equals(arg)) {
                outPath = Path.of(requiredValue(safeArgs, ++i, "--out"));
                continue;
            }
            throw new IllegalArgumentException("Unknown option: " + arg);
        }
        return PerformanceRunCliOptions.builder()
                .help(help)
                .planPath(planPath)
                .outPath(outPath)
                .build();
    }

    private static String requiredValue(String[] args, int index, String optionName) {
        if (index >= args.length || args[index] == null || args[index].isBlank()) {
            throw new IllegalArgumentException(optionName + " requires a value");
        }
        return args[index];
    }
}
