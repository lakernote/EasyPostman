package com.laker.postman.settings;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PreferencesStoreMigrationTest {

    @Test
    public void shouldApplyPendingMigrationsAndPersistVersion() throws Exception {
        Path settingsFile = Files.createTempFile("easy-postman-settings", ".properties");
        Properties oldProperties = new Properties();
        oldProperties.setProperty("old_update_source", "github");
        try (var output = Files.newOutputStream(settingsFile)) {
            oldProperties.store(output, "test");
        }

        SettingKey<String> updateSourceKey = SettingKey.stringKey("update_source_preference", "auto");
        PreferencesStore store = PreferencesStore.fileBacked(settingsFile);

        store.loadAndMigrate(List.of(
                SettingsMigration.toVersion(1, properties -> {
                    String oldValue = properties.getProperty("old_update_source");
                    if (oldValue != null) {
                        properties.setProperty(updateSourceKey.name(), oldValue);
                        properties.remove("old_update_source");
                    }
                })
        ));

        assertEquals(store.get(updateSourceKey), "github");
        assertEquals(store.getSchemaVersion(), 1);
        assertFalse(store.snapshot().containsKey("old_update_source"));

        Properties reloaded = new Properties();
        try (var input = Files.newInputStream(settingsFile)) {
            reloaded.load(input);
        }
        assertEquals(reloaded.getProperty("update_source_preference"), "github");
        assertEquals(reloaded.getProperty(PreferencesStore.DEFAULT_SCHEMA_VERSION_KEY), "1");
        assertFalse(reloaded.containsKey("old_update_source"));
    }

    @Test
    public void shouldReportWhetherTypedKeyIsStored() throws Exception {
        Path settingsFile = Files.createTempFile("easy-postman-settings", ".properties");
        PreferencesStore store = PreferencesStore.fileBacked(settingsFile);
        SettingKey<String> key = SettingKey.stringKey("optional_key", "default");

        store.load();
        assertFalse(store.contains(key));
        assertEquals(store.get(key), "default");

        store.put(key, "stored");

        assertTrue(store.contains(key));
        assertEquals(store.get(key), "stored");
    }
}
