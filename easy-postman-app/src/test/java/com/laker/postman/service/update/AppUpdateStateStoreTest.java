package com.laker.postman.service.update;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.platform.update.model.UpdateCheckState;
import com.laker.postman.platform.update.model.UpdateTarget;
import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AppUpdateStateStoreTest {

    private Path pluginDataDir;

    @BeforeMethod
    public void setUp() throws Exception {
        pluginDataDir = Files.createTempDirectory("app-update-state-store-test");
        System.setProperty("easyPostman.data.dir", pluginDataDir.toString());
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty("easyPostman.data.dir");
        PluginRuntime.resetForTests();
    }

    @Test
    public void appTargetShouldReadAndWriteAppUpdateState() throws Exception {
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);

        try {
            props.clear();
            AppUpdateStateStore store = new AppUpdateStateStore();

            store.recordCheck(UpdateTarget.APP, 12_345L);
            store.rememberNotifiedMarker(UpdateTarget.APP, "app@1.2.0@UPDATE_AVAILABLE");
            store.rememberIgnoredMarker(UpdateTarget.APP, "app@1.3.0@UPDATE_AVAILABLE");

            UpdateCheckState state = store.state(UpdateTarget.APP);
            assertEquals(state.target(), UpdateTarget.APP);
            assertEquals(state.lastCheckTimeMillis(), 12_345L);
            assertFalse(state.wasNotified("app@1.2.0@UPDATE_AVAILABLE"));
            assertTrue(store.ignoredMarkers(UpdateTarget.APP).contains("app@1.3.0@UPDATE_AVAILABLE"));
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    @Test
    public void pluginTargetShouldReadAndWritePluginUpdateState() {
        AppUpdateStateStore store = new AppUpdateStateStore();

        store.recordCheck(UpdateTarget.PLUGIN, 54_321L);
        store.rememberNotifiedMarker(UpdateTarget.PLUGIN, "plugin-redis@6.0.2");
        store.rememberNotifiedMarker(UpdateTarget.PLUGIN, " ");

        UpdateCheckState state = store.state(UpdateTarget.PLUGIN);
        assertEquals(state.target(), UpdateTarget.PLUGIN);
        assertEquals(state.lastCheckTimeMillis(), 54_321L);
        assertTrue(state.wasNotified("plugin-redis@6.0.2"));
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
