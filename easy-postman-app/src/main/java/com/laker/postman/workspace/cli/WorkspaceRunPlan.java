package com.laker.postman.workspace.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WorkspaceRunPlan(
        List<WorkspaceRunSelectedRequest> requests,
        List<String> collectionNames,
        List<Map<String, String>> embeddedIterationData,
        String selectionMode,
        String embeddedIterationDataSource
) {
    public WorkspaceRunPlan {
        requests = requests == null ? List.of() : List.copyOf(requests);
        collectionNames = collectionNames == null ? List.of() : List.copyOf(collectionNames);
        embeddedIterationData = copyRows(embeddedIterationData);
        selectionMode = selectionMode == null ? "" : selectionMode;
        embeddedIterationDataSource = embeddedIterationDataSource == null
                ? "<none>"
                : embeddedIterationDataSource;
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
