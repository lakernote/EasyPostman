package com.laker.postman.plugin.capture;

import com.laker.postman.plugin.api.PluginStorage;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.UserPreferencesStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
class CaptureSettingsStore {
    static final String STORAGE_FILE = "settings.json";
    private static final String KEY_BIND_HOST = "bindHost";
    private static final String KEY_BIND_PORT = "bindPort";
    private static final String KEY_SYNC_SYSTEM_PROXY = "syncSystemProxy";
    private static final String KEY_HOST_FILTER = "hostFilter";

    private static final String LEGACY_BIND_HOST = "plugin.capture.bindHost";
    private static final String LEGACY_BIND_PORT = "plugin.capture.bindPort";
    private static final String LEGACY_SYNC_SYSTEM_PROXY = "plugin.capture.syncSystemProxy";
    private static final String LEGACY_HOST_FILTER = "plugin.capture.hostFilter";

    private static final String DEFAULT_BIND_HOST = "127.0.0.1";
    private static final int DEFAULT_BIND_PORT = 8888;

    private final PluginStorage storage;

    CaptureSettings load() {
        return loadStoredSettings().orElseGet(this::loadLegacySettings);
    }

    void save(CaptureSettings settings) {
        try {
            storage.writeString(STORAGE_FILE, toJson(normalize(settings)));
        } catch (IOException e) {
            log.warn("Failed to save capture plugin settings", e);
        }
    }

    private Optional<CaptureSettings> loadStoredSettings() {
        try {
            Optional<String> content = storage.readString(STORAGE_FILE);
            if (content.isEmpty() || content.get().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(fromJson(content.get()));
        } catch (Exception e) {
            log.warn("Failed to load capture plugin settings", e);
            return Optional.empty();
        }
    }

    private CaptureSettings loadLegacySettings() {
        CaptureSettings settings = normalize(new CaptureSettings(
                UserPreferencesStore.getString(LEGACY_BIND_HOST),
                legacyPort(),
                Boolean.TRUE.equals(UserPreferencesStore.getBoolean(LEGACY_SYNC_SYSTEM_PROXY)),
                UserPreferencesStore.getString(LEGACY_HOST_FILTER)
        ));
        if (hasLegacySettings()) {
            save(settings);
        }
        return settings;
    }

    private int legacyPort() {
        Integer savedPort = UserPreferencesStore.getInt(LEGACY_BIND_PORT);
        return savedPort == null ? DEFAULT_BIND_PORT : savedPort;
    }

    private boolean hasLegacySettings() {
        return UserPreferencesStore.get(LEGACY_BIND_HOST) != null
                || UserPreferencesStore.get(LEGACY_BIND_PORT) != null
                || UserPreferencesStore.get(LEGACY_SYNC_SYSTEM_PROXY) != null
                || UserPreferencesStore.get(LEGACY_HOST_FILTER) != null;
    }

    private String toJson(CaptureSettings settings) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put(KEY_BIND_HOST, settings.bindHost());
        root.put(KEY_BIND_PORT, settings.bindPort());
        root.put(KEY_SYNC_SYSTEM_PROXY, settings.syncSystemProxy());
        root.put(KEY_HOST_FILTER, settings.hostFilter());
        return JsonUtil.toJsonPrettyStr(root);
    }

    private CaptureSettings fromJson(String json) {
        Map<String, Object> root = objectMap(JsonUtil.convertValue(JsonUtil.readTree(json), Map.class));
        return normalize(new CaptureSettings(
                stringValue(root, KEY_BIND_HOST, DEFAULT_BIND_HOST),
                intValue(root, KEY_BIND_PORT, DEFAULT_BIND_PORT),
                booleanValue(root, KEY_SYNC_SYSTEM_PROXY, false),
                stringValue(root, KEY_HOST_FILTER, "")
        ));
    }

    private static CaptureSettings normalize(CaptureSettings settings) {
        CaptureSettings source = settings == null
                ? new CaptureSettings(DEFAULT_BIND_HOST, DEFAULT_BIND_PORT, false, "")
                : settings;
        return new CaptureSettings(
                safeTrim(source.bindHost(), DEFAULT_BIND_HOST),
                normalizePort(source.bindPort()),
                source.syncSystemProxy(),
                safeTrim(source.hostFilter(), "")
        );
    }

    private static int normalizePort(int port) {
        return port < 1 || port > 65535 ? DEFAULT_BIND_PORT : port;
    }

    private static String safeTrim(String value, String defaultValue) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? defaultValue : normalized;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String stringValue(Map<String, Object> root, String key, String defaultValue) {
        Object value = root.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private static int intValue(Map<String, Object> root, String key, int defaultValue) {
        Object value = root.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static boolean booleanValue(Map<String, Object> root, String key, boolean defaultValue) {
        Object value = root.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text.trim());
        }
        return defaultValue;
    }
}
