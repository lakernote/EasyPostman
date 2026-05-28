package com.laker.postman.panel.performance;


import javax.swing.*;
import java.awt.*;

public final class PerformanceStagePropertyLayout {
    public static final int FIELD_HEIGHT = 28;
    public static final int SPINNER_FIELD_WIDTH = 118;
    public static final int TEXT_FIELD_WIDTH = 320;

    private PerformanceStagePropertyLayout() {
    }

    public static void applyCompactBorder(JComponent component) {
        component.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
    }

    public static GridBagConstraints createBaseConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        return gbc;
    }

    public static void configureFieldWidth(JComponent component, int preferredWidth, int minimumWidth) {
        Dimension preferredSize = new Dimension(preferredWidth, FIELD_HEIGHT);
        Dimension minimumSize = new Dimension(minimumWidth, FIELD_HEIGHT);
        component.setPreferredSize(preferredSize);
        component.setMinimumSize(minimumSize);
    }

    public static void addCenteredCompactFormRow(JPanel target, GridBagConstraints gbc, JComponent label, JComponent field) {
        int previousFill = gbc.fill;
        int previousAnchor = gbc.anchor;

        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        rowPanel.setOpaque(false);
        rowPanel.add(label);
        rowPanel.add(field);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        target.add(rowPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = previousFill;
        gbc.anchor = previousAnchor;
    }

    public static void addVerticalFiller(JPanel target, GridBagConstraints gbc, int gridWidth) {
        gbc.gridx = 0;
        gbc.gridwidth = gridWidth;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        target.add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
    }
}
