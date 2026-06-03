package com.laker.postman.common.component.setting;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import lombok.experimental.UtilityClass;

import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Cursor;
import java.awt.Font;

/**
 * Shared styling for settings form inputs.
 */
@UtilityClass
public class SettingsInputStyle {

    private static final String TEXT_FIELD_STYLE = "arc: 8; margin: 7, 12, 7, 12";

    public static void apply(JComponent component) {
        if (component == null) {
            return;
        }
        component.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        component.setBackground(ModernColors.getInputBackgroundColor());
        component.setForeground(ModernColors.getTextPrimary());

        if (component instanceof JTextField field) {
            field.putClientProperty(FlatClientProperties.STYLE, TEXT_FIELD_STYLE);
            field.putClientProperty(FlatClientProperties.OUTLINE, null);
        } else if (component instanceof JComboBox<?> comboBox) {
            comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            comboBox.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        }
    }
}
