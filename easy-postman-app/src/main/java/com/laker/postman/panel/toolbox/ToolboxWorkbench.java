package com.laker.postman.panel.toolbox;

import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.util.FontsUtil;
import lombok.experimental.UtilityClass;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Shared toolbox page chrome: compact IDEA-like toolbar rows plus borderless editor sections.
 */
@UtilityClass
class ToolboxWorkbench {
    private static final int ROOT_TOP = 8;
    private static final int ROOT_LEFT = 10;
    private static final int ROOT_BOTTOM = 8;
    private static final int ROOT_RIGHT = 10;
    private static final int SECTION_GAP = 4;

    void applyRoot(JPanel panel) {
        panel.setLayout(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyCard(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(ROOT_TOP, ROOT_LEFT, ROOT_BOTTOM, ROOT_RIGHT));
    }

    JPanel toolbar(Component leftActions, Component rightActions) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 8, 0));
        if (leftActions != null) {
            panel.add(leftActions, BorderLayout.WEST);
        }
        if (rightActions != null) {
            panel.add(rightActions, BorderLayout.EAST);
        }
        return panel;
    }

    JPanel optionsRow() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 6, 0));
        return panel;
    }

    JPanel stackedTop(Component toolbar, Component options) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);
        if (toolbar != null) {
            panel.add(toolbar, BorderLayout.NORTH);
        }
        if (options != null) {
            panel.add(options, BorderLayout.SOUTH);
        }
        return panel;
    }

    JPanel editorSection(String title, JComponent editor) {
        JPanel panel = new JPanel(new BorderLayout(0, SECTION_GAP));
        panel.setOpaque(false);
        panel.add(sectionTitle(title), BorderLayout.NORTH);
        panel.add(editor, BorderLayout.CENTER);
        return panel;
    }

    JPanel formSection(String title) {
        JPanel panel = new JPanel(new BorderLayout(8, 6));
        panel.setOpaque(false);
        panel.add(sectionTitle(title), BorderLayout.NORTH);
        return panel;
    }

    JSplitPane editorSplit(Component top, Component bottom, int dividerLocation) {
        JSplitPane splitPane = AppToolWindowChrome.createVerticalInnerSplitPane(top, bottom, dividerLocation);
        splitPane.setResizeWeight(0.5);
        return splitPane;
    }

    JSplitPane horizontalSplit(Component left, Component right, int dividerLocation) {
        JSplitPane splitPane = AppToolWindowChrome.createHorizontalInnerSplitPane(left, right, dividerLocation);
        splitPane.setResizeWeight(0.5);
        return splitPane;
    }

    JPanel statusBar(Component status) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));
        panel.add(status);
        return panel;
    }

    ToolWindowActionToolbar leftToolbar(Component... components) {
        return ToolWindowActionToolbar.inlineLeft(components);
    }

    ToolWindowActionToolbar rightToolbar(Component... components) {
        return ToolWindowActionToolbar.inlineRight(components);
    }

    JSeparator verticalSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, ToolWindowActionToolbar.ACTION_SIZE));
        return separator;
    }

    JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        return label;
    }
}
