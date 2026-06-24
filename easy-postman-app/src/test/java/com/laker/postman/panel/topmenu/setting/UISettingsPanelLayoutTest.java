package com.laker.postman.panel.topmenu.setting;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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

    private static void assertBefore(String source, String first, String second) {
        int firstIndex = source.indexOf(first);
        int secondIndex = source.indexOf(second);
        assertTrue(firstIndex >= 0, "Missing source marker: " + first);
        assertTrue(secondIndex >= 0, "Missing source marker: " + second);
        assertTrue(firstIndex < secondIndex, first + " should appear before " + second);
    }
}
