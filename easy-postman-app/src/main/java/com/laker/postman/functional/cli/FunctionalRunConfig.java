package com.laker.postman.functional.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record FunctionalRunConfig(
        List<String> requestIds,
        List<Map<String, String>> iterationData,
        String iterationDataSource
) {
    FunctionalRunConfig {
        requestIds = requestIds == null ? List.of() : List.copyOf(requestIds);
        iterationData = copyRows(iterationData);
        iterationDataSource = iterationDataSource == null ? "" : iterationDataSource;
    }

    private static List<Map<String, String>> copyRows(List<Map<String, String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> copied = new ArrayList<>();
        for (Map<String, String> row : rows) {
            copied.add(row == null ? Map.of() : new LinkedHashMap<>(row));
        }
        return List.copyOf(copied);
    }
}
