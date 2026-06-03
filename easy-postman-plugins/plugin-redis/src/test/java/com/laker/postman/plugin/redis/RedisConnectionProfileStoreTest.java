package com.laker.postman.plugin.redis;

import com.laker.postman.plugin.api.PluginStorage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class RedisConnectionProfileStoreTest {

    @Test
    public void shouldRoundTripMultipleConnectionProfiles() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        RedisConnectionProfileStore store = new RedisConnectionProfileStore(storage);
        RedisConnectionProfile local = RedisConnectionProfile.builder()
                .id("local")
                .name("Local")
                .host("localhost")
                .port(6379)
                .database(0)
                .build();
        RedisConnectionProfile internal = RedisConnectionProfile.builder()
                .id("internal")
                .name("Internal")
                .host("redis.internal")
                .port(6380)
                .database(3)
                .username("app")
                .password("secret")
                .hostHistory(List.of("redis.internal", "localhost"))
                .build();

        store.saveProfiles(List.of(local, internal), "internal");

        List<RedisConnectionProfile> profiles = store.loadProfiles();
        assertEquals(profiles.size(), 3);
        assertEquals(profiles.get(0).getName(), "Default");
        assertEquals(profiles.get(1).getName(), "Local");
        assertEquals(profiles.get(2).getName(), "Internal");
        RedisConnectionProfile active = store.loadActiveProfile().orElseThrow();
        assertEquals(active.getId(), "internal");
        assertEquals(active.getHost(), "redis.internal");
        assertEquals(active.getPort(), 6380);
        assertEquals(active.getDatabase(), 3);
        assertEquals(active.getUsername(), "app");
        assertEquals(active.getPassword(), "secret");
        assertEquals(active.getHostHistory(), List.of("redis.internal", "localhost"));
    }

    @Test
    public void shouldDeleteProfileAndMoveActiveProfile() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        RedisConnectionProfileStore store = new RedisConnectionProfileStore(storage);
        store.saveProfiles(List.of(
                RedisConnectionProfile.builder().id("local").name("Local").host("localhost").port(6379).build(),
                RedisConnectionProfile.builder().id("internal").name("Internal").host("redis.internal").port(6380).build()
        ), "internal");

        store.deleteProfile("internal");

        List<RedisConnectionProfile> profiles = store.loadProfiles();
        assertEquals(profiles.size(), 2);
        assertEquals(profiles.get(0).getId(), "default");
        assertEquals(profiles.get(1).getId(), "local");
        assertEquals(store.loadActiveProfile().orElseThrow().getId(), "default");
    }

    @Test
    public void shouldKeepDefaultProfileWhenDeletingDefault() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        RedisConnectionProfileStore store = new RedisConnectionProfileStore(storage);

        store.deleteProfile("default");

        List<RedisConnectionProfile> profiles = store.loadProfiles();
        assertEquals(profiles.size(), 1);
        assertEquals(profiles.get(0).getId(), "default");
    }

    @Test
    public void shouldMigrateSingleProfileJson() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        storage.writeString("connection-profile.json", """
                {
                    "host": "redis.internal",
                    "port": 6380,
                    "database": 2,
                    "username": "app",
                    "password": "secret",
                    "hostHistory": ["redis.internal", "localhost"]
                }
                """);

        RedisConnectionProfile profile = new RedisConnectionProfileStore(storage).loadActiveProfile().orElseThrow();

        assertEquals(profile.getId(), "default");
        assertEquals(profile.getName(), "Default");
        assertEquals(profile.getHost(), "redis.internal");
        assertEquals(profile.getPort(), 6380);
        assertEquals(profile.getDatabase(), 2);
    }

    @Test
    public void shouldCreateDefaultProfileForBlankStoredProfile() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        storage.writeString("connection-profile.json", "  ");

        RedisConnectionProfile profile = new RedisConnectionProfileStore(storage).loadActiveProfile().orElseThrow();

        assertEquals(profile.getId(), "default");
        assertEquals(profile.getName(), "Default");
        assertEquals(profile.getHost(), "localhost");
        assertEquals(profile.getPort(), 6379);
    }

    @Test
    public void shouldCreateDefaultProfileWhenMissingProfileFile() throws Exception {
        RedisConnectionProfile profile = new RedisConnectionProfileStore(new MemoryPluginStorage())
                .loadActiveProfile()
                .orElseThrow();

        assertEquals(profile.getId(), "default");
        assertEquals(profile.getName(), "Default");
        assertEquals(profile.getHost(), "localhost");
        assertEquals(profile.getPort(), 6379);
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
