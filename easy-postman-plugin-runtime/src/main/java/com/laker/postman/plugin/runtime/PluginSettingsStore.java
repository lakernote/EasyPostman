package com.laker.postman.plugin.runtime;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PluginSettingsStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Object LOCK = new Object();

    private PluginSettingsStore() {
    }

    public static String getString(String key) {
        JsonNode root = readSettings();
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? null : node.asText();
    }

    public static void putString(String key, String value) {
        synchronized (LOCK) {
            ObjectNode root = readSettings().deepCopy();
            if (value == null || value.isBlank()) {
                root.remove(key);
            } else {
                root.put(key, value);
            }
            writeSettings(root);
        }
    }

    public static Set<String> getStringSet(String key) {
        JsonNode node = readSettings().get(key);
        if (!(node instanceof ArrayNode arrayNode)) {
            return Set.of();
        }
        Set<String> values = new LinkedHashSet<>();
        arrayNode.forEach(item -> {
            if (item != null && !item.isNull()) {
                String value = item.asText();
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            }
        });
        return values;
    }

    public static void putStringSet(String key, Set<String> values) {
        synchronized (LOCK) {
            ObjectNode root = readSettings().deepCopy();
            if (values == null || values.isEmpty()) {
                root.remove(key);
            } else {
                ArrayNode arrayNode = root.putArray(key);
                values.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .forEach(arrayNode::add);
            }
            writeSettings(root);
        }
    }

    private static ObjectNode readSettings() {
        synchronized (LOCK) {
            Path path = PluginRuntimePaths.userSettingsFile();
            if (!Files.exists(path)) {
                return MAPPER.createObjectNode();
            }
            try {
                JsonNode node = MAPPER.readTree(Files.readString(path));
                return node instanceof ObjectNode objectNode ? objectNode : MAPPER.createObjectNode();
            } catch (Exception e) {
                return MAPPER.createObjectNode();
            }
        }
    }

    private static void writeSettings(ObjectNode root) {
        Path path = PluginRuntimePaths.userSettingsFile();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist plugin settings", e);
        }
    }
}
