package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Consistent title/action row for compact tool-window sidebars.
 */
public final class ToolWindowSidebarHeader extends JPanel {
    public static final int HORIZONTAL_PADDING = 8;
    public static final int VERTICAL_PADDING = 4;
    public static final int ACTION_SIZE = 28;
    public static final int ACTION_GAP = 4;

    private JLabel titleLabel;

    public ToolWindowSidebarHeader(String title, AbstractButton... actions) {
        super(new BorderLayout(ACTION_GAP, 0));
        setOpaque(false);

        titleLabel = new JLabel(title);
        titleLabel.setVerticalAlignment(JLabel.CENTER);
        add(titleLabel, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel();
        actionsPanel.setOpaque(false);
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.X_AXIS));
        for (int i = 0; i < actions.length; i++) {
            if (i > 0) {
                actionsPanel.add(Box.createHorizontalStrut(ACTION_GAP));
            }
            configureAction(actions[i]);
            actionsPanel.add(actions[i]);
        }
        if (actions.length > 0) {
            add(actionsPanel, BorderLayout.EAST);
        }

        refreshStyle();
    }

    public JLabel getTitleLabel() {
        return titleLabel;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        refreshStyle();
    }

    private void refreshStyle() {
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, ACTION_GAP)
        ));
        if (titleLabel != null) {
            titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
            titleLabel.setForeground(ModernColors.getTextPrimary());
        }
    }

    private static void configureAction(AbstractButton button) {
        Dimension size = new Dimension(ACTION_SIZE, ACTION_SIZE);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setAlignmentY(Component.CENTER_ALIGNMENT);
    }
}
