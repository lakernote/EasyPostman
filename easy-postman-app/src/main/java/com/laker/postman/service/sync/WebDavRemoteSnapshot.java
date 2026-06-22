package com.laker.postman.service.sync;

import com.laker.postman.util.JsonUtil;
import tools.jackson.databind.JsonNode;

public record WebDavRemoteSnapshot(
        int schemaVersion,
        String createdAt,
        String appVersion,
        String snapshotFile,
        long snapshotBytes
) {
    static WebDavRemoteSnapshot fromJson(String json) {
        JsonNode root = JsonUtil.readTree(json == null || json.isBlank() ? "{}" : json);
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Invalid WebDAV manifest");
        }
        return new WebDavRemoteSnapshot(
                intValue(root, "schemaVersion"),
                textValue(root, "createdAt"),
                textValue(root, "appVersion"),
                textValue(root, "snapshotFile"),
                longValue(root, "snapshotBytes")
        );
    }

    private static String textValue(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? "" : node.asText();
    }

    private static int intValue(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? 0 : node.asInt();
    }

    private static long longValue(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? 0L : node.asLong();
    }
}
