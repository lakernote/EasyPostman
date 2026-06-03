package com.laker.postman.panel.toolbox;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class ElasticsearchConnectionProfileStoreTest {

    @Test
    public void shouldRoundTripMultipleConnectionProfiles() throws Exception {
        ElasticsearchConnectionProfileStore store = new ElasticsearchConnectionProfileStore(tempStorage());
        ElasticsearchConnectionProfile local = ElasticsearchConnectionProfile.builder()
                .id("local")
                .name("Local")
                .baseUrl("localhost:9200")
                .build();
        ElasticsearchConnectionProfile internal = ElasticsearchConnectionProfile.builder()
                .id("internal")
                .name("Internal")
                .baseUrl("https://es.internal:9243/")
                .authEnabled(true)
                .username("elastic")
                .password("secret")
                .hostHistory(List.of("https://es.internal:9243", "http://localhost:9200"))
                .build();

        store.saveProfiles(List.of(local, internal), "internal");

        List<ElasticsearchConnectionProfile> profiles = store.loadProfiles();
        assertEquals(profiles.size(), 3);
        assertEquals(profiles.get(0).getName(), "Default");
        assertEquals(profiles.get(1).getBaseUrl(), "http://localhost:9200");
        assertEquals(profiles.get(2).getBaseUrl(), "https://es.internal:9243");
        ElasticsearchConnectionProfile active = store.loadActiveProfile().orElseThrow();
        assertEquals(active.getId(), "internal");
        assertEquals(active.isAuthEnabled(), true);
        assertEquals(active.getUsername(), "elastic");
        assertEquals(active.getPassword(), "secret");
        assertEquals(active.getHostHistory(), List.of("https://es.internal:9243", "http://localhost:9200"));
    }

    @Test
    public void shouldDeleteProfileAndMoveActiveProfileToDefault() throws Exception {
        ElasticsearchConnectionProfileStore store = new ElasticsearchConnectionProfileStore(tempStorage());
        store.saveProfiles(List.of(
                ElasticsearchConnectionProfile.builder().id("local").name("Local").baseUrl("http://localhost:9200").build(),
                ElasticsearchConnectionProfile.builder().id("internal").name("Internal").baseUrl("http://es.internal:9200").build()
        ), "internal");

        store.deleteProfile("internal");

        List<ElasticsearchConnectionProfile> profiles = store.loadProfiles();
        assertEquals(profiles.size(), 2);
        assertEquals(profiles.get(0).getId(), "default");
        assertEquals(profiles.get(1).getId(), "local");
        assertEquals(store.loadActiveProfile().orElseThrow().getId(), "default");
    }

    @Test
    public void shouldMigrateSingleProfileJson() throws Exception {
        Path storage = tempStorage();
        Files.writeString(storage, """
                {
                    "baseUrl": "es.internal:9200",
                    "username": "elastic",
                    "password": "secret",
                    "hostHistory": ["es.internal:9200", "http://localhost:9200"]
                }
                """);

        ElasticsearchConnectionProfile profile = new ElasticsearchConnectionProfileStore(storage)
                .loadActiveProfile()
                .orElseThrow();

        assertEquals(profile.getId(), "default");
        assertEquals(profile.getName(), "Default");
        assertEquals(profile.getBaseUrl(), "http://es.internal:9200");
        assertEquals(profile.isAuthEnabled(), true);
        assertEquals(profile.getUsername(), "elastic");
        assertEquals(profile.getHostHistory(), List.of("http://es.internal:9200", "http://localhost:9200"));
    }

    private Path tempStorage() throws Exception {
        return Files.createTempDirectory("easy-postman-es-profiles").resolve("profiles.json");
    }
}
