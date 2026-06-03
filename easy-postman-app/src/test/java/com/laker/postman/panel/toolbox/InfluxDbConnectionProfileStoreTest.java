package com.laker.postman.panel.toolbox;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class InfluxDbConnectionProfileStoreTest {

    @Test
    public void shouldRoundTripMultipleConnectionProfiles() throws Exception {
        InfluxDbConnectionProfileStore store = new InfluxDbConnectionProfileStore(tempStorage());
        InfluxDbConnectionProfile v2 = InfluxDbConnectionProfile.builder()
                .id("v2")
                .name("V2")
                .baseUrl("localhost:8086")
                .mode(InfluxDbPanel.QueryMode.FLUX_V2.name())
                .token("token")
                .org("dev")
                .build();
        InfluxDbConnectionProfile v1 = InfluxDbConnectionProfile.builder()
                .id("v1")
                .name("V1")
                .baseUrl("https://influx.internal:8086/")
                .mode(InfluxDbPanel.QueryMode.INFLUXQL_V1.name())
                .database("metrics")
                .measurement("cpu")
                .username("admin")
                .password("secret")
                .hostHistory(List.of("https://influx.internal:8086", "http://localhost:8086"))
                .build();

        store.saveProfiles(List.of(v2, v1), "v1");

        List<InfluxDbConnectionProfile> profiles = store.loadProfiles();
        assertEquals(profiles.size(), 3);
        assertEquals(profiles.get(0).getName(), "Default");
        assertEquals(profiles.get(1).getBaseUrl(), "http://localhost:8086");
        assertEquals(profiles.get(2).getBaseUrl(), "https://influx.internal:8086");
        InfluxDbConnectionProfile active = store.loadActiveProfile().orElseThrow();
        assertEquals(active.getId(), "v1");
        assertEquals(active.getMode(), InfluxDbPanel.QueryMode.INFLUXQL_V1.name());
        assertEquals(active.getDatabase(), "metrics");
        assertEquals(active.getMeasurement(), "cpu");
        assertEquals(active.getUsername(), "admin");
        assertEquals(active.getPassword(), "secret");
        assertEquals(active.getHostHistory(), List.of("https://influx.internal:8086", "http://localhost:8086"));
    }

    @Test
    public void shouldDeleteProfileAndMoveActiveProfileToDefault() throws Exception {
        InfluxDbConnectionProfileStore store = new InfluxDbConnectionProfileStore(tempStorage());
        store.saveProfiles(List.of(
                InfluxDbConnectionProfile.builder().id("local").name("Local").baseUrl("http://localhost:8086").build(),
                InfluxDbConnectionProfile.builder().id("internal").name("Internal").baseUrl("http://influx.internal:8086").build()
        ), "internal");

        store.deleteProfile("internal");

        List<InfluxDbConnectionProfile> profiles = store.loadProfiles();
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
                    "baseUrl": "influx.internal:8086",
                    "mode": "INFLUXQL_V1",
                    "database": "metrics",
                    "measurement": "cpu",
                    "username": "admin",
                    "password": "secret",
                    "hostHistory": ["influx.internal:8086", "http://localhost:8086"]
                }
                """);

        InfluxDbConnectionProfile profile = new InfluxDbConnectionProfileStore(storage)
                .loadActiveProfile()
                .orElseThrow();

        assertEquals(profile.getId(), "default");
        assertEquals(profile.getName(), "Default");
        assertEquals(profile.getBaseUrl(), "http://influx.internal:8086");
        assertEquals(profile.getMode(), InfluxDbPanel.QueryMode.INFLUXQL_V1.name());
        assertEquals(profile.getDatabase(), "metrics");
        assertEquals(profile.getMeasurement(), "cpu");
        assertEquals(profile.getUsername(), "admin");
        assertEquals(profile.getHostHistory(), List.of("http://influx.internal:8086", "http://localhost:8086"));
    }

    private Path tempStorage() throws Exception {
        return Files.createTempDirectory("easy-postman-influx-profiles").resolve("profiles.json");
    }
}
