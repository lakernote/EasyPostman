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
}
