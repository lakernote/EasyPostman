package com.laker.postman.collection.cli;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Value
public class CollectionRunCliOptions {
    boolean help;
    Path collectionPath;
    Path environmentPath;
    Path globalsPath;
    Path iterationDataPath;
    Integer iterationCount;
    List<String> folders;
    Path workingDirectory;
    Path outPath;
    boolean bail;

    @Builder
    public CollectionRunCliOptions(Boolean help,
                                   Path collectionPath,
                                   Path environmentPath,
                                   Path globalsPath,
                                   Path iterationDataPath,
                                   Integer iterationCount,
                                   List<String> folders,
                                   Path workingDirectory,
                                   Path outPath,
                                   Boolean bail) {
        this.help = help != null && help;
        this.collectionPath = collectionPath;
        this.environmentPath = environmentPath;
        this.globalsPath = globalsPath;
        this.iterationDataPath = iterationDataPath;
        this.iterationCount = iterationCount;
        this.folders = folders == null ? List.of() : List.copyOf(folders);
        this.workingDirectory = workingDirectory;
        this.outPath = outPath;
        this.bail = bail != null && bail;
    }

    public static CollectionRunCliOptions parse(String[] args) {
        String[] safeArgs = args == null ? new String[0] : args;
        boolean help = false;
        Path collectionPath = null;
        Path environmentPath = null;
        Path globalsPath = null;
        Path iterationDataPath = null;
        Integer iterationCount = null;
        List<String> folders = new ArrayList<>();
        Path workingDirectory = null;
        Path outPath = null;
        boolean bail = false;

        int index = 2;
        if (index < safeArgs.length && !safeArgs[index].startsWith("-")) {
            collectionPath = Path.of(safeArgs[index++]);
        }
        while (index < safeArgs.length) {
            String arg = safeArgs[index++];
            switch (arg) {
                case "--help", "-h" -> help = true;
                case "--environment", "-e" -> environmentPath = Path.of(requiredValue(safeArgs, index++, arg));
                case "--globals", "-g" -> globalsPath = Path.of(requiredValue(safeArgs, index++, arg));
                case "--iteration-data", "-d" -> iterationDataPath = Path.of(requiredValue(safeArgs, index++, arg));
                case "--iteration-count", "-n" -> iterationCount = parsePositiveInt(
                        requiredValue(safeArgs, index++, arg),
                        arg
                );
                case "--folder" -> folders.add(requiredValue(safeArgs, index++, arg));
                case "--working-dir" -> workingDirectory = Path.of(requiredValue(safeArgs, index++, arg));
                case "--out" -> outPath = Path.of(requiredValue(safeArgs, index++, arg));
                case "--bail" -> bail = true;
                default -> throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }

        return CollectionRunCliOptions.builder()
                .help(help)
                .collectionPath(collectionPath)
                .environmentPath(environmentPath)
                .globalsPath(globalsPath)
                .iterationDataPath(iterationDataPath)
                .iterationCount(iterationCount)
                .folders(folders)
                .workingDirectory(workingDirectory)
                .outPath(outPath)
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
                 "--environment", "-e",
                 "--globals", "-g",
                 "--iteration-data", "-d",
                 "--iteration-count", "-n",
                 "--folder", "--working-dir", "--out", "--bail" -> true;
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
