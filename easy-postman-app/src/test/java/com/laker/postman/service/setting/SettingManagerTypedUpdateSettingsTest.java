package com.laker.postman.service.setting;

import com.laker.postman.common.constants.ConfigPathConstants;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.testng.Assert.assertEquals;

public class SettingManagerTypedUpdateSettingsTest {

    @Test
    public void shouldNormalizeInvalidStoredUpdateSettingsWithoutOverwritingValidSetterValues() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();
            props.setProperty("auto_update_check_frequency", "bad");
            props.setProperty("update_source_preference", "bad");
            props.setProperty("last_update_check_time", "-100");

            assertEquals(SettingManager.getAutoUpdateCheckFrequency(), "daily");
            assertEquals(SettingManager.getUpdateSourcePreference(), "auto");
            assertEquals(SettingManager.getLastUpdateCheckTime(), 0L);

            SettingManager.setAutoUpdateCheckFrequency("weekly");
            SettingManager.setUpdateSourcePreference("github");
            SettingManager.setLastUpdateCheckTime(123L);
            assertEquals(props.getProperty("auto_update_check_frequency"), "weekly");
            assertEquals(props.getProperty("update_source_preference"), "github");
            assertEquals(props.getProperty("last_update_check_time"), "123");

            SettingManager.setAutoUpdateCheckFrequency("bad");
            SettingManager.setUpdateSourcePreference("bad");
            SettingManager.setLastUpdateCheckTime(-1L);
            assertEquals(props.getProperty("auto_update_check_frequency"), "weekly");
            assertEquals(props.getProperty("update_source_preference"), "github");
            assertEquals(props.getProperty("last_update_check_time"), "0");
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
