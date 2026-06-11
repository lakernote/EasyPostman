package com.laker.postman.common.component.setting;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyPasswordField;
import org.testng.annotations.Test;

import javax.swing.JTextField;
import javax.swing.border.Border;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SettingsTextFieldValidatorTest {

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
