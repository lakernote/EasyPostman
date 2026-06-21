package com.laker.postman.common.component.setting;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SettingsSurfaceStyleTest {
    private Map<String, Object> previousTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousTokens = new HashMap<>();
        for (String key : new String[]{
                ThemeColors.BACKGROUND,
                ThemeColors.DIALOG_CHROME_BACKGROUND,
                ThemeColors.SURFACE,
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_SECONDARY
        }) {
            previousTokens.put(key, UIManager.get(key));
        }
    }

    @AfterMethod
    public void restoreThemeTokens() {
        previousTokens.forEach(UIManager::put);
    }

    @Test
    public void settingsContainersShouldUseDialogChromeBackgroundInsteadOfMainWindowBackground() {
        Color mainWindowBackground = new Color(233, 234, 238);
        Color dialogBackground = new Color(247, 248, 249);
        Color cardSurface = new Color(255, 255, 255);
        UIManager.put(ThemeColors.BACKGROUND, mainWindowBackground);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, dialogBackground);
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
        assertFalse(checkBox.isContentAreaFilled());
        assertTrue(checkBox.isFocusPainted());
    }

    @Test
    public void settingsRowsShouldRefreshAgainstLightAndDarkDialogChromeTokens() {
        Color lightDialogBackground = new Color(247, 248, 249);
        Color lightText = new Color(15, 23, 42);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, lightDialogBackground);
        UIManager.put(ThemeColors.TEXT_PRIMARY, lightText);

        SettingsSectionPanel section = new SettingsSectionPanel("General", "");
        SettingsFieldRow fieldRow = new SettingsFieldRow("Name", "", new JTextField());
        JCheckBox checkBox = new JCheckBox("Enabled");
        SettingsCheckBoxRow checkBoxRow = new SettingsCheckBoxRow(checkBox, "");

        assertEquals(section.getBackground(), lightDialogBackground);
        assertEquals(fieldRow.getBackground(), lightDialogBackground);
        assertEquals(checkBoxRow.getBackground(), lightDialogBackground);
        assertEquals(checkBox.getBackground(), lightDialogBackground);

        Color darkDialogBackground = new Color(30, 31, 34);
        Color darkText = new Color(201, 204, 211);
        UIManager.put(ThemeColors.DIALOG_CHROME_BACKGROUND, darkDialogBackground);
        UIManager.put(ThemeColors.TEXT_PRIMARY, darkText);

        SwingUtilities.updateComponentTreeUI(section);
        SwingUtilities.updateComponentTreeUI(fieldRow);
        SwingUtilities.updateComponentTreeUI(checkBoxRow);

        assertEquals(section.getBackground(), darkDialogBackground);
        assertEquals(fieldRow.getBackground(), darkDialogBackground);
        assertEquals(checkBoxRow.getBackground(), darkDialogBackground);
        assertEquals(checkBox.getBackground(), darkDialogBackground);
        assertEquals(checkBox.getForeground(), darkText);
    }

    @Test
    public void settingsFieldLabelsShouldKeepLeftAlignedSettingsRhythm() {
        SettingsFieldRow row = new SettingsFieldRow("Max idle connections:", "", new JTextField());

        assertEquals(row.label().getHorizontalAlignment(), JLabel.LEADING);
    }
}
