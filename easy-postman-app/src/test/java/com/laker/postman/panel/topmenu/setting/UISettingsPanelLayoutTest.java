package com.laker.postman.panel.topmenu.setting;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class UISettingsPanelLayoutTest {

    @Test
    public void sidebarTabsHintShouldUseSharedWrappingHintComponent() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/UISettingsPanelModern.java"
        ));

        assertTrue(source.contains("SettingsHintLabel"));
        assertFalse(source.contains("new JLabel(\"<html>\" + I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_SIDEBAR_TABS_HINT)"));
    }

    @Test
    public void sidebarTabsEditorShouldUseDialogListSurface() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/UISettingsPanelModern.java"
        ));

        assertTrue(source.contains("ToolWindowSurfaceStyle.applyDialogListScrollPane(scrollPane, sidebarTabList)"));
        assertFalse(source.contains("ToolWindowSurfaceStyle.applyListScrollPaneCard(scrollPane, sidebarTabList)"));
    }

    @Test
    public void generalSettingsShouldKeepRelatedOptionsGrouped() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/UISettingsPanelModern.java"
        ));

        assertBefore(source, "maxOpenedRequestsCountField = new JTextField", "requestEditorTabsMultiLineCheckBox = new JCheckBox");
        assertBefore(source, "requestEditorTabsMultiLineCheckBox = new JCheckBox", "maxHistoryCountField = new JTextField");
        assertBefore(source, "sidebarExpandedCheckBox = new JCheckBox", "JPanel sidebarTabsRow = createSidebarTabsRow");
        assertBefore(source, "JPanel sidebarTabsRow = createSidebarTabsRow", "notificationPositionComboBox = new JComboBox");
        assertBefore(source, "notificationPositionComboBox = new JComboBox", "gitDiffLargeFileThresholdField = new JTextField");
    }

    @Test
    public void editorFontSettingsShouldBeSeparateFromUiFontSettings() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/UISettingsPanelModern.java"
        ));

        assertTrue(source.contains("private JPanel createUiFontSection()"));
        assertTrue(source.contains("private JPanel createEditorFontSection()"));
        assertBefore(source, "contentPanel.add(createUiFontSection())", "contentPanel.add(createEditorFontSection())");
        assertBefore(source, "createUiFontSection()", "createEditorFontSection()");
        assertFalse(source.contains(String.join("\n",
                "        int fontFieldLabelWidth = calculateFieldLabelWidth(List.of(",
                "                fontNameLabel,",
                "                fontSizeLabel,",
                "                editorFontNameLabel"
        )));
    }

    @Test
    public void uiFontSectionShouldInitializeWithLocaleAllowedSavedFont() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/UISettingsPanelModern.java"
        ));

        assertTrue(source.contains(
                "FontManager.resolveAllowedUiFontNameForLocale(SettingManager.getUiFontName(), I18nUtil.currentLocale())"
        ));
        assertBefore(
                source,
                "FontManager.resolveAllowedUiFontNameForLocale(SettingManager.getUiFontName(), I18nUtil.currentLocale())",
                "selectFontComboValue(fontNameComboBox, currentFont)"
        );
    }

    @Test
    public void editorFontPreviewShouldPaintWithFallbackFonts() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/UISettingsPanelModern.java"
        ));

        assertTrue(source.contains("new EditorFontPreviewLabel("));
        assertTrue(source.contains("private static final class EditorFontPreviewLabel extends JLabel"));
        assertTrue(source.contains("editorPreviewLabel.setPreviewFonts(primaryFont, fallbackFont);"));
    }

    @Test
    public void editorFontLabelsShouldUsePrimaryAndFallbackTerminology() throws Exception {
        Properties zh = loadProperties("src/main/resources/messages_zh.properties");
        Properties en = loadProperties("src/main/resources/messages_en.properties");
        String messageKeysSource = Files.readString(Path.of(
                "../easy-postman-foundation/src/main/java/com/laker/postman/util/MessageKeys.java"
        ));

        assertEquals(zh.getProperty("settings.editor.title"), "编辑器字体");
        assertEquals(zh.getProperty("settings.editor.font_name"), "编辑器主字体:");
        assertEquals(zh.getProperty("settings.editor.font_fallback_name"), "缺字回退字体:");
        assertTrue(zh.getProperty("settings.editor.description").contains("主字体缺字"));
        assertTrue(zh.getProperty("settings.editor.font_fallback_name.tooltip").contains("符号或 emoji"));
        assertFalse(zh.containsKey("settings.editor.font.default"));
        assertFalse(zh.containsKey("settings.editor.font.fallback_auto"));
        assertFalse(zh.containsKey("settings.editor.font.fallback_none"));

        assertEquals(en.getProperty("settings.editor.title"), "Editor Font");
        assertEquals(en.getProperty("settings.editor.font_name"), "Editor Primary Font:");
        assertEquals(en.getProperty("settings.editor.font_fallback_name"), "Missing Glyph Fallback Font:");
        assertTrue(en.getProperty("settings.editor.description").contains("missing glyphs"));
        assertTrue(en.getProperty("settings.editor.font_fallback_name.tooltip").contains("symbols, or emoji"));
        assertFalse(en.containsKey("settings.editor.font.default"));
        assertFalse(en.containsKey("settings.editor.font.fallback_auto"));
        assertFalse(en.containsKey("settings.editor.font.fallback_none"));
        assertFalse(messageKeysSource.contains("SETTINGS_EDITOR_FONT_DEFAULT ="));
        assertFalse(messageKeysSource.contains("SETTINGS_EDITOR_FONT_FALLBACK_AUTO ="));
        assertFalse(messageKeysSource.contains("SETTINGS_EDITOR_FONT_FALLBACK_NONE"));
    }

    @Test
    public void editorFontAutoOptionsShouldDisplayResolvedFontNames() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/UISettingsPanelModern.java"
        ));
        Properties zh = loadProperties("src/main/resources/messages_zh.properties");
        Properties en = loadProperties("src/main/resources/messages_en.properties");
        String messageKeysSource = Files.readString(Path.of(
                "../easy-postman-foundation/src/main/java/com/laker/postman/util/MessageKeys.java"
        ));

        assertEquals(zh.getProperty("settings.editor.font.default_resolved"), "自动选择（{0}）");
        assertEquals(zh.getProperty("settings.editor.font.fallback_auto_resolved"), "自动选择（{0}，推荐）");
        assertEquals(en.getProperty("settings.editor.font.default_resolved"), "Auto ({0})");
        assertEquals(en.getProperty("settings.editor.font.fallback_auto_resolved"), "Auto ({0}, Recommended)");
        assertTrue(messageKeysSource.contains("SETTINGS_EDITOR_FONT_DEFAULT_RESOLVED"));
        assertTrue(messageKeysSource.contains("SETTINGS_EDITOR_FONT_FALLBACK_AUTO_RESOLVED"));
        assertTrue(source.contains("createEditorPrimaryAutoFontLabel()"));
        assertTrue(source.contains("createEditorFallbackAutoFontLabel()"));
        assertTrue(source.contains("EditorFontManager.getDefaultEditorFontFamily()"));
        assertTrue(source.contains("EditorFontManager.getDefaultEditorFallbackFontFamily()"));
    }

    @Test
    public void editorFontSaveShouldRefreshEditorsWithoutReinstallingLookAndFeel() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/UISettingsPanelModern.java"
        ));

        String editorFontBranch = source.substring(
                source.indexOf("} else if (editorFontChanged) {"),
                source.indexOf("            }", source.indexOf("} else if (editorFontChanged) {")) + "            }".length()
        );

        assertTrue(editorFontBranch.contains("UIRefreshManager.refreshEditorFonts();"));
        assertFalse(editorFontBranch.contains("UIRefreshManager.refreshAllWindows();"));
    }

    private static void assertBefore(String source, String first, String second) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        assertTrue(firstIndex >= 0, "Missing source marker: " + first);
        assertTrue(secondIndex >= 0, "Missing source marker: " + second);
        assertTrue(firstIndex < secondIndex, first + " should appear before " + second);
    }

    private static Properties loadProperties(String path) throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(path))) {
            properties.load(reader);
        }
        return properties;
    }
}
