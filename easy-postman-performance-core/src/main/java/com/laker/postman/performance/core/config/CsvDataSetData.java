package com.laker.postman.performance.core.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
    @Getter(lombok.AccessLevel.NONE)
    private volatile boolean fileRowsLoaded;

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
        ensureFileRowsLoaded();
        if (!hasRows()) {
            return Collections.emptyMap();
        }
        int rowIndex = Math.max(0, virtualUserIndex) % rows.size();
        return new LinkedHashMap<>(rows.get(rowIndex));
    }

    private void ensureFileRowsLoaded() {
        if (hasRows() || !hasFileReference() || fileRowsLoaded) {
            return;
        }
        synchronized (this) {
            if (hasRows() || !hasFileReference() || fileRowsLoaded) {
                return;
            }
            try {
                CsvTextData textData = parseCsvText(Files.readString(Path.of(filePath), charset()));
                List<String> resolvedHeaders = resolveHeaders(textData);
                setHeaders(resolvedHeaders);
                setRows(toRows(resolvedHeaders, textData));
            } catch (Exception ex) {
                log.warn("Failed to load performance CSV file: {}", filePath, ex);
            } finally {
                fileRowsLoaded = true;
            }
        }
    }

    private Charset charset() {
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }

    private List<String> resolveHeaders(CsvTextData textData) {
        if (hasHeader && !textData.records().isEmpty()) {
            return new ArrayList<>(textData.records().get(0));
        }
        if (headers != null && !headers.isEmpty()) {
            return new ArrayList<>(headers);
        }
        int columnCount = textData.records().isEmpty() ? 0 : textData.records().get(0).size();
        List<String> generatedHeaders = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            generatedHeaders.add("column" + (i + 1));
        }
        return generatedHeaders;
    }

    private List<Map<String, String>> toRows(List<String> resolvedHeaders, CsvTextData textData) {
        List<Map<String, String>> parsedRows = new ArrayList<>();
        int firstDataRow = hasHeader ? 1 : 0;
        for (int i = firstDataRow; i < textData.records().size(); i++) {
            List<String> record = textData.records().get(i);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < resolvedHeaders.size(); j++) {
                String key = resolvedHeaders.get(j) == null ? "" : resolvedHeaders.get(j).trim();
                if (!key.isEmpty()) {
                    row.put(key, j < record.size() ? record.get(j) : "");
                }
            }
            parsedRows.add(row);
        }
        return parsedRows;
    }

    private CsvTextData parseCsvText(String content) {
        if (content == null || content.isBlank()) {
            return new CsvTextData(List.of());
        }
        char separator = delimiter == null || delimiter.isEmpty() ? ',' : delimiter.charAt(0);
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(ch);
                }
                continue;
            }
            if (ch == '"') {
                quoted = true;
            } else if (ch == separator) {
                record.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n') {
                record.add(field.toString());
                records.add(record);
                record = new ArrayList<>();
                field.setLength(0);
            } else if (ch != '\r') {
                field.append(ch);
            }
        }
        if (!record.isEmpty() || field.length() > 0) {
            record.add(field.toString());
            records.add(record);
        }
        return new CsvTextData(records);
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

    private record CsvTextData(List<List<String>> records) {
    }
}
