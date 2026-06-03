package com.laker.postman.common.component.setting;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Checkbox row for settings forms.
 */
public class SettingsCheckBoxRow extends JPanel {

    public static final int DEFAULT_ROW_HEIGHT = 36;

    public SettingsCheckBoxRow(JCheckBox checkBox, String tooltip) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(ModernColors.getCardBackgroundColor());
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, DEFAULT_ROW_HEIGHT));

        checkBox.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        checkBox.setForeground(ModernColors.getTextPrimary());
        checkBox.setBackground(ModernColors.getCardBackgroundColor());
        checkBox.setFocusPainted(false);
        if (tooltip != null && !tooltip.isEmpty()) {
            checkBox.setToolTipText(tooltip);
        }
        checkBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (checkBox.isEnabled()) {
                    checkBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                checkBox.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        add(checkBox);
        add(Box.createHorizontalGlue());
    }
}
