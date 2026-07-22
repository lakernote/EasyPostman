package com.laker.postman.functional.cli;

import com.laker.postman.workspace.cli.WorkspaceRunOptions;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Value
public class FunctionalRunCliOptions {
    boolean help;
    String workspace;
    String environment;
    Path iterationDataPath;
    Integer iterationCount;
    Path workingDirectory;
    Path outPath;
    boolean bail;

    @Builder
    public FunctionalRunCliOptions(Boolean help,
                                   String workspace,
                                   String environment,
                                   Path iterationDataPath,
                                   Integer iterationCount,
                                   Path workingDirectory,
                                   Path outPath,
                                   Boolean bail) {
        this.help = help != null && help;
        this.workspace = workspace;
        this.environment = environment;
        this.iterationDataPath = iterationDataPath;
        this.iterationCount = iterationCount;
        this.workingDirectory = workingDirectory;
        this.outPath = outPath;
        this.bail = bail != null && bail;
    }

    public static FunctionalRunCliOptions parse(String[] args) {
        String[] safeArgs = args == null ? new String[0] : args;
        boolean help = false;
        String workspace = null;
        String environment = null;
        Path iterationDataPath = null;
        Integer iterationCount = null;
        Path workingDirectory = null;
        Path outPath = null;
        boolean bail = false;

        int index = 2;
        if (index < safeArgs.length && !safeArgs[index].startsWith("-")) {
            workspace = safeArgs[index++];
        }
        while (index < safeArgs.length) {
            String arg = safeArgs[index++];
            switch (arg) {
                case "--help", "-h" -> help = true;
                case "--workspace", "-w" -> {
                    if (workspace != null) {
                        throw new IllegalArgumentException("Workspace may be specified only once");
                    }
                    workspace = requiredValue(safeArgs, index++, arg);
                }
                case "--environment", "-e" -> environment = requiredValue(safeArgs, index++, arg);
                case "--iteration-data", "-d" -> iterationDataPath = Path.of(requiredValue(safeArgs, index++, arg));
                case "--iteration-count", "-n" -> iterationCount = parsePositiveInt(
                        requiredValue(safeArgs, index++, arg),
                        arg
                );
                case "--working-dir" -> workingDirectory = Path.of(requiredValue(safeArgs, index++, arg));
                case "--out" -> outPath = Path.of(requiredValue(safeArgs, index++, arg));
                case "--bail" -> bail = true;
                default -> throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }
        if (!help && (workspace == null || workspace.isBlank())) {
            throw new IllegalArgumentException("Workspace directory is required");
        }

        return FunctionalRunCliOptions.builder()
                .help(help)
                .workspace(workspace)
                .environment(environment)
                .iterationDataPath(iterationDataPath)
                .iterationCount(iterationCount)
                .workingDirectory(workingDirectory)
                .outPath(outPath)
                .bail(bail)
                .build();
    }

    WorkspaceRunOptions toRunOptions() {
        return WorkspaceRunOptions.builder()
                .workspace(workspace)
                .environment(environment)
                .iterationDataPath(iterationDataPath)
                .iterationCount(iterationCount)
                .workingDirectory(workingDirectory)
                .bail(bail)
                .build();
    }

    private static String requiredValue(String[] args, int index, String optionName) {
        if (index >= args.length
                || args[index] == null
                || args[index].isBlank()
                || isKnownOption(args[index])) {
            throw new IllegalArgumentException(optionName + " requires a value");
        }
        return args[index];
    }

    private static boolean isKnownOption(String value) {
        return switch (value) {
            case "--help", "-h",
                 "--workspace", "-w",
                 "--environment", "-e",
                 "--iteration-data", "-d",
                 "--iteration-count", "-n",
                 "--working-dir", "--out", "--bail" -> true;
            default -> false;
        };
    }

    private static int parsePositiveInt(String value, String optionName) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(optionName + " must be positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(optionName + " must be a number");
        }
    }
}
