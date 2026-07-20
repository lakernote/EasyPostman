package com.laker.postman.performance.output;

import lombok.experimental.UtilityClass;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@UtilityClass
public class PerformanceCommandLinePathOption {

    public Path find(String[] args, int startIndex, String optionName) {
        if (args == null || optionName == null || optionName.isBlank()) {
            return null;
        }
        Path result = null;
        for (int i = Math.max(0, startIndex); i < args.length - 1; i++) {
            if (!optionName.equals(args[i]) || args[i + 1] == null || args[i + 1].isBlank()) {
                continue;
            }
            try {
                result = Path.of(args[++i]);
            } catch (InvalidPathException ignored) {
                return null;
            }
        }
        return result;
    }
}
