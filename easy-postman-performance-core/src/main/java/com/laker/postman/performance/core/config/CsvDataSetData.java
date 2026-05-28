package com.laker.postman.performance.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class CsvDataSetData {
    @Setter
    private String sourceName;
    private List<String> headers = new ArrayList<>();
    private List<Map<String, String>> rows = new ArrayList<>();

    public CsvDataSetData(String sourceName, List<String> headers, List<Map<String, String>> rows) {
        this.sourceName = sourceName;
        setHeaders(headers);
        setRows(rows);
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers == null ? new ArrayList<>() : new ArrayList<>(headers);
    }

    public void setRows(List<Map<String, String>> rows) {
        this.rows = copyRows(rows);
    }

    public boolean hasRows() {
        return rows != null && !rows.isEmpty();
    }

    public Map<String, String> rowForVirtualUser(int virtualUserIndex) {
        if (!hasRows()) {
            return Collections.emptyMap();
        }
        int rowIndex = Math.max(0, virtualUserIndex) % rows.size();
        return new LinkedHashMap<>(rows.get(rowIndex));
    }

    private static List<Map<String, String>> copyRows(List<Map<String, String>> rows) {
        List<Map<String, String>> copy = new ArrayList<>();
        if (rows == null) {
            return copy;
        }
        for (Map<String, String> row : rows) {
            copy.add(row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row));
        }
        return copy;
    }
}
