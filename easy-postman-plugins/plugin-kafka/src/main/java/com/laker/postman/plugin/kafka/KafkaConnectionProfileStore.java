package com.laker.postman.plugin.kafka;

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
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
class KafkaConnectionProfileStore {
    static final String STORAGE_FILE = "connection-profile.json";
    private static final String SCHEMA_VERSION = "2";
    static final String DEFAULT_PROFILE_ID = "default";
    static final String DEFAULT_PROFILE_NAME = "Default";
    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_CLIENT_ID = "easy-postman-toolbox";
    private static final String DEFAULT_SECURITY_PROTOCOL = "PLAINTEXT";
    private static final String DEFAULT_SASL_MECHANISM = "PLAIN";
    private static final Set<String> SECURITY_PROTOCOLS = Set.of("PLAINTEXT", "SASL_PLAINTEXT", "SASL_SSL", "SSL");
    private static final Set<String> SASL_MECHANISMS = Set.of("PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512");

    private final PluginStorage storage;

    Optional<KafkaConnectionProfile> load() {
        return loadActiveProfile();
    }

    Optional<KafkaConnectionProfile> loadActiveProfile() {
        ProfileDocument document = loadDocument();
        if (document.profiles().isEmpty()) {
            return Optional.empty();
        }
        return document.profiles().stream()
                .filter(profile -> profile.getId().equals(document.activeProfileId()))
                .findFirst()
                .or(() -> Optional.of(document.profiles().get(0)));
    }

    List<KafkaConnectionProfile> loadProfiles() {
        return loadDocument().profiles();
    }

    void save(KafkaConnectionProfile profile) {
        upsertProfile(profile);
    }

    void upsertProfile(KafkaConnectionProfile profile) {
        KafkaConnectionProfile normalizedProfile = normalizeProfile(profile);
        if (normalizedProfile == null) {
            return;
        }
        ProfileDocument document = loadDocument();
        List<KafkaConnectionProfile> profiles = new ArrayList<>();
        boolean replaced = false;
        for (KafkaConnectionProfile existing : document.profiles()) {
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
        List<KafkaConnectionProfile> profiles = document.profiles().stream()
                .filter(profile -> !normalizedId.equals(profile.getId()))
                .toList();
        String activeProfileId = normalizedId.equals(document.activeProfileId())
                ? profiles.stream().findFirst().map(KafkaConnectionProfile::getId).orElse("")
                : document.activeProfileId();
        saveProfiles(profiles, activeProfileId);
    }

    void saveProfiles(List<KafkaConnectionProfile> profiles, String activeProfileId) {
        List<KafkaConnectionProfile> normalizedProfiles = normalizeProfiles(profiles);
        String normalizedActiveProfileId = normalizeActiveProfileId(normalizedProfiles, activeProfileId);
        try {
            storage.writeString(STORAGE_FILE, toJson(normalizedProfiles, normalizedActiveProfileId));
        } catch (IOException e) {
            log.warn("Failed to save Kafka connection profiles", e);
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
            log.warn("Failed to load Kafka connection profiles", e);
            return defaultDocument();
        }
    }

    private String toJson(List<KafkaConnectionProfile> profiles, String activeProfileId) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", SCHEMA_VERSION);
        root.put("activeProfileId", activeProfileId);
        List<Map<String, Object>> profileMaps = new ArrayList<>();
        for (KafkaConnectionProfile profile : profiles) {
            profileMaps.add(toMap(profile));
        }
        root.put("profiles", profileMaps);
        return JsonUtil.toJsonPrettyStr(root);
    }

    private Map<String, Object> toMap(KafkaConnectionProfile profile) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", safeTrim(profile.getId(), profileIdFor(profile)));
        root.put("name", safeTrim(profile.getName(), defaultProfileName(profile)));
        root.put("bootstrapServers", safeTrim(profile.getBootstrapServers(), DEFAULT_BOOTSTRAP_SERVERS));
        root.put("clientId", safeTrim(profile.getClientId(), DEFAULT_CLIENT_ID));
        root.put("securityProtocol", normalizeSecurityProtocol(profile.getSecurityProtocol()));
        root.put("saslMechanism", normalizeSaslMechanism(profile.getSaslMechanism()));
        root.put("username", safeTrim(profile.getUsername(), ""));
        root.put("password", safe(profile.getPassword()));
        return root;
    }

    private ProfileDocument fromJson(String json) {
        Map<String, Object> root = objectMap(JsonUtil.convertValue(JsonUtil.readTree(json), Map.class));
        List<Map<String, Object>> profileMaps = listObjectMap(root.get("profiles"));
        if (profileMaps.isEmpty()) {
            KafkaConnectionProfile profile = fromSingleProfileMap(root);
            return new ProfileDocument(profile.getId(), List.of(profile));
        }

        List<KafkaConnectionProfile> profiles = new ArrayList<>();
        for (Map<String, Object> profileMap : profileMaps) {
            KafkaConnectionProfile profile = fromProfileMap(profileMap);
            if (profile != null) {
                profiles.add(profile);
            }
        }
        List<KafkaConnectionProfile> normalizedProfiles = normalizeProfiles(profiles);
        String activeProfileId = normalizeActiveProfileId(normalizedProfiles, stringValue(root, "activeProfileId", ""));
        return new ProfileDocument(activeProfileId, normalizedProfiles);
    }

    private KafkaConnectionProfile fromSingleProfileMap(Map<String, Object> root) {
        KafkaConnectionProfile profile = fromProfileMap(root);
        return KafkaConnectionProfile.builder()
                .id(DEFAULT_PROFILE_ID)
                .name(DEFAULT_PROFILE_NAME)
                .bootstrapServers(profile.getBootstrapServers())
                .clientId(profile.getClientId())
                .securityProtocol(profile.getSecurityProtocol())
                .saslMechanism(profile.getSaslMechanism())
                .username(profile.getUsername())
                .password(profile.getPassword())
                .build();
    }

    private KafkaConnectionProfile fromProfileMap(Map<String, Object> root) {
        KafkaConnectionProfile profile = KafkaConnectionProfile.builder()
                .id(safeTrim(stringValue(root, "id", ""), ""))
                .name(safeTrim(stringValue(root, "name", ""), ""))
                .bootstrapServers(safeTrim(stringValue(root, "bootstrapServers", DEFAULT_BOOTSTRAP_SERVERS), DEFAULT_BOOTSTRAP_SERVERS))
                .clientId(safeTrim(stringValue(root, "clientId", DEFAULT_CLIENT_ID), DEFAULT_CLIENT_ID))
                .securityProtocol(normalizeSecurityProtocol(stringValue(root, "securityProtocol", DEFAULT_SECURITY_PROTOCOL)))
                .saslMechanism(normalizeSaslMechanism(stringValue(root, "saslMechanism", DEFAULT_SASL_MECHANISM)))
                .username(safeTrim(stringValue(root, "username", ""), ""))
                .password(stringValue(root, "password", ""))
                .build();
        return normalizeProfile(profile);
    }

    private List<KafkaConnectionProfile> normalizeProfiles(List<KafkaConnectionProfile> profiles) {
        List<KafkaConnectionProfile> normalized = new ArrayList<>();
        KafkaConnectionProfile defaultProfile = null;
        List<KafkaConnectionProfile> source = profiles == null ? List.of() : profiles;
        for (KafkaConnectionProfile profile : source) {
            KafkaConnectionProfile normalizedProfile = normalizeProfile(profile);
            if (normalizedProfile == null || containsProfileId(normalized, normalizedProfile.getId())) {
                continue;
            }
            if (DEFAULT_PROFILE_ID.equals(normalizedProfile.getId())) {
                defaultProfile = normalizedProfile;
            } else {
                normalized.add(normalizedProfile);
            }
        }
        List<KafkaConnectionProfile> result = new ArrayList<>();
        result.add(defaultProfile == null ? defaultProfile() : defaultProfile);
        result.addAll(normalized);
        return List.copyOf(result);
    }

    private KafkaConnectionProfile normalizeProfile(KafkaConnectionProfile profile) {
        if (profile == null) {
            return null;
        }
        KafkaConnectionProfile candidate = KafkaConnectionProfile.builder()
                .id(safeTrim(profile.getId(), ""))
                .name(safeTrim(profile.getName(), ""))
                .bootstrapServers(safeTrim(profile.getBootstrapServers(), DEFAULT_BOOTSTRAP_SERVERS))
                .clientId(safeTrim(profile.getClientId(), DEFAULT_CLIENT_ID))
                .securityProtocol(normalizeSecurityProtocol(profile.getSecurityProtocol()))
                .saslMechanism(normalizeSaslMechanism(profile.getSaslMechanism()))
                .username(safeTrim(profile.getUsername(), ""))
                .password(safe(profile.getPassword()))
                .build();
        return KafkaConnectionProfile.builder()
                .id(safeTrim(candidate.getId(), profileIdFor(candidate)))
                .name(DEFAULT_PROFILE_ID.equals(safeTrim(candidate.getId(), ""))
                        ? DEFAULT_PROFILE_NAME
                        : safeTrim(candidate.getName(), defaultProfileName(candidate)))
                .bootstrapServers(candidate.getBootstrapServers())
                .clientId(candidate.getClientId())
                .securityProtocol(candidate.getSecurityProtocol())
                .saslMechanism(candidate.getSaslMechanism())
                .username(candidate.getUsername())
                .password(candidate.getPassword())
                .build();
    }

    private static boolean containsProfileId(List<KafkaConnectionProfile> profiles, String profileId) {
        return profiles.stream().anyMatch(profile -> profile.getId().equals(profileId));
    }

    private static String normalizeActiveProfileId(List<KafkaConnectionProfile> profiles, String activeProfileId) {
        String normalized = safeTrim(activeProfileId, "");
        if (!normalized.isBlank() && containsProfileId(profiles, normalized)) {
            return normalized;
        }
        return profiles.stream().findFirst().map(KafkaConnectionProfile::getId).orElse("");
    }

    private static String defaultProfileName(KafkaConnectionProfile profile) {
        return safeTrim(profile.getBootstrapServers(), DEFAULT_BOOTSTRAP_SERVERS);
    }

    private static String profileIdFor(KafkaConnectionProfile profile) {
        String name = safeTrim(profile.getName(), defaultProfileName(profile));
        return "profile-" + Integer.toHexString(Objects.hash(
                name,
                safeTrim(profile.getBootstrapServers(), DEFAULT_BOOTSTRAP_SERVERS),
                safeTrim(profile.getClientId(), DEFAULT_CLIENT_ID)
        ));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String stringValue(Map<String, Object> root, String key, String defaultValue) {
        Object value = root.get(key);
        return value == null ? defaultValue : value.toString();
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

    private static String normalizeSecurityProtocol(String securityProtocol) {
        String value = safeTrim(securityProtocol, DEFAULT_SECURITY_PROTOCOL);
        return SECURITY_PROTOCOLS.contains(value) ? value : DEFAULT_SECURITY_PROTOCOL;
    }

    private static String normalizeSaslMechanism(String saslMechanism) {
        String value = safeTrim(saslMechanism, DEFAULT_SASL_MECHANISM);
        return SASL_MECHANISMS.contains(value) ? value : DEFAULT_SASL_MECHANISM;
    }

    private static String safeTrim(String value, String defaultValue) {
        String normalized = safe(value).trim();
        return normalized.isBlank() ? defaultValue : normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static KafkaConnectionProfile defaultProfile() {
        return KafkaConnectionProfile.builder()
                .id(DEFAULT_PROFILE_ID)
                .name(DEFAULT_PROFILE_NAME)
                .bootstrapServers(DEFAULT_BOOTSTRAP_SERVERS)
                .clientId(DEFAULT_CLIENT_ID)
                .securityProtocol(DEFAULT_SECURITY_PROTOCOL)
                .saslMechanism(DEFAULT_SASL_MECHANISM)
                .build();
    }

    private static ProfileDocument defaultDocument() {
        return new ProfileDocument(DEFAULT_PROFILE_ID, List.of(defaultProfile()));
    }

    private record ProfileDocument(String activeProfileId, List<KafkaConnectionProfile> profiles) {
    }
}
