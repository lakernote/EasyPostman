package com.laker.postman.service.setting;

import com.laker.postman.common.constants.ConfigPathConstants;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.testng.Assert.assertEquals;

public class SettingManagerEditorFontSettingsTest {

    @Test
    public void editorFontSettingsShouldUseTypedKeysAndClampSize() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();

            assertEquals(SettingManager.getEditorFontName(), "");
            assertEquals(SettingManager.getEditorFontFallbackName(), "");
            assertEquals(SettingManager.getEditorFontSize(), 13);

            SettingManager.setEditorFontName("Consolas");
            SettingManager.setEditorFontFallbackName("Microsoft YaHei UI");
            SettingManager.setEditorFontSize(99);

            assertEquals(SettingManager.getEditorFontName(), "Consolas");
            assertEquals(SettingManager.getEditorFontFallbackName(), "Microsoft YaHei UI");
            assertEquals(SettingManager.getEditorFontSize(), 32);
            assertEquals(props.getProperty("editor_font_name"), "Consolas");
            assertEquals(props.getProperty("editor_font_fallback_name"), "Microsoft YaHei UI");
            assertEquals(props.getProperty("editor_font_size"), "32");
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
