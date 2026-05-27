package com.laker.postman.panel.performance.config;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class CsvDataSetData {
    private String sourceName;
    private List<String> headers = new ArrayList<>();
    private List<Map<String, String>> rows = new ArrayList<>();

    public CsvDataSetData(String sourceName, List<String> headers, List<Map<String, String>> rows) {
        this.sourceName = sourceName;
        setHeaders(headers);
        setRows(rows);
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
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
            return null;
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
