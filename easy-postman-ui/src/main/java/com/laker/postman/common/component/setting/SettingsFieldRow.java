package com.laker.postman.common.component.setting;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Label + editor row for settings forms.
 */
public class SettingsFieldRow extends JPanel {

    public static final int DEFAULT_LABEL_WIDTH = 220;
    public static final int DEFAULT_FIELD_WIDTH = 300;
    public static final int DEFAULT_ROW_HEIGHT = 36;
    public static final int DEFAULT_GAP = 16;

    private final JLabel label;

    public SettingsFieldRow(String labelText, String tooltip, JComponent inputComponent) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(ModernColors.getCardBackgroundColor());
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, DEFAULT_ROW_HEIGHT));

        label = new JLabel(labelText);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(ModernColors.getTextPrimary());
        label.setPreferredSize(new Dimension(DEFAULT_LABEL_WIDTH, 32));
        label.setMinimumSize(new Dimension(DEFAULT_LABEL_WIDTH, 32));
        label.setMaximumSize(new Dimension(DEFAULT_LABEL_WIDTH, 32));
        if (tooltip != null && !tooltip.isEmpty()) {
            label.setToolTipText(tooltip);
        }

        add(label);
        add(Box.createHorizontalStrut(DEFAULT_GAP));
        add(inputComponent);
        add(Box.createHorizontalGlue());
    }

    public JLabel label() {
        return label;
    }
}
