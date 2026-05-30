package com.laker.postman.performance.core.worker;

import com.laker.postman.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PerformanceWorkerAssignmentJsonStorage {
    public static final String FORMAT_VERSION = "1.0";

    public void save(Path path, PerformanceWorkerAssignment assignment) throws IOException {
        if (path == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toJson(assignment), StandardCharsets.UTF_8);
    }

    public PerformanceWorkerAssignment load(Path path) throws IOException {
        if (path == null || !Files.exists(path) || Files.size(path) == 0) {
            return null;
        }
        return fromJson(Files.readString(path, StandardCharsets.UTF_8));
    }

    public String toJson(PerformanceWorkerAssignment assignment) {
        return JsonUtil.toJsonPrettyStr(toMap(assignment));
    }

    public PerformanceWorkerAssignment fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        Map<String, Object> root = objectMap(JsonUtil.convertValue(JsonUtil.readTree(json), Map.class));
        return PerformanceWorkerAssignment.builder()
                .schemaVersion(stringValue(root, "schemaVersion", FORMAT_VERSION))
                .runId(stringValue(root, "runId", ""))
                .workerId(stringValue(root, "workerId", ""))
                .assignmentId(stringValue(root, "assignmentId", ""))
                .endpoint(readEndpoint(objectMap(root.get("endpoint"))))
                .threadGroups(readThreadGroups(listValue(root.get("threadGroups"))))
                .build();
    }

    private Map<String, Object> toMap(PerformanceWorkerAssignment assignment) {
        PerformanceWorkerAssignment safeAssignment = assignment == null
                ? PerformanceWorkerAssignment.builder().build()
                : assignment;
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", safeAssignment.getSchemaVersion());
        root.put("runId", safeAssignment.getRunId());
        root.put("workerId", safeAssignment.getWorkerId());
        root.put("assignmentId", safeAssignment.getAssignmentId());
        root.put("endpoint", endpointToMap(safeAssignment.getEndpoint()));
        root.put("threadGroups", threadGroupsToList(safeAssignment.getThreadGroups()));
        return root;
    }

    private Map<String, Object> endpointToMap(PerformanceWorkerEndpoint endpoint) {
        if (endpoint == null) {
            return null;
        }
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("host", endpoint.getHost());
        json.put("port", endpoint.getPort());
        return json;
    }

    private List<Map<String, Object>> threadGroupsToList(List<PerformanceWorkerThreadGroupAssignment> threadGroups) {
        List<Map<String, Object>> array = new ArrayList<>();
        if (threadGroups == null) {
            return array;
        }
        for (PerformanceWorkerThreadGroupAssignment threadGroup : threadGroups) {
            if (threadGroup == null) {
                continue;
            }
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("threadGroupPath", threadGroup.getThreadGroupPath());
            json.put("threadGroupIndex", threadGroup.getThreadGroupIndex());
            json.put("firstVirtualUserIndex", threadGroup.getFirstVirtualUserIndex());
            json.put("virtualUserCount", threadGroup.getVirtualUserCount());
            array.add(json);
        }
        return array;
    }

    private PerformanceWorkerEndpoint readEndpoint(Map<String, Object> json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return new PerformanceWorkerEndpoint(
                stringValue(json, "host", "127.0.0.1"),
                intValue(json, "port", 0)
        );
    }

    private List<PerformanceWorkerThreadGroupAssignment> readThreadGroups(List<Object> values) {
        List<PerformanceWorkerThreadGroupAssignment> threadGroups = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> json = objectMap(value);
            if (json.isEmpty()) {
                continue;
            }
            threadGroups.add(new PerformanceWorkerThreadGroupAssignment(
                    stringValue(json, "threadGroupPath", ""),
                    intValue(json, "threadGroupIndex", 0),
                    intValue(json, "firstVirtualUserIndex", 0),
                    intValue(json, "virtualUserCount", 0)
            ));
        }
        return threadGroups;
    }

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

    private int intValue(Map<String, Object> json, String key, int defaultValue) {
        Object value = json.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
