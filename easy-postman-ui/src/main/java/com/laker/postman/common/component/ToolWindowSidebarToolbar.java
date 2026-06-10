package com.laker.postman.common.component;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Consistent toolbar used at the top of rounded tool-window sidebars.
 */
public final class ToolWindowSidebarToolbar extends JPanel {
    public static final int HORIZONTAL_PADDING = 8;
    public static final int VERTICAL_PADDING = 6;
    public static final int ACTION_SIZE = 32;
    public static final int SEARCH_HEIGHT = 28;
    public static final int CONTROL_GAP = 6;

    public ToolWindowSidebarToolbar(AbstractButton leadingAction, SearchTextField searchField) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING));

        if (leadingAction != null) {
            configureAction(leadingAction);
            add(leadingAction);
            add(Box.createHorizontalStrut(CONTROL_GAP));
        }
        configureSearchField(searchField);
        add(searchField);
    }

    private static void configureAction(AbstractButton button) {
        Dimension size = new Dimension(ACTION_SIZE, ACTION_SIZE);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setAlignmentY(Component.CENTER_ALIGNMENT);
    }

    private static void configureSearchField(SearchTextField searchField) {
        Dimension preferredSize = searchField.getPreferredSize();
        int preferredWidth = Math.max(preferredSize.width, 220);
        searchField.setPreferredSize(new Dimension(preferredWidth, SEARCH_HEIGHT));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, SEARCH_HEIGHT));
        searchField.setMinimumSize(new Dimension(50, SEARCH_HEIGHT));
        searchField.setAlignmentY(Component.CENTER_ALIGNMENT);
        searchField.installUserActivatedFocus();
    }
}
