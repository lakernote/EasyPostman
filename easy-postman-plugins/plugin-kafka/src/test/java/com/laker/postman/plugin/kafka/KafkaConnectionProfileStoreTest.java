package com.laker.postman.plugin.kafka;

import com.laker.postman.plugin.api.PluginStorage;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class KafkaConnectionProfileStoreTest {

    @Test
    public void shouldRoundTripMultipleConnectionProfiles() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        KafkaConnectionProfileStore store = new KafkaConnectionProfileStore(storage);
        KafkaConnectionProfile local = KafkaConnectionProfile.builder()
                .id("local")
                .name("Local")
                .bootstrapServers("localhost:9092")
                .clientId("easy-postman-local")
                .build();
        KafkaConnectionProfile internal = KafkaConnectionProfile.builder()
                .id("internal")
                .name("Internal")
                .bootstrapServers("kafka.internal:9093")
                .clientId("easy-postman-test")
                .securityProtocol("SASL_SSL")
                .saslMechanism("SCRAM-SHA-512")
                .username("app")
                .password("secret")
                .build();

        store.saveProfiles(java.util.List.of(local, internal), "internal");

        java.util.List<KafkaConnectionProfile> profiles = store.loadProfiles();
        assertEquals(profiles.size(), 3);
        assertEquals(profiles.get(0).getName(), "Default");
        assertEquals(profiles.get(1).getName(), "Local");
        assertEquals(profiles.get(2).getName(), "Internal");
        KafkaConnectionProfile active = store.loadActiveProfile().orElseThrow();
        assertEquals(active.getId(), "internal");
        assertEquals(active.getBootstrapServers(), "kafka.internal:9093");
        assertEquals(active.getClientId(), "easy-postman-test");
        assertEquals(active.getSecurityProtocol(), "SASL_SSL");
        assertEquals(active.getSaslMechanism(), "SCRAM-SHA-512");
        assertEquals(active.getUsername(), "app");
        assertEquals(active.getPassword(), "secret");
    }

    @Test
    public void shouldMigrateSingleProfileJson() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        storage.writeString("connection-profile.json", """
                {
                    "bootstrapServers": "kafka.internal:9093",
                    "clientId": "easy-postman-test",
                    "securityProtocol": "SASL_SSL",
                    "saslMechanism": "SCRAM-SHA-512",
                    "username": "app",
                    "password": "secret"
                }
                """);

        KafkaConnectionProfile profile = new KafkaConnectionProfileStore(storage).loadActiveProfile().orElseThrow();

        assertEquals(profile.getId(), "default");
        assertEquals(profile.getName(), "Default");
        assertEquals(profile.getBootstrapServers(), "kafka.internal:9093");
    }

    @Test
    public void shouldDeleteProfileAndMoveActiveProfile() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        KafkaConnectionProfileStore store = new KafkaConnectionProfileStore(storage);
        store.saveProfiles(java.util.List.of(
                KafkaConnectionProfile.builder().id("local").name("Local").bootstrapServers("localhost:9092").build(),
                KafkaConnectionProfile.builder().id("internal").name("Internal").bootstrapServers("kafka.internal:9093").build()
        ), "internal");

        store.deleteProfile("internal");

        java.util.List<KafkaConnectionProfile> profiles = store.loadProfiles();
        assertEquals(profiles.size(), 2);
        assertEquals(profiles.get(0).getId(), "default");
        assertEquals(profiles.get(1).getId(), "local");
        assertEquals(store.loadActiveProfile().orElseThrow().getId(), "default");
    }

    @Test
    public void shouldKeepDefaultProfileWhenDeletingDefault() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        KafkaConnectionProfileStore store = new KafkaConnectionProfileStore(storage);

        store.deleteProfile("default");

        java.util.List<KafkaConnectionProfile> profiles = store.loadProfiles();
        assertEquals(profiles.size(), 1);
        assertEquals(profiles.get(0).getId(), "default");
    }

    @Test
    public void shouldCreateDefaultProfileWhenMissingProfileFile() throws Exception {
        KafkaConnectionProfile profile = new KafkaConnectionProfileStore(new MemoryPluginStorage())
                .loadActiveProfile()
                .orElseThrow();

        assertEquals(profile.getId(), "default");
        assertEquals(profile.getName(), "Default");
        assertEquals(profile.getBootstrapServers(), "localhost:9092");
    }

    private static final class MemoryPluginStorage implements PluginStorage {
        private final Map<String, String> files = new HashMap<>();

        @Override
        public Optional<String> readString(String relativePath) {
            return Optional.ofNullable(files.get(relativePath));
        }

        @Override
        public void writeString(String relativePath, String content) {
            files.put(relativePath, content);
        }

        @Override
        public void delete(String relativePath) {
            files.remove(relativePath);
        }

        @Override
        public Path dataDirectory() {
            return Path.of("");
        }
    }
}
