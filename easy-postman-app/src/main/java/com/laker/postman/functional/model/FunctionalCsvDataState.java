package com.laker.postman.functional.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Functional persistence snapshot for CSV iteration data.
 */
public final class FunctionalCsvDataState {
    private final String sourceName;
    private final List<String> headers;
    private final List<Map<String, String>> rows;

    public FunctionalCsvDataState(String sourceName, List<String> headers, List<Map<String, String>> rows) {
        this.sourceName = sourceName;
        this.headers = copyHeaders(headers);
        this.rows = copyRows(rows);
    }

    public String getSourceName() {
        return sourceName;
    }

    public List<String> getHeaders() {
        return copyHeaders(headers);
    }

    public List<Map<String, String>> getRows() {
        return copyRows(rows);
    }

    private static List<String> copyHeaders(List<String> headers) {
        if (headers == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(headers);
    }

    private static List<Map<String, String>> copyRows(List<Map<String, String>> rows) {
        List<Map<String, String>> copiedRows = new ArrayList<>();
        if (rows == null) {
            return copiedRows;
        }
        for (Map<String, String> row : rows) {
            copiedRows.add(row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row));
        }
        return copiedRows;
    }
}
