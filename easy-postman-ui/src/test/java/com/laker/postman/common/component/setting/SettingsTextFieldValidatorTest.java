package com.laker.postman.common.component.setting;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyPasswordField;
import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SettingsTextFieldValidatorTest {
    private Map<String, Object> previousTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousTokens = new HashMap<>();
        for (String key : new String[]{
                ThemeColors.INPUT_BACKGROUND,
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_DISABLED,
                "TextField.disabledBackground"
        }) {
            previousTokens.put(key, UIManager.get(key));
        }
    }

    @AfterMethod
    public void restoreThemeTokens() {
        previousTokens.forEach(UIManager::put);
    }

    @Test
    public void settingsInputStyleShouldUseFlatLafPropertiesInsteadOfCustomBorders() {
        JTextField field = new JTextField();
        Border originalBorder = field.getBorder();

        SettingsInputStyle.apply(field);

        assertSame(field.getBorder(), originalBorder,
                "Settings text fields should keep FlatLaf's own border/focus rendering");
        assertTrue(Objects.toString(field.getClientProperty(FlatClientProperties.STYLE), "")
                .contains("arc"));
    }

    @Test
    public void settingsInputStyleShouldPreservePasswordRevealButton() {
        EasyPasswordField field = new EasyPasswordField();

        SettingsInputStyle.apply(field);

        String style = Objects.toString(field.getClientProperty(FlatClientProperties.STYLE), "");
        assertTrue(style.contains("arc"));
        assertTrue(style.contains("showRevealButton: true"));
    }

    @Test
    public void settingsFieldRowShouldDimLabelAndInputWhenDisabled() {
        Color inputBackground = new Color(255, 255, 255);
        Color disabledBackground = new Color(241, 243, 246);
        Color textPrimary = new Color(15, 23, 42);
        Color textDisabled = new Color(148, 163, 184);
        UIManager.put(ThemeColors.INPUT_BACKGROUND, inputBackground);
        UIManager.put(ThemeColors.TEXT_PRIMARY, textPrimary);
        UIManager.put(ThemeColors.TEXT_DISABLED, textDisabled);
        UIManager.put("TextField.disabledBackground", disabledBackground);
        JTextField field = new JTextField("3000");
        SettingsInputStyle.apply(field);
        SettingsFieldRow row = new SettingsFieldRow("Remote script timeout:", "", field);

        row.setEnabled(false);

        assertFalse(row.label().isEnabled());
        assertFalse(field.isEnabled());
        assertEquals(row.label().getForeground(), textDisabled);
        assertEquals(field.getBackground(), disabledBackground);
        assertEquals(field.getDisabledTextColor(), textDisabled);

        row.setEnabled(true);

        assertTrue(row.label().isEnabled());
        assertTrue(field.isEnabled());
        assertEquals(row.label().getForeground(), textPrimary);
        assertEquals(field.getBackground(), inputBackground);
        assertEquals(field.getForeground(), textPrimary);
    }

    @Test
    public void validatorShouldUseFlatLafErrorOutlineAndClearItWhenValid() {
        JTextField field = new JTextField();
        SettingsTextFieldValidator validator = SettingsTextFieldValidator.install(
                field,
                value -> value.matches("\\d+"),
                "Only digits"
        );

        field.setText("abc");

        assertTrue(validator.hasValidationError());
        assertEquals(field.getClientProperty(FlatClientProperties.OUTLINE), FlatClientProperties.OUTLINE_ERROR);
        assertEquals(field.getToolTipText(), "Only digits");

        field.setText("123");

        assertFalse(validator.hasValidationError());
        assertEquals(field.getClientProperty(FlatClientProperties.OUTLINE), null);
        assertEquals(field.getToolTipText(), null);
    }
}
