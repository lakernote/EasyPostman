package com.laker.postman.common.component.setting;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class SettingsSurfaceStyleTest {
    private Map<String, Object> previousTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousTokens = new HashMap<>();
        for (String key : new String[]{ThemeColors.BACKGROUND, ThemeColors.SURFACE}) {
            previousTokens.put(key, UIManager.get(key));
        }
    }

    @AfterMethod
    public void restoreThemeTokens() {
        previousTokens.forEach(UIManager::put);
    }

    @Test
    public void settingsContainersShouldUseDialogBackgroundInsteadOfCardSurface() {
        Color dialogBackground = new Color(233, 234, 238);
        Color cardSurface = new Color(255, 255, 255);
        UIManager.put(ThemeColors.BACKGROUND, dialogBackground);
        UIManager.put(ThemeColors.SURFACE, cardSurface);

        SettingsSectionPanel section = new SettingsSectionPanel("General", "");
        SettingsFieldRow fieldRow = new SettingsFieldRow("Name", "", new JTextField());
        JCheckBox checkBox = new JCheckBox("Enabled");
        SettingsCheckBoxRow checkBoxRow = new SettingsCheckBoxRow(checkBox, "");

        assertFalse(section.isOpaque());
        assertFalse(fieldRow.isOpaque());
        assertFalse(checkBoxRow.isOpaque());
        assertEquals(section.getBackground(), dialogBackground);
        assertEquals(fieldRow.getBackground(), dialogBackground);
        assertEquals(checkBoxRow.getBackground(), dialogBackground);
        assertEquals(checkBox.getBackground(), dialogBackground);
    }
}
