package com.laker.postman.common.component;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Consistent compact action row for tool-window cards and side panels.
 */
public final class ToolWindowActionToolbar extends JPanel {
    public static final int HORIZONTAL_PADDING = 8;
    public static final int VERTICAL_PADDING = 4;
    public static final int ACTION_SIZE = 28;
    public static final int CONTROL_GAP = 4;

    private ToolWindowActionToolbar(boolean alignRight,
                                    int horizontalPadding,
                                    int verticalPadding,
                                    Component... components) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding));

        if (alignRight) {
            add(Box.createHorizontalGlue());
        }

        for (int i = 0; i < components.length; i++) {
            if (i > 0) {
                add(Box.createHorizontalStrut(CONTROL_GAP));
            }
            Component component = components[i];
            configureComponent(component);
            add(component);
        }
    }

    public static ToolWindowActionToolbar left(Component... components) {
        return new ToolWindowActionToolbar(false, HORIZONTAL_PADDING, VERTICAL_PADDING, components);
    }

    public static ToolWindowActionToolbar right(Component... components) {
        return new ToolWindowActionToolbar(true, HORIZONTAL_PADDING, VERTICAL_PADDING, components);
    }

    public static ToolWindowActionToolbar inlineLeft(Component... components) {
        return new ToolWindowActionToolbar(false, 0, 0, components);
    }

    public static ToolWindowActionToolbar inlineRight(Component... components) {
        return new ToolWindowActionToolbar(true, 0, 0, components);
    }

    private static void configureComponent(Component component) {
        if (component instanceof JComponent jComponent) {
            jComponent.setAlignmentY(Component.CENTER_ALIGNMENT);
        }
        if (component instanceof AbstractButton button
                && !(button instanceof JCheckBox)
                && !(button instanceof JRadioButton)) {
            String text = button.getText();
            int width = text == null || text.isBlank()
                    ? ACTION_SIZE
                    : Math.max(button.getPreferredSize().width, ACTION_SIZE);
            Dimension size = new Dimension(width, ACTION_SIZE);
            button.setPreferredSize(size);
            button.setMinimumSize(size);
            button.setMaximumSize(size);
        }
    }
}
