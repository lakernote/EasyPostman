package com.laker.postman.panel.performance.plan;


import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Value
public class PerformanceCsvState {
    String sourceName;
    List<String> headers;
    List<Map<String, String>> rows;

    public PerformanceCsvState(String sourceName, List<String> headers, List<Map<String, String>> rows) {
        this.sourceName = sourceName;
        this.headers = copyHeaders(headers);
        this.rows = copyRows(rows);
    }

    private static List<String> copyHeaders(List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(headers));
    }

    private static List<Map<String, String>> copyRows(List<Map<String, String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> copiedRows = new ArrayList<>(rows.size());
        for (Map<String, String> row : rows) {
            copiedRows.add(Collections.unmodifiableMap(new LinkedHashMap<>(row == null ? Map.of() : row)));
        }
        return Collections.unmodifiableList(copiedRows);
    }
}
