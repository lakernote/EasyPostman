package com.laker.postman.performance.output;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@UtilityClass
public class PerformanceCommandPathValidator {

    public boolean refersToSameFile(Path firstPath, Path secondPath) {
        if (firstPath == null || secondPath == null) {
            return false;
        }
        if (firstPath.toAbsolutePath().normalize().equals(secondPath.toAbsolutePath().normalize())) {
            return true;
        }
        try {
            return Files.isSameFile(firstPath, secondPath);
        } catch (NoSuchFileException ignored) {
            return false;
        } catch (IOException | SecurityException ignored) {
            return true;
        }
    }

    public void requireDistinctPlanAndOutput(Path planPath, Path outPath) {
        if (refersToSameFile(planPath, outPath)) {
            throw new IllegalArgumentException("--out must not point to the plan file: " + outPath);
        }
    }
}
