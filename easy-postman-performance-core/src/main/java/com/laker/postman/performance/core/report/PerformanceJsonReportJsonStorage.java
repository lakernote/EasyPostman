package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.run.PerformanceRunStatus;
import com.laker.postman.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PerformanceJsonReportJsonStorage {
    public static final String FORMAT_VERSION = "1.1";

    public void save(Path path, PerformanceJsonReport report) throws IOException {
        if (path == null) {
            return;
        }
        Path target = path.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent == null) {
            throw new IOException("Report path has no parent directory: " + path);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".easy-postman-report-", ".tmp");
        try {
            Files.writeString(temporary, toJson(report), StandardCharsets.UTF_8);
            moveReplacing(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public PerformanceJsonReport load(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path) || Files.size(path) == 0) {
            return null;
        }
        return fromJson(Files.readString(path, StandardCharsets.UTF_8));
    }

    public String toJson(PerformanceJsonReport report) {
        return JsonUtil.toJsonPrettyStr(toMap(report));
    }

    public PerformanceJsonReport fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return fromMap(objectMap(JsonUtil.convertValue(JsonUtil.readTree(json), Map.class)));
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Map<String, Object> toMap(PerformanceJsonReport report) {
        PerformanceJsonReport safeReport = report == null ? PerformanceJsonReport.builder().build() : report;
        PerformanceJsonReportMetadata metadata = safeReport.getMetadata();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", FORMAT_VERSION);
        root.put("runId", metadata.getRunId());
        root.put("source", metadata.getSource());
        root.put("status", metadata.getStatus());
        root.put("planPath", metadata.getPlanPath());
        root.put("startTimeMs", metadata.getStartTimeMs());
        root.put("endTimeMs", metadata.getEndTimeMs());
        root.put("elapsedTimeMs", metadata.getElapsedTimeMs());
        root.put("stopped", metadata.isStopped());
        root.put("error", metadata.getError());
        root.put("summary", summaryToMap(safeReport.getSummary()));
        root.put("protocols", protocolsToMap(safeReport.getProtocols()));
        return root;
    }

    private PerformanceJsonReport fromMap(Map<String, Object> root) {
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .runId(stringValue(root, "runId", ""))
                        .source(stringValue(root, "source", "local"))
                        .status(stringValue(root, "status", PerformanceRunStatus.SUCCESS))
                        .planPath(stringValue(root, "planPath", ""))
                        .startTimeMs(longValue(root, "startTimeMs", 0))
                        .endTimeMs(longValue(root, "endTimeMs", 0))
                        .elapsedTimeMs(longValue(root, "elapsedTimeMs", 0))
                        .stopped(booleanValue(root, "stopped", false))
                        .error(stringValue(root, "error", ""))
                        .build())
                .summary(readSummary(objectMap(root.get("summary"))))
                .protocols(readProtocols(objectMap(root.get("protocols"))))
                .build();
    }

    private Map<String, Object> summaryToMap(PerformanceJsonReportSummary summary) {
        PerformanceJsonReportSummary safeSummary = summary == null
                ? PerformanceJsonReportSummary.builder().build()
                : summary;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("totalRequests", safeSummary.getTotalRequests());
        json.put("successRequests", safeSummary.getSuccessRequests());
        json.put("failedRequests", safeSummary.getFailedRequests());
        json.put("successRate", safeSummary.getSuccessRate());
        return json;
    }

    private PerformanceJsonReportSummary readSummary(Map<String, Object> json) {
        return PerformanceJsonReportSummary.builder()
                .totalRequests(longValue(json, "totalRequests", 0))
                .successRequests(longValue(json, "successRequests", 0))
                .failedRequests(longValue(json, "failedRequests", 0))
                .successRate(doubleValue(json, "successRate", 0))
                .build();
    }

    private Map<String, Object> protocolsToMap(Map<String, PerformanceJsonReportProtocol> protocols) {
        Map<String, Object> json = new LinkedHashMap<>();
        if (protocols == null) {
            return json;
        }
        for (Map.Entry<String, PerformanceJsonReportProtocol> entry : protocols.entrySet()) {
            json.put(entry.getKey(), protocolToMap(entry.getValue()));
        }
        return json;
    }

    private Map<String, PerformanceJsonReportProtocol> readProtocols(Map<String, Object> json) {
        Map<String, PerformanceJsonReportProtocol> protocols = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            protocols.put(entry.getKey(), readProtocol(objectMap(entry.getValue())));
        }
        return protocols;
    }

    private Map<String, Object> protocolToMap(PerformanceJsonReportProtocol protocol) {
        PerformanceJsonReportProtocol safeProtocol = protocol == null
                ? PerformanceJsonReportProtocol.builder().build()
                : protocol;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("protocol", safeProtocol.getProtocol());
        json.put("total", apiToMap(safeProtocol.getTotal()));
        json.put("apis", apisToList(safeProtocol.getApis()));
        return json;
    }

    private PerformanceJsonReportProtocol readProtocol(Map<String, Object> json) {
        return PerformanceJsonReportProtocol.builder()
                .protocol(stringValue(json, "protocol", ""))
                .total(readApi(objectMap(json.get("total"))))
                .apis(readApis(listValue(json.get("apis"))))
                .build();
    }

    private List<Map<String, Object>> apisToList(List<PerformanceJsonReportApi> apis) {
        List<Map<String, Object>> values = new ArrayList<>();
        if (apis == null) {
            return values;
        }
        for (PerformanceJsonReportApi api : apis) {
            values.add(apiToMap(api));
        }
        return values;
    }

    private List<PerformanceJsonReportApi> readApis(List<Object> values) {
        List<PerformanceJsonReportApi> apis = new ArrayList<>();
        for (Object value : values) {
            apis.add(readApi(objectMap(value)));
        }
        return apis;
    }

    private Map<String, Object> apiToMap(PerformanceJsonReportApi api) {
        PerformanceJsonReportApi safeApi = api == null ? PerformanceJsonReportApi.builder().build() : api;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("apiId", safeApi.getApiId());
        json.put("name", safeApi.getName());
        json.put("protocol", safeApi.getProtocol());
        json.put("total", safeApi.getTotal());
        json.put("success", safeApi.getSuccess());
        json.put("failed", safeApi.getFailed());
        json.put("successRate", safeApi.getSuccessRate());
        json.put("samplesPerSecond", safeApi.getSamplesPerSecond());
        json.put("firstSampleStartTimeMs", safeApi.getFirstSampleStartTimeMs());
        json.put("lastSampleEndTimeMs", safeApi.getLastSampleEndTimeMs());
        json.put("durationMs", durationToMap(safeApi.getDurationMs()));
        json.put("bytes", bytesToMap(safeApi.getBytes()));
        json.put("stream", streamToMap(safeApi.getStream()));
        json.put("firstMessageLatencyMs", durationToMap(safeApi.getFirstMessageLatencyMs()));
        return json;
    }

    private PerformanceJsonReportApi readApi(Map<String, Object> json) {
        return PerformanceJsonReportApi.builder()
                .apiId(stringValue(json, "apiId", ""))
                .name(stringValue(json, "name", ""))
                .protocol(stringValue(json, "protocol", ""))
                .total(longValue(json, "total", 0))
                .success(longValue(json, "success", 0))
                .failed(longValue(json, "failed", 0))
                .successRate(doubleValue(json, "successRate", 0))
                .samplesPerSecond(doubleValue(json, "samplesPerSecond", 0))
                .firstSampleStartTimeMs(longValue(json, "firstSampleStartTimeMs", 0))
                .lastSampleEndTimeMs(longValue(json, "lastSampleEndTimeMs", 0))
                .durationMs(readDuration(objectMap(json.get("durationMs"))))
                .bytes(readBytes(objectMap(json.get("bytes"))))
                .stream(readStream(objectMap(json.get("stream"))))
                .firstMessageLatencyMs(readDuration(objectMap(json.get("firstMessageLatencyMs"))))
                .build();
    }

    private Map<String, Object> durationToMap(PerformanceJsonReportDuration duration) {
        PerformanceJsonReportDuration safeDuration = duration == null
                ? PerformanceJsonReportDuration.builder().build()
                : duration;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("avg", safeDuration.getAvg());
        json.put("min", safeDuration.getMin());
        json.put("max", safeDuration.getMax());
        json.put("p90", safeDuration.getP90());
        json.put("p95", safeDuration.getP95());
        json.put("p99", safeDuration.getP99());
        return json;
    }

    private PerformanceJsonReportDuration readDuration(Map<String, Object> json) {
        return PerformanceJsonReportDuration.builder()
                .avg(longValue(json, "avg", 0))
                .min(longValue(json, "min", 0))
                .max(longValue(json, "max", 0))
                .p90(longValue(json, "p90", 0))
                .p95(longValue(json, "p95", 0))
                .p99(longValue(json, "p99", 0))
                .build();
    }

    private Map<String, Object> streamToMap(PerformanceJsonReportStream stream) {
        PerformanceJsonReportStream safeStream = stream == null
                ? PerformanceJsonReportStream.builder().build()
                : stream;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("sentMessages", safeStream.getSentMessages());
        json.put("receivedMessages", safeStream.getReceivedMessages());
        json.put("matchedMessages", safeStream.getMatchedMessages());
        json.put("sendRate", safeStream.getSendRate());
        json.put("receiveRate", safeStream.getReceiveRate());
        json.put("matchedRate", safeStream.getMatchedRate());
        return json;
    }

    private PerformanceJsonReportStream readStream(Map<String, Object> json) {
        return PerformanceJsonReportStream.builder()
                .sentMessages(longValue(json, "sentMessages", 0))
                .receivedMessages(longValue(json, "receivedMessages", 0))
                .matchedMessages(longValue(json, "matchedMessages", 0))
                .sendRate(doubleValue(json, "sendRate", 0))
                .receiveRate(doubleValue(json, "receiveRate", 0))
                .matchedRate(doubleValue(json, "matchedRate", 0))
                .build();
    }

    private Map<String, Object> bytesToMap(PerformanceJsonReportBytes bytes) {
        PerformanceJsonReportBytes safeBytes = bytes == null
                ? PerformanceJsonReportBytes.builder().build()
                : bytes;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("sentBytes", safeBytes.getSentBytes());
        json.put("receivedBytes", safeBytes.getReceivedBytes());
        json.put("sentBytesPerSecond", safeBytes.getSentBytesPerSecond());
        json.put("receivedBytesPerSecond", safeBytes.getReceivedBytesPerSecond());
        json.put("avgReceivedBytes", safeBytes.getAvgReceivedBytes());
        return json;
    }

    private PerformanceJsonReportBytes readBytes(Map<String, Object> json) {
        return PerformanceJsonReportBytes.builder()
                .sentBytes(longValue(json, "sentBytes", 0))
                .receivedBytes(longValue(json, "receivedBytes", 0))
                .sentBytesPerSecond(doubleValue(json, "sentBytesPerSecond", 0))
                .receivedBytesPerSecond(doubleValue(json, "receivedBytesPerSecond", 0))
                .avgReceivedBytes(longValue(json, "avgReceivedBytes", 0))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> listValue(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of();
    }

    private String stringValue(Map<String, Object> json, String key, String defaultValue) {
        Object value = json.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private boolean booleanValue(Map<String, Object> json, String key, boolean defaultValue) {
        Object value = json.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return defaultValue;
    }

    private long longValue(Map<String, Object> json, String key, long defaultValue) {
        Object value = json.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double doubleValue(Map<String, Object> json, String key, double defaultValue) {
        Object value = json.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
