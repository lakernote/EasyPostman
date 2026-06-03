package com.laker.postman.plugin.redis;

import com.laker.postman.plugin.api.PluginStorage;
import com.laker.postman.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
class RedisConnectionProfileStore {
    static final String STORAGE_FILE = "connection-profile.json";
    private static final String SCHEMA_VERSION = "2";
    static final String DEFAULT_PROFILE_ID = "default";
    static final String DEFAULT_PROFILE_NAME = "Default";
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_DATABASE = 0;
    private static final int MAX_HOST_HISTORY = 5;

    private final PluginStorage storage;

    Optional<RedisConnectionProfile> load() {
        return loadActiveProfile();
    }

    Optional<RedisConnectionProfile> loadActiveProfile() {
        ProfileDocument document = loadDocument();
        if (document.profiles().isEmpty()) {
            return Optional.empty();
        }
        return document.profiles().stream()
                .filter(profile -> profile.getId().equals(document.activeProfileId()))
                .findFirst()
                .or(() -> Optional.of(document.profiles().get(0)));
    }

    List<RedisConnectionProfile> loadProfiles() {
        return loadDocument().profiles();
    }

    void save(RedisConnectionProfile profile) {
        upsertProfile(profile);
    }

    void upsertProfile(RedisConnectionProfile profile) {
        RedisConnectionProfile normalizedProfile = normalizeProfile(profile);
        if (normalizedProfile == null) {
            return;
        }
        ProfileDocument document = loadDocument();
        List<RedisConnectionProfile> profiles = new ArrayList<>();
        boolean replaced = false;
        for (RedisConnectionProfile existing : document.profiles()) {
            if (existing.getId().equals(normalizedProfile.getId())) {
                profiles.add(normalizedProfile);
                replaced = true;
            } else {
                profiles.add(existing);
            }
        }
        if (!replaced) {
            profiles.add(normalizedProfile);
        }
        saveProfiles(profiles, normalizedProfile.getId());
    }

    void deleteProfile(String profileId) {
        String normalizedId = safeTrim(profileId, "");
        if (normalizedId.isBlank() || DEFAULT_PROFILE_ID.equals(normalizedId)) {
            return;
        }
        ProfileDocument document = loadDocument();
        List<RedisConnectionProfile> profiles = document.profiles().stream()
                .filter(profile -> !normalizedId.equals(profile.getId()))
                .toList();
        String activeProfileId = normalizedId.equals(document.activeProfileId())
                ? profiles.stream().findFirst().map(RedisConnectionProfile::getId).orElse("")
                : document.activeProfileId();
        saveProfiles(profiles, activeProfileId);
    }

    void saveProfiles(List<RedisConnectionProfile> profiles, String activeProfileId) {
        List<RedisConnectionProfile> normalizedProfiles = normalizeProfiles(profiles);
        String normalizedActiveProfileId = normalizeActiveProfileId(normalizedProfiles, activeProfileId);
        try {
            storage.writeString(STORAGE_FILE, toJson(normalizedProfiles, normalizedActiveProfileId));
        } catch (IOException e) {
            log.warn("Failed to save Redis connection profiles", e);
        }
    }

    private ProfileDocument loadDocument() {
        try {
            Optional<String> content = storage.readString(STORAGE_FILE);
            if (content.isEmpty() || content.get().isBlank()) {
                return defaultDocument();
            }
            return fromJson(content.get());
        } catch (Exception e) {
            log.warn("Failed to load Redis connection profiles", e);
            return defaultDocument();
        }
    }

    private String toJson(List<RedisConnectionProfile> profiles, String activeProfileId) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("activeProfileId", activeProfileId);
        List<Map<String, Object>> profileMaps = new ArrayList<>();
        for (RedisConnectionProfile profile : profiles) {
            profileMaps.add(toMap(profile));
        }
        root.put("profiles", profileMaps);
        return JsonUtil.toJsonPrettyStr(root);
    }

    private Map<String, Object> toMap(RedisConnectionProfile profile) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", safeTrim(profile.getId(), profileIdFor(profile)));
        root.put("name", safeTrim(profile.getName(), defaultProfileName(profile)));
        root.put("host", safeTrim(profile.getHost(), "localhost"));
        root.put("port", normalizePort(profile.getPort()));
        root.put("database", normalizeDatabase(profile.getDatabase()));
        root.put("username", safeTrim(profile.getUsername(), ""));
        root.put("password", safe(profile.getPassword()));
        root.put("hostHistory", normalizeHostHistory(profile.getHostHistory(), profile.getHost()));
        return root;
    }

    private ProfileDocument fromJson(String json) {
        Map<String, Object> root = objectMap(JsonUtil.convertValue(JsonUtil.readTree(json), Map.class));
        List<Map<String, Object>> profileMaps = listObjectMap(root.get("profiles"));
        if (profileMaps.isEmpty()) {
            RedisConnectionProfile profile = fromSingleProfileMap(root);
            return new ProfileDocument(profile.getId(), List.of(profile));
        }

        List<RedisConnectionProfile> profiles = new ArrayList<>();
        for (Map<String, Object> profileMap : profileMaps) {
            RedisConnectionProfile profile = fromProfileMap(profileMap);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        List<RedisConnectionProfile> normalizedProfiles = normalizeProfiles(profiles);
        String activeProfileId = normalizeActiveProfileId(normalizedProfiles, stringValue(root, "activeProfileId", ""));
        return new ProfileDocument(activeProfileId, normalizedProfiles);
    }

    private RedisConnectionProfile fromSingleProfileMap(Map<String, Object> root) {
        RedisConnectionProfile profile = fromProfileMap(root);
        return RedisConnectionProfile.builder()
                .id(DEFAULT_PROFILE_ID)
                .name(DEFAULT_PROFILE_NAME)
                .host(profile.getHost())
                .port(profile.getPort())
                .database(profile.getDatabase())
                .username(profile.getUsername())
                .password(profile.getPassword())
                .hostHistory(profile.getHostHistory())
                .build();
    }

    private RedisConnectionProfile fromProfileMap(Map<String, Object> root) {
        String host = safeTrim(stringValue(root, "host", "localhost"), "localhost");
        int port = intValue(root, "port", DEFAULT_PORT, 1, 65535);
        int database = intValue(root, "database", DEFAULT_DATABASE, 0, 15);
        RedisConnectionProfile profile = RedisConnectionProfile.builder()
                .id(safeTrim(stringValue(root, "id", ""), ""))
                .name(safeTrim(stringValue(root, "name", ""), ""))
                .host(host)
                .port(port)
                .database(database)
                .username(safeTrim(stringValue(root, "username", ""), ""))
                .password(stringValue(root, "password", ""))
                .hostHistory(normalizeHostHistory(listValue(root.get("hostHistory")), host))
                .build();
        return normalizeProfile(profile);
    }

    private List<RedisConnectionProfile> normalizeProfiles(List<RedisConnectionProfile> profiles) {
        List<RedisConnectionProfile> normalized = new ArrayList<>();
        RedisConnectionProfile defaultProfile = null;
        List<RedisConnectionProfile> source = profiles == null ? List.of() : profiles;
        for (RedisConnectionProfile profile : source) {
            RedisConnectionProfile normalizedProfile = normalizeProfile(profile);
            if (normalizedProfile == null || containsProfileId(normalized, normalizedProfile.getId())) {
                continue;
            }
            if (DEFAULT_PROFILE_ID.equals(normalizedProfile.getId())) {
                defaultProfile = normalizedProfile;
            } else {
                normalized.add(normalizedProfile);
            }
        }
        List<RedisConnectionProfile> result = new ArrayList<>();
        result.add(defaultProfile == null ? defaultProfile() : defaultProfile);
        result.addAll(normalized);
        return List.copyOf(result);
    }

    private RedisConnectionProfile normalizeProfile(RedisConnectionProfile profile) {
        if (profile == null) {
            return null;
        }
        String host = safeTrim(profile.getHost(), "localhost");
        int port = normalizePort(profile.getPort());
        int database = normalizeDatabase(profile.getDatabase());
        RedisConnectionProfile candidate = RedisConnectionProfile.builder()
                .id(safeTrim(profile.getId(), ""))
                .name(safeTrim(profile.getName(), ""))
                .host(host)
                .port(port)
                .database(database)
                .username(safeTrim(profile.getUsername(), ""))
                .password(safe(profile.getPassword()))
                .hostHistory(normalizeHostHistory(profile.getHostHistory(), host))
                .build();
        String id = safeTrim(candidate.getId(), profileIdFor(candidate));
        String name = DEFAULT_PROFILE_ID.equals(id)
                ? DEFAULT_PROFILE_NAME
                : safeTrim(candidate.getName(), defaultProfileName(candidate));
        return RedisConnectionProfile.builder()
                .id(id)
                .name(name)
                .host(host)
                .port(port)
                .database(database)
                .username(candidate.getUsername())
                .password(candidate.getPassword())
                .hostHistory(candidate.getHostHistory())
                .build();
    }

    private static boolean containsProfileId(List<RedisConnectionProfile> profiles, String profileId) {
        return profiles.stream().anyMatch(profile -> profile.getId().equals(profileId));
    }

    private static String normalizeActiveProfileId(List<RedisConnectionProfile> profiles, String activeProfileId) {
        String normalized = safeTrim(activeProfileId, "");
        if (!normalized.isBlank() && containsProfileId(profiles, normalized)) {
            return normalized;
        }
        return profiles.stream().findFirst().map(RedisConnectionProfile::getId).orElse("");
    }

    private static String defaultProfileName(RedisConnectionProfile profile) {
        return safeTrim(profile.getHost(), "localhost") + ":" + normalizePort(profile.getPort())
                + "/db" + normalizeDatabase(profile.getDatabase());
    }

    private static String profileIdFor(RedisConnectionProfile profile) {
        String name = safeTrim(profile.getName(), defaultProfileName(profile));
        return "profile-" + Integer.toHexString(Objects.hash(
                name,
                safeTrim(profile.getHost(), "localhost"),
                normalizePort(profile.getPort()),
                normalizeDatabase(profile.getDatabase())
        ));
    }

    static List<String> normalizeHostHistory(List<String> rawHistory, String activeHost) {
        List<String> normalized = new ArrayList<>();
        addUnique(normalized, safeTrim(activeHost, ""));
        if (rawHistory != null) {
            for (String item : rawHistory) {
                addUnique(normalized, safeTrim(item, ""));
                if (normalized.size() >= MAX_HOST_HISTORY) {
                    break;
                }
            }
        }
        return List.copyOf(normalized);
    }

    private static void addUnique(List<String> values, String value) {
        if (value.isBlank() || values.contains(value)) {
            return;
        }
        values.add(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static List<String> listValue(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(item.toString());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listObjectMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private static String stringValue(Map<String, Object> root, String key, String defaultValue) {
        Object value = root.get(key);
        return value == null ? defaultValue : value.toString();
    }

    private static int intValue(Map<String, Object> root, String key, int defaultValue, int min, int max) {
        Object value = root.get(key);
        int parsed;
        if (value instanceof Number number) {
            parsed = number.intValue();
        } else {
            try {
                parsed = Integer.parseInt(value == null ? "" : value.toString().trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return parsed >= min && parsed <= max ? parsed : defaultValue;
    }

    private static int normalizePort(int port) {
        return port >= 1 && port <= 65535 ? port : DEFAULT_PORT;
    }

    private static int normalizeDatabase(int database) {
        return database >= 0 && database <= 15 ? database : DEFAULT_DATABASE;
    }

    private static String safeTrim(String value, String defaultValue) {
        String normalized = safe(value).trim();
        return normalized.isBlank() ? defaultValue : normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static RedisConnectionProfile defaultProfile() {
        return RedisConnectionProfile.builder()
                .id(DEFAULT_PROFILE_ID)
                .name(DEFAULT_PROFILE_NAME)
                .host("localhost")
                .port(DEFAULT_PORT)
                .database(DEFAULT_DATABASE)
                .hostHistory(List.of("localhost"))
                .build();
    }

    private static ProfileDocument defaultDocument() {
        return new ProfileDocument(DEFAULT_PROFILE_ID, List.of(defaultProfile()));
    }

    private record ProfileDocument(String activeProfileId, List<RedisConnectionProfile> profiles) {
    }
}
