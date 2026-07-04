package com.laker.postman.util;

import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import java.awt.Font;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class EditorFontManagerTest {

    @Test
    public void configuredEditorFontShouldUseIndependentEditorSettings() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();
            SettingManager.setUiFontSize(24);
            SettingManager.setEditorFontName(Font.MONOSPACED);
            SettingManager.setEditorFontSize(17);

            Font font = EditorFontManager.getConfiguredEditorFont();

            assertEquals(font.getFamily(), Font.MONOSPACED);
            assertEquals(font.getSize(), 17);
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    @Test
    public void blankEditorFallbackSettingShouldUseAutomaticFallbackFont() throws Exception {
        Properties props = getSettingsProperties();
        Properties backup = new Properties();
        backup.putAll(props);
        Path configPath = Path.of(ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        boolean configExisted = Files.exists(configPath);
        String originalConfig = configExisted ? Files.readString(configPath) : null;

        try {
            props.clear();
            SettingManager.setEditorFontFallbackName("");
            SettingManager.setEditorFontSize(15);

            Font fallbackFont = EditorFontManager.getConfiguredEditorFallbackFont();

            assertNotNull(fallbackFont);
            assertEquals(fallbackFont.getSize(), 15);
        } finally {
            props.clear();
            props.putAll(backup);
            restoreConfig(configPath, configExisted, originalConfig);
        }
    }

    @Test
    public void defaultEditorFallbackFontShouldPreferWindowsNativeCjkCandidatesOnWindows() {
        String fallback = EditorFontManager.resolveDefaultEditorFallbackFontFamily(List.of(
                "Consolas",
                "PingFang SC",
                "Microsoft YaHei UI"
        ), "Windows 11");

        assertEquals(fallback, "Microsoft YaHei UI");
    }

    @Test
    public void defaultEditorFallbackFontShouldPreferMacNativeCjkCandidatesOnMacos() {
        String fallback = EditorFontManager.resolveDefaultEditorFallbackFontFamily(List.of(
                "Consolas",
                "PingFang SC",
                "Microsoft YaHei UI"
        ), "Mac OS X");

        assertEquals(fallback, "PingFang SC");
    }

    @Test
    public void defaultEditorFallbackFontShouldPreferLinuxNativeCjkCandidatesOnLinux() {
        String fallback = EditorFontManager.resolveDefaultEditorFallbackFontFamily(List.of(
                "Consolas",
                "Noto Sans CJK SC",
                "Microsoft YaHei UI",
                "PingFang SC"
        ), "Linux");

        assertEquals(fallback, "Noto Sans CJK SC");
    }

    @Test
    public void defaultEditorFontResolutionShouldUseCachedFontCatalog() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/laker/postman/util/EditorFontManager.java"));

        assertTrue(source.contains("cachedAvailableFontFamilyNames"),
                "Editor font auto resolution should cache the system font family list");
        assertEquals(
                countOccurrences(source, "GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()"),
                1,
                "Editor font auto resolution should have exactly one system font enumeration point"
        );
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
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
