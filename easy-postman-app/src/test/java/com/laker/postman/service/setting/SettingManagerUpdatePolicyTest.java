package com.laker.postman.service.setting;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.platform.update.model.UpdateCheckFrequency;
import com.laker.postman.platform.update.model.UpdatePolicy;
import com.laker.postman.platform.update.model.UpdateTarget;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SettingManagerUpdatePolicyTest {

    @Test
    public void appUpdatePolicyShouldUseAppUpdateSettings() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("auto_update_check_enabled", "false");
            props.setProperty("auto_update_check_frequency", "weekly");

            UpdatePolicy policy = SettingManager.getAppUpdatePolicy();

            assertEquals(policy.target(), UpdateTarget.APP);
            assertFalse(policy.enabled());
            assertEquals(policy.frequency(), UpdateCheckFrequency.WEEKLY);
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void pluginUpdatePolicyShouldFallbackToGlobalUpdateSettings() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("auto_update_check_enabled", "false");
            props.setProperty("auto_update_check_frequency", "monthly");

            UpdatePolicy policy = SettingManager.getPluginUpdatePolicy();

            assertEquals(policy.target(), UpdateTarget.PLUGIN);
            assertFalse(policy.enabled());
            assertEquals(policy.frequency(), UpdateCheckFrequency.MONTHLY);
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void pluginUpdatePolicyShouldUseTargetSpecificSettingsWhenPresent() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("auto_update_check_enabled", "false");
            props.setProperty("auto_update_check_frequency", "monthly");
            props.setProperty("plugin_update_check_enabled", "true");
            props.setProperty("plugin_update_check_frequency", "startup");

            UpdatePolicy policy = SettingManager.getPluginUpdatePolicy();

            assertEquals(policy.target(), UpdateTarget.PLUGIN);
            assertTrue(policy.enabled());
            assertEquals(policy.frequency(), UpdateCheckFrequency.STARTUP);
        } finally {
            props.clear();
            props.putAll(backup);
        }
    }

    @Test
    public void appUpdateIgnoredMarkersShouldIgnoreBlankMarkersAndKeepExistingOnes() throws Exception {
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            props.setProperty("app_update_ignored_markers", "app@v1.2.0@UPDATE_AVAILABLE");

            SettingManager.rememberAppUpdateIgnoredMarker(" ");
            SettingManager.rememberAppUpdateIgnoredMarker("app@v1.3.0@UPDATE_AVAILABLE_NO_ASSET");

            assertTrue(SettingManager.getAppUpdateIgnoredMarkers().contains("app@v1.2.0@UPDATE_AVAILABLE"));
            assertEquals(
                    props.getProperty("app_update_ignored_markers"),
                    "app@v1.2.0@UPDATE_AVAILABLE,app@v1.3.0@UPDATE_AVAILABLE_NO_ASSET"
            );
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
