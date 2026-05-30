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
    public static final String SOURCE_INLINE = "INLINE";
    public static final String SOURCE_FILE = "FILE";
    public static final String SHARING_THREAD_GROUP = "THREAD_GROUP";
    public static final String SHARING_ALL_THREADS = "ALL_THREADS";
    public static final String EOF_RECYCLE = "RECYCLE";
    public static final String EOF_STOP_THREAD = "STOP_THREAD";

    @Setter
    private String sourceName;
    @Setter
    private String sourceType = SOURCE_INLINE;
    @Setter
    private String filePath;
    @Setter
    private String encoding = "UTF-8";
    @Setter
    private String delimiter = ",";
    @Setter
    private boolean hasHeader = true;
    @Setter
    private String sharingMode = SHARING_THREAD_GROUP;
    @Setter
    private String eofMode = EOF_RECYCLE;
    private List<String> headers = new ArrayList<>();
    private List<Map<String, String>> rows = new ArrayList<>();

    public CsvDataSetData(String sourceName, List<String> headers, List<Map<String, String>> rows) {
        this.sourceName = sourceName;
        setHeaders(headers);
        setRows(rows);
    }

    public static CsvDataSetData file(String sourceName, String filePath) {
        CsvDataSetData data = new CsvDataSetData();
        data.setSourceName(sourceName);
        data.setSourceType(SOURCE_FILE);
        data.setFilePath(filePath);
        return data;
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

    public boolean isFileSource() {
        return SOURCE_FILE.equalsIgnoreCase(sourceType);
    }

    public boolean hasFileReference() {
        return isFileSource() && filePath != null && !filePath.isBlank();
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
