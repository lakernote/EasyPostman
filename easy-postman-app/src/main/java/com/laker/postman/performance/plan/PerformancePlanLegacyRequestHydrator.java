package com.laker.postman.performance.plan;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@UtilityClass
class PerformancePlanLegacyRequestHydrator {

    boolean hydrate(Map<String, Object> treeMap, Path configPath) {
        // TODO: Remove this legacy requestSnapshot backfill after old performance configs are no longer supported.
        if (!hasLegacyRequestWithoutSnapshot(treeMap)) {
            return false;
        }
        return hydrateLegacyRequestSnapshots(treeMap, loadCollectionRequestIndex(configPath));
    }

    @SuppressWarnings("unchecked")
    private boolean hydrateLegacyRequestSnapshots(Map<String, Object> node, CollectionRequestIndex requestIndex) {
        if (node == null || node.isEmpty()) {
            return false;
        }

        boolean hydrated = false;
        if ("REQUEST".equals(stringValue(node.get("type"))) && !node.containsKey("requestSnapshot")) {
            HttpRequestItem requestItem = requestIndex.find(
                    stringValue(node.get("requestItemId")),
                    stringValue(node.get("name"))
            );
            PerformanceRequestSnapshot snapshot = PerformanceRequestSnapshotMapper.fromHttpRequestItem(requestItem, null);
            if (snapshot != null) {
                node.put("requestSnapshot", toRequestSnapshotMap(snapshot));
                hydrated = true;
            }
        }

        Object children = node.get("children");
        if (children instanceof List<?> childList) {
            for (Object child : childList) {
                if (child instanceof Map<?, ?> childMap) {
                    hydrated |= hydrateLegacyRequestSnapshots((Map<String, Object>) childMap, requestIndex);
                }
            }
        }
        return hydrated;
    }

    @SuppressWarnings("unchecked")
    private boolean hasLegacyRequestWithoutSnapshot(Map<String, Object> node) {
        if (node == null || node.isEmpty()) {
            return false;
        }
        if ("REQUEST".equals(stringValue(node.get("type"))) && !node.containsKey("requestSnapshot")) {
            return true;
        }

        Object children = node.get("children");
        if (children instanceof List<?> childList) {
            for (Object child : childList) {
                if (child instanceof Map<?, ?> childMap
                        && hasLegacyRequestWithoutSnapshot((Map<String, Object>) childMap)) {
                    return true;
                }
            }
        }
        return false;
    }

    private CollectionRequestIndex loadCollectionRequestIndex(Path configPath) {
        if (configPath == null || configPath.getParent() == null) {
            return CollectionRequestIndex.empty();
        }
        Path collectionsPath = configPath.getParent().resolve("collections.json");
        if (!Files.exists(collectionsPath)) {
            return CollectionRequestIndex.empty();
        }

        CollectionRequestIndex index = new CollectionRequestIndex();
        try {
            for (Object entry : JSONUtil.parseArray(Files.readString(collectionsPath, StandardCharsets.UTF_8))) {
                if (entry instanceof JSONObject object) {
                    collectRequests(object, index);
                } else {
                    collectRequests(JSONUtil.parseObj(entry), index);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build request index for legacy performance config migration: {}", e.getMessage());
            return CollectionRequestIndex.empty();
        }
        return index;
    }

    private void collectRequests(JSONObject node, CollectionRequestIndex index) {
        if (node == null || node.isEmpty()) {
            return;
        }
        if ("request".equals(node.getStr("type"))) {
            JSONObject data = node.getJSONObject("data");
            if (data != null && !data.isEmpty()) {
                HttpRequestItem item = JSONUtil.toBean(data, HttpRequestItem.class);
                item.setBody(item.getBody() == null ? "" : item.getBody());
                index.add(item);
            }
            return;
        }

        var children = node.getJSONArray("children");
        if (children == null) {
            return;
        }
        for (Object child : children) {
            if (child instanceof JSONObject object) {
                collectRequests(object, index);
            } else {
                collectRequests(JSONUtil.parseObj(child), index);
            }
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Map<String, Object> toRequestSnapshotMap(PerformanceRequestSnapshot snapshot) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", snapshot.getId());
        json.put("name", snapshot.getName());
        json.put("description", snapshot.getDescription());
        json.put("url", snapshot.getUrl());
        json.put("method", snapshot.getMethod());
        json.put("protocol", snapshot.getProtocol().name());
        json.put("headers", JsonUtil.convertValue(snapshot.getHeaders(), List.class));
        json.put("bodyType", snapshot.getBodyType());
        json.put("body", snapshot.getBody());
        json.put("params", JsonUtil.convertValue(snapshot.getParams(), List.class));
        json.put("formData", JsonUtil.convertValue(snapshot.getFormData(), List.class));
        json.put("urlencoded", JsonUtil.convertValue(snapshot.getUrlencoded(), List.class));
        json.put("authType", snapshot.getAuthType().name());
        json.put("authUsername", snapshot.getAuthUsername());
        json.put("authPassword", snapshot.getAuthPassword());
        json.put("authToken", snapshot.getAuthToken());
        json.put("followRedirects", snapshot.getFollowRedirects());
        json.put("cookieJarEnabled", snapshot.getCookieJarEnabled());
        json.put("httpVersion", snapshot.getHttpVersion());
        json.put("requestTimeoutMs", snapshot.getRequestTimeoutMs());
        json.put("prescript", snapshot.getPrescript());
        json.put("postscript", snapshot.getPostscript());
        if (snapshot.getExecutionScope() != null && !snapshot.getExecutionScope().getGroupVariables().isEmpty()) {
            json.put("executionScope", new LinkedHashMap<>(snapshot.getExecutionScope().getGroupVariables()));
        }
        return json;
    }

    private static class CollectionRequestIndex {
        private final Map<String, HttpRequestItem> byId = new HashMap<>();
        private final Map<String, HttpRequestItem> byName = new HashMap<>();
        private final Set<String> duplicateNames = new HashSet<>();

        static CollectionRequestIndex empty() {
            return new CollectionRequestIndex();
        }

        void add(HttpRequestItem item) {
            if (item == null) {
                return;
            }
            String id = normalize(item.getId());
            if (!id.isEmpty()) {
                byId.putIfAbsent(id, item);
            }

            String name = normalize(item.getName());
            if (name.isEmpty() || duplicateNames.contains(name)) {
                return;
            }
            HttpRequestItem previous = byName.putIfAbsent(name, item);
            if (previous != null) {
                byName.remove(name);
                duplicateNames.add(name);
            }
        }

        HttpRequestItem find(String requestId, String requestName) {
            HttpRequestItem item = byId.get(normalize(requestId));
            if (item != null) {
                return item;
            }
            String name = normalize(requestName);
            return duplicateNames.contains(name) ? null : byName.get(name);
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
