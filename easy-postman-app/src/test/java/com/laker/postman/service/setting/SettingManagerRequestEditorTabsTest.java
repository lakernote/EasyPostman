package com.laker.postman.service.setting;

import com.laker.postman.common.constants.ConfigPathConstants;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SettingManagerRequestEditorTabsTest {

    @Test
    public void requestEditorTabsShouldBeVisibleByDefaultAndRespectHiddenSet() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();

            assertTrue(SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_DOCS));
            assertTrue(SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_PARAMS));
            assertFalse(SettingManager.isRequestEditorTabsMultiLineEnabled());

            SettingManager.setHiddenRequestEditorTabs(List.of(
                    SettingManager.REQUEST_EDITOR_TAB_DOCS,
                    "body"
            ));

            assertFalse(SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_DOCS));
            assertFalse(SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_BODY));
            assertTrue(SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_HEADERS));
            assertEquals(
                    SettingManager.getHiddenRequestEditorTabs(),
                    java.util.Set.of(SettingManager.REQUEST_EDITOR_TAB_DOCS, SettingManager.REQUEST_EDITOR_TAB_BODY)
            );

            SettingManager.setHiddenRequestEditorTabs(List.of());
            assertTrue(SettingManager.isRequestEditorTabVisible(SettingManager.REQUEST_EDITOR_TAB_DOCS));
            assertFalse(props.containsKey("request_editor_hidden_tabs"));

            SettingManager.setRequestEditorTabsMultiLineEnabled(true);
            assertTrue(SettingManager.isRequestEditorTabsMultiLineEnabled());
            assertEquals(props.getProperty("request_editor_tabs_multiline"), "true");

            SettingManager.setRequestEditorTabsMultiLineEnabled(false);
            assertFalse(SettingManager.isRequestEditorTabsMultiLineEnabled());
            assertEquals(props.getProperty("request_editor_tabs_multiline"), "false");
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
