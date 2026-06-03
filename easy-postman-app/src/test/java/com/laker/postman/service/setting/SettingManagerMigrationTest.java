package com.laker.postman.service.setting;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.settings.PreferencesStore;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class SettingManagerMigrationTest {

    @Test
    public void loadShouldMigrateLegacyUpdateSourceAlias() throws Exception {
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            Properties legacySettings = new Properties();
            legacySettings.setProperty("update_source", "gitee");
            PreferencesStore.storeProperties(legacySettings, configPath);

            props.clear();
            SettingManager.load();

            assertEquals(props.getProperty("update_source_preference"), "gitee");
            assertFalse(props.containsKey("update_source"));
            assertEquals(props.getProperty(PreferencesStore.DEFAULT_SCHEMA_VERSION_KEY),
                    String.valueOf(AppSettingsMigrations.LATEST_SCHEMA_VERSION));

            Properties reloaded = PreferencesStore.loadProperties(configPath);
            assertEquals(reloaded.getProperty("update_source_preference"), "gitee");
            assertFalse(reloaded.containsKey("update_source"));
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    @Test
    public void loadShouldKeepCurrentUpdateSourceWhenLegacyAliasAlsoExists() throws Exception {
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            Properties mixedSettings = new Properties();
            mixedSettings.setProperty("update_source", "github");
            mixedSettings.setProperty("update_source_preference", "gitee");
            PreferencesStore.storeProperties(mixedSettings, configPath);

            props.clear();
            SettingManager.load();

            assertEquals(props.getProperty("update_source_preference"), "gitee");
            assertFalse(props.containsKey("update_source"));
            assertEquals(props.getProperty(PreferencesStore.DEFAULT_SCHEMA_VERSION_KEY),
                    String.valueOf(AppSettingsMigrations.LATEST_SCHEMA_VERSION));
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    private static Properties getSettingsProperties() throws Exception {
        Field propsField = SettingManager.class.getDeclaredField("props");
        propsField.setAccessible(true);
        return (Properties) propsField.get(null);
    }

    private static void restoreConfig(Path configPath, boolean configExisted, String originalConfig) throws Exception {
        if (configExisted) {
            Files.writeString(configPath, originalConfig);
        } else {
            Files.deleteIfExists(configPath);
        }
    }
}
