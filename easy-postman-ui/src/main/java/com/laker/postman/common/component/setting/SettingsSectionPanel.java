package com.laker.postman.common.component.setting;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Shared settings section container with title and optional wrapping description.
 */
public class SettingsSectionPanel extends JPanel {

    public static final int DEFAULT_DESCRIPTION_WIDTH = 640;
    private JLabel titleLabel;

    public SettingsSectionPanel(String title, String description) {
        this(title, description, DEFAULT_DESCRIPTION_WIDTH);
    }

    public SettingsSectionPanel(String title, String description, int descriptionWidth) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(0, 0, 18, 0));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        titleLabel = new JLabel(title);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        applySettingsSurface();

        if (description == null || description.isEmpty()) {
            titleLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
            add(titleLabel);
            return;
        }

        SettingsHintLabel descriptionLabel = new SettingsHintLabel(description, descriptionWidth);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        add(titleLabel);
        add(descriptionLabel);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applySettingsSurface();
    }

    private void applySettingsSurface() {
        setOpaque(false);
        setBackground(ModernColors.getDialogChromeBackgroundColor());
        if (titleLabel == null) {
            return;
        }
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        titleLabel.setForeground(ModernColors.getTextPrimary());
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension preferredSize = getPreferredSize();
        return new Dimension(Short.MAX_VALUE, preferredSize.height);
    }
}
