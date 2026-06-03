package com.laker.postman.panel.toolbox;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
class ElasticsearchConnectionProfileStore {
    private static final String SCHEMA_VERSION = "2";
    static final String DEFAULT_PROFILE_ID = "default";
    static final String DEFAULT_PROFILE_NAME = "Default";
    private static final String DEFAULT_BASE_URL = "http://localhost:9200";
    private static final int MAX_HOST_HISTORY = 5;

    private final Path storagePath;

    ElasticsearchConnectionProfileStore() {
        this(Path.of(ConfigPathConstants.ELASTICSEARCH_CONNECTION_PROFILES));
    }

    Optional<ElasticsearchConnectionProfile> loadActiveProfile() {
        ProfileDocument document = loadDocument();
        if (document.profiles().isEmpty()) {
            return Optional.empty();
        }
        return document.profiles().stream()
                .filter(profile -> profile.getId().equals(document.activeProfileId()))
                .findFirst()
                .or(() -> Optional.of(document.profiles().get(0)));
    }

    List<ElasticsearchConnectionProfile> loadProfiles() {
        return loadDocument().profiles();
    }

    void upsertProfile(ElasticsearchConnectionProfile profile) {
        ElasticsearchConnectionProfile normalizedProfile = normalizeProfile(profile);
        if (normalizedProfile == null) {
            return;
        }
        ProfileDocument document = loadDocument();
        List<ElasticsearchConnectionProfile> profiles = new ArrayList<>();
        boolean replaced = false;
        for (ElasticsearchConnectionProfile existing : document.profiles()) {
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
        List<ElasticsearchConnectionProfile> profiles = document.profiles().stream()
                .filter(profile -> !normalizedId.equals(profile.getId()))
                .toList();
        String activeProfileId = normalizedId.equals(document.activeProfileId())
                ? profiles.stream().findFirst().map(ElasticsearchConnectionProfile::getId).orElse("")
                : document.activeProfileId();
        saveProfiles(profiles, activeProfileId);
    }

    void saveProfiles(List<ElasticsearchConnectionProfile> profiles, String activeProfileId) {
        List<ElasticsearchConnectionProfile> normalizedProfiles = normalizeProfiles(profiles);
        String normalizedActiveProfileId = normalizeActiveProfileId(normalizedProfiles, activeProfileId);
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(storagePath, toJson(normalizedProfiles, normalizedActiveProfileId),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to save Elasticsearch connection profiles", e);
        }
    }

    private ProfileDocument loadDocument() {
        try {
            if (!Files.exists(storagePath)) {
                return defaultDocument();
            }
            String content = Files.readString(storagePath, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return defaultDocument();
            }
            return fromJson(content);
        } catch (Exception e) {
            log.warn("Failed to load Elasticsearch connection profiles", e);
            return defaultDocument();
        }
    }

    private String toJson(List<ElasticsearchConnectionProfile> profiles, String activeProfileId) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("activeProfileId", activeProfileId);
        List<Map<String, Object>> profileMaps = new ArrayList<>();
        for (ElasticsearchConnectionProfile profile : profiles) {
            profileMaps.add(toMap(profile));
        }
        root.put("profiles", profileMaps);
        return JsonUtil.toJsonPrettyStr(root);
    }

    private Map<String, Object> toMap(ElasticsearchConnectionProfile profile) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", safeTrim(profile.getId(), profileIdFor(profile)));
        root.put("name", safeTrim(profile.getName(), defaultProfileName(profile)));
        root.put("baseUrl", normalizeBaseUrl(profile.getBaseUrl()));
        root.put("authEnabled", profile.isAuthEnabled());
        root.put("username", safeTrim(profile.getUsername(), ""));
        root.put("password", safe(profile.getPassword()));
        root.put("hostHistory", normalizeHostHistory(profile.getHostHistory(), profile.getBaseUrl()));
        return root;
    }

    private ProfileDocument fromJson(String json) {
        Map<String, Object> root = objectMap(JsonUtil.convertValue(JsonUtil.readTree(json), Map.class));
        List<Map<String, Object>> profileMaps = listObjectMap(root.get("profiles"));
        if (profileMaps.isEmpty()) {
            ElasticsearchConnectionProfile profile = fromSingleProfileMap(root);
            return new ProfileDocument(profile.getId(), List.of(profile));
        }

        List<ElasticsearchConnectionProfile> profiles = new ArrayList<>();
        for (Map<String, Object> profileMap : profileMaps) {
            ElasticsearchConnectionProfile profile = fromProfileMap(profileMap);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        List<ElasticsearchConnectionProfile> normalizedProfiles = normalizeProfiles(profiles);
        String activeProfileId = normalizeActiveProfileId(normalizedProfiles, stringValue(root, "activeProfileId", ""));
        return new ProfileDocument(activeProfileId, normalizedProfiles);
    }

    private ElasticsearchConnectionProfile fromSingleProfileMap(Map<String, Object> root) {
        ElasticsearchConnectionProfile profile = fromProfileMap(root);
        return ElasticsearchConnectionProfile.builder()
                .id(DEFAULT_PROFILE_ID)
                .name(DEFAULT_PROFILE_NAME)
                .baseUrl(profile.getBaseUrl())
                .authEnabled(profile.isAuthEnabled())
                .username(profile.getUsername())
                .password(profile.getPassword())
                .hostHistory(profile.getHostHistory())
                .build();
    }

    private ElasticsearchConnectionProfile fromProfileMap(Map<String, Object> root) {
        String baseUrl = normalizeBaseUrl(stringValue(root, "baseUrl", stringValue(root, "host", DEFAULT_BASE_URL)));
        String username = safeTrim(stringValue(root, "username", ""), "");
        String password = stringValue(root, "password", "");
        ElasticsearchConnectionProfile profile = ElasticsearchConnectionProfile.builder()
                .id(safeTrim(stringValue(root, "id", ""), ""))
                .name(safeTrim(stringValue(root, "name", ""), ""))
                .baseUrl(baseUrl)
                .authEnabled(booleanValue(root, "authEnabled", !username.isBlank() || !password.isBlank()))
                .username(username)
                .password(password)
                .hostHistory(normalizeHostHistory(listValue(root.get("hostHistory")), baseUrl))
                .build();
        return normalizeProfile(profile);
    }

    private List<ElasticsearchConnectionProfile> normalizeProfiles(List<ElasticsearchConnectionProfile> profiles) {
        List<ElasticsearchConnectionProfile> normalized = new ArrayList<>();
        ElasticsearchConnectionProfile defaultProfile = null;
        List<ElasticsearchConnectionProfile> source = profiles == null ? List.of() : profiles;
        for (ElasticsearchConnectionProfile profile : source) {
            ElasticsearchConnectionProfile normalizedProfile = normalizeProfile(profile);
            if (normalizedProfile == null || containsProfileId(normalized, normalizedProfile.getId())) {
                continue;
            }
            if (DEFAULT_PROFILE_ID.equals(normalizedProfile.getId())) {
                defaultProfile = normalizedProfile;
            } else {
                normalized.add(normalizedProfile);
            }
        }
        List<ElasticsearchConnectionProfile> result = new ArrayList<>();
        result.add(defaultProfile == null ? defaultProfile() : defaultProfile);
        result.addAll(normalized);
        return List.copyOf(result);
    }

    private ElasticsearchConnectionProfile normalizeProfile(ElasticsearchConnectionProfile profile) {
        if (profile == null) {
            return null;
        }
        String baseUrl = normalizeBaseUrl(profile.getBaseUrl());
        ElasticsearchConnectionProfile candidate = ElasticsearchConnectionProfile.builder()
                .id(safeTrim(profile.getId(), ""))
                .name(safeTrim(profile.getName(), ""))
                .baseUrl(baseUrl)
                .authEnabled(profile.isAuthEnabled())
                .username(safeTrim(profile.getUsername(), ""))
                .password(safe(profile.getPassword()))
                .hostHistory(normalizeHostHistory(profile.getHostHistory(), baseUrl))
                .build();
        String id = safeTrim(candidate.getId(), profileIdFor(candidate));
        String name = DEFAULT_PROFILE_ID.equals(id)
                ? DEFAULT_PROFILE_NAME
                : safeTrim(candidate.getName(), defaultProfileName(candidate));
        return ElasticsearchConnectionProfile.builder()
                .id(id)
                .name(name)
                .baseUrl(candidate.getBaseUrl())
                .authEnabled(candidate.isAuthEnabled())
                .username(candidate.getUsername())
                .password(candidate.getPassword())
                .hostHistory(candidate.getHostHistory())
                .build();
    }

    private static boolean containsProfileId(List<ElasticsearchConnectionProfile> profiles, String profileId) {
        return profiles.stream().anyMatch(profile -> profile.getId().equals(profileId));
    }

    private static String normalizeActiveProfileId(List<ElasticsearchConnectionProfile> profiles, String activeProfileId) {
        String normalized = safeTrim(activeProfileId, "");
        if (!normalized.isBlank() && containsProfileId(profiles, normalized)) {
            return normalized;
        }
        return profiles.stream().findFirst().map(ElasticsearchConnectionProfile::getId).orElse("");
    }

    private static String defaultProfileName(ElasticsearchConnectionProfile profile) {
        return normalizeBaseUrl(profile.getBaseUrl());
    }

    private static String profileIdFor(ElasticsearchConnectionProfile profile) {
        String name = safeTrim(profile.getName(), defaultProfileName(profile));
        return "profile-" + Integer.toHexString(Objects.hash(name, normalizeBaseUrl(profile.getBaseUrl())));
    }

    static List<String> normalizeHostHistory(List<String> rawHistory, String activeHost) {
        List<String> normalized = new ArrayList<>();
        addUnique(normalized, normalizeBaseUrl(activeHost));
        if (rawHistory != null) {
            for (String item : rawHistory) {
                addUnique(normalized, normalizeBaseUrl(item));
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

    static String normalizeBaseUrl(String rawUrl) {
        String value = safeTrim(rawUrl, DEFAULT_BASE_URL);
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://" + value;
        }
        while (value.endsWith("/") && value.length() > "https://".length()) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
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

    private static boolean booleanValue(Map<String, Object> root, String key, boolean defaultValue) {
        Object value = root.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static String safeTrim(String value, String defaultValue) {
        String normalized = safe(value).trim();
        return normalized.isBlank() ? defaultValue : normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static ElasticsearchConnectionProfile defaultProfile() {
        return ElasticsearchConnectionProfile.builder()
                .id(DEFAULT_PROFILE_ID)
                .name(DEFAULT_PROFILE_NAME)
                .baseUrl(DEFAULT_BASE_URL)
                .hostHistory(List.of(DEFAULT_BASE_URL))
                .build();
    }

    private static ProfileDocument defaultDocument() {
        return new ProfileDocument(DEFAULT_PROFILE_ID, List.of(defaultProfile()));
    }

    private record ProfileDocument(String activeProfileId, List<ElasticsearchConnectionProfile> profiles) {
    }
}
