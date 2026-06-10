package com.laker.postman.panel.sidebar;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Manages the sidebar console area and its collapsed bottom bar state.
 */
final class SidebarConsoleArea {
    static final int DEFAULT_EXPANDED_CONSOLE_HEIGHT = 300;
    static final int EXPANDED_CONSOLE_DIVIDER_SIZE = 3;
    private static final int MIN_EXPANDED_CONSOLE_HEIGHT = 160;

    private final JPanel owner;
    private final SidebarBottomBar bottomBar;
    private final JPanel consoleContainer = new JPanel(new BorderLayout());
    private final ConsolePanel consolePanel;
    private JTabbedPane tabbedPane;
    private boolean expanded;

    SidebarConsoleArea(JPanel owner, SidebarBottomBar bottomBar) {
        this.owner = owner;
        this.bottomBar = bottomBar;
        this.consolePanel = UiSingletonFactory.getInstance(ConsolePanel.class);
        this.consolePanel.setCloseAction(e -> collapse());
        consoleContainer.setOpaque(false);
        ToolWindowSurfaceStyle.applyBackground(consoleContainer);
        refreshTheme();
    }

    void setTabbedPane(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
        render();
    }

    void expand() {
        expanded = true;
        render();
    }

    void refreshTheme() {
        consoleContainer.setBorder(BorderFactory.createEmptyBorder());
    }

    private void collapse() {
        expanded = false;
        render();
    }

    private void render() {
        if (tabbedPane == null) {
            return;
        }
        owner.removeAll();
        if (expanded) {
            showExpandedConsole();
        } else {
            showCollapsedBottomBar();
        }
        owner.revalidate();
        owner.repaint();
    }

    private void showExpandedConsole() {
        consoleContainer.removeAll();
        consoleContainer.add(consolePanel, BorderLayout.CENTER);
        consoleContainer.setPreferredSize(new Dimension(0, DEFAULT_EXPANDED_CONSOLE_HEIGHT));

        JSplitPane splitPane = createExpandedConsoleSplitPane(tabbedPane, consoleContainer);
        splitPane.setResizeWeight(1.0);
        splitPane.setMinimumSize(new Dimension(0, 10));
        tabbedPane.setMinimumSize(new Dimension(0, 30));
        consoleContainer.setMinimumSize(new Dimension(0, 30));

        owner.add(splitPane, BorderLayout.CENTER);
        installInitialExpandedConsoleDivider(splitPane);
    }

    static JSplitPane createExpandedConsoleSplitPane(JTabbedPane tabbedPane, JPanel consoleContainer) {
        JSplitPane splitPane = AppToolWindowChrome.createVerticalInnerSplitPane(tabbedPane, consoleContainer, 0);
        splitPane.setDividerSize(EXPANDED_CONSOLE_DIVIDER_SIZE);
        return splitPane;
    }

    private void showCollapsedBottomBar() {
        consoleContainer.removeAll();
        consoleContainer.setPreferredSize(null);
        consoleContainer.setMinimumSize(null);
        consoleContainer.add(bottomBar.leftPanel(), BorderLayout.WEST);
        consoleContainer.add(bottomBar.rightPanel(), BorderLayout.EAST);

        owner.add(tabbedPane, BorderLayout.CENTER);
        owner.add(consoleContainer, BorderLayout.SOUTH);
    }

    private void installInitialExpandedConsoleDivider(JSplitPane splitPane) {
        ComponentAdapter listener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (applyInitialExpandedConsoleDivider(splitPane)) {
                    splitPane.removeComponentListener(this);
                }
            }
        };
        splitPane.addComponentListener(listener);
        SwingUtilities.invokeLater(() -> {
            if (applyInitialExpandedConsoleDivider(splitPane)) {
                splitPane.removeComponentListener(listener);
            }
        });
    }

    private boolean applyInitialExpandedConsoleDivider(JSplitPane splitPane) {
        int splitHeight = splitPane.getHeight();
        int readyHeight = MIN_EXPANDED_CONSOLE_HEIGHT + Math.max(0, splitPane.getDividerSize());
        if (splitHeight <= readyHeight || !expanded || splitPane.getParent() == null) {
            return false;
        }
        splitPane.setDividerLocation(defaultExpandedConsoleDividerLocation(
                splitHeight,
                splitPane.getDividerSize()
        ));
        return true;
    }

    static int defaultExpandedConsoleDividerLocation(int splitHeight, int dividerSize) {
        int consoleHeight = defaultExpandedConsoleHeight(splitHeight);
        return Math.max(0, splitHeight - consoleHeight - Math.max(0, dividerSize));
    }

    private static int defaultExpandedConsoleHeight(int splitHeight) {
        if (splitHeight <= 0) {
            return DEFAULT_EXPANDED_CONSOLE_HEIGHT;
        }
        int maxReasonableHeight = Math.max(MIN_EXPANDED_CONSOLE_HEIGHT, splitHeight / 2);
        return Math.min(DEFAULT_EXPANDED_CONSOLE_HEIGHT, maxReasonableHeight);
    }
}
