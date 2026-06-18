package com.laker.postman.panel.sidebar;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Manages the sidebar console tool window and keeps the bottom toolbar outside the console split.
 */
final class SidebarConsoleArea {
    static final int DEFAULT_CONSOLE_HEIGHT = 300;
    static final int CONSOLE_DIVIDER_SIZE = AppToolWindowChrome.STACKED_DIVIDER_SIZE;
    private static final int MIN_CONSOLE_HEIGHT = 160;

    private final JPanel owner;
    private final SidebarBottomBar bottomBar;
    private final JPanel consoleContainer = new JPanel(new BorderLayout());
    private final JPanel bottomBarContainer = new JPanel(new BorderLayout());
    private final JComponent consolePanel;
    private final JComponent sideAssistant;
    private JTabbedPane tabbedPane;
    private SidebarTabContentHost consoleHost;
    private JSplitPane consoleSplitPane;
    private boolean consoleVisible;

    SidebarConsoleArea(JPanel owner, SidebarBottomBar bottomBar) {
        this(owner, bottomBar, UiSingletonFactory.getInstance(ConsolePanel.class), null);
    }

    SidebarConsoleArea(JPanel owner, SidebarBottomBar bottomBar, JComponent consolePanel) {
        this(owner, bottomBar, consolePanel, null);
    }

    SidebarConsoleArea(JPanel owner, SidebarBottomBar bottomBar, JComponent consolePanel, JComponent sideAssistant) {
        this.owner = owner;
        this.bottomBar = bottomBar;
        this.consolePanel = consolePanel;
        this.sideAssistant = sideAssistant;
        if (consolePanel instanceof ConsolePanel panel) {
            panel.setHideAction(e -> hideConsole());
        }
        consoleContainer.setOpaque(false);
        bottomBarContainer.setOpaque(false);
        ToolWindowSurfaceStyle.applyBackground(bottomBarContainer);
        refreshTheme();
    }

    void setTabbedPane(JTabbedPane tabbedPane) {
        restoreContentHost();
        this.tabbedPane = tabbedPane;
        render();
    }

    void showConsole() {
        consoleVisible = true;
        render();
    }

    void toggleConsole() {
        consoleVisible = !consoleVisible;
        render();
    }

    void refreshTheme() {
        consoleContainer.setBorder(BorderFactory.createEmptyBorder());
        bottomBarContainer.setBorder(BorderFactory.createEmptyBorder());
        Dimension bottomBarSize = new Dimension(0, SidebarBottomBar.STRIPE_THICKNESS);
        bottomBarContainer.setPreferredSize(bottomBarSize);
        bottomBarContainer.setMinimumSize(bottomBarSize);
    }

    void handleSelectedTabChanged() {
        if (consoleVisible) {
            render();
        }
    }

    private void hideConsole() {
        consoleVisible = false;
        render();
    }

    private void render() {
        if (tabbedPane == null) {
            return;
        }
        owner.removeAll();
        if (consoleVisible) {
            renderVisibleConsole();
        } else {
            renderHiddenConsole();
        }
        owner.revalidate();
        owner.repaint();
    }

    private void renderVisibleConsole() {
        owner.add(tabbedPane, BorderLayout.CENTER);
        renderSideAssistant();
        renderBottomBar();
        owner.add(bottomBarContainer, BorderLayout.SOUTH);
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }
        SidebarTabContentHost selectedHost = SidebarTabContentHost.from(tabbedPane.getComponentAt(selectedIndex));
        if (selectedHost == null) {
            return;
        }
        if (consoleHost == selectedHost && consoleSplitPane != null) {
            return;
        }
        restoreContentHost();

        Component selectedContent = selectedHost.content();
        JComponent resizableContent = createResizableContentWrapper(selectedContent);
        consoleContainer.removeAll();
        consoleContainer.add(consolePanel, BorderLayout.CENTER);
        consoleContainer.setPreferredSize(new Dimension(0, DEFAULT_CONSOLE_HEIGHT));

        JSplitPane splitPane = createConsoleSplitPane(resizableContent, consoleContainer);
        splitPane.setResizeWeight(1.0);
        splitPane.setMinimumSize(new Dimension(0, 10));
        consoleContainer.setMinimumSize(new Dimension(0, 30));

        consoleHost = selectedHost;
        consoleSplitPane = splitPane;
        selectedHost.showConsoleSplit(splitPane);
        installInitialConsoleDivider(splitPane);
    }

    private static JComponent createResizableContentWrapper(Component content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMinimumSize(new Dimension(0, 30));
        if (content != null) {
            wrapper.add(content, BorderLayout.CENTER);
            wrapper.setSize(content.getSize());
        }
        return wrapper;
    }

    static JSplitPane createConsoleSplitPane(Component content, JPanel consoleContainer) {
        return AppToolWindowChrome.createVerticalStackedCardSplitPane(
                content,
                consoleContainer,
                initialConsoleDividerLocation(content)
        );
    }

    private static int initialConsoleDividerLocation(Component content) {
        int contentHeight = content != null ? content.getHeight() : 0;
        if (contentHeight <= MIN_CONSOLE_HEIGHT + CONSOLE_DIVIDER_SIZE) {
            return Integer.MAX_VALUE / 4;
        }
        return defaultConsoleDividerLocation(contentHeight, CONSOLE_DIVIDER_SIZE);
    }

    private void renderHiddenConsole() {
        restoreContentHost();
        consoleContainer.removeAll();
        consoleContainer.setPreferredSize(null);
        consoleContainer.setMinimumSize(null);
        renderBottomBar();

        owner.add(tabbedPane, BorderLayout.CENTER);
        renderSideAssistant();
        owner.add(bottomBarContainer, BorderLayout.SOUTH);
    }

    private void renderSideAssistant() {
        if (sideAssistant != null) {
            owner.add(sideAssistant, BorderLayout.EAST);
        }
    }

    private void renderBottomBar() {
        bottomBarContainer.removeAll();
        bottomBarContainer.add(bottomBar.leftPanel(), BorderLayout.WEST);
        bottomBarContainer.add(bottomBar.rightPanel(), BorderLayout.EAST);
    }

    void restoreContentHost() {
        if (consoleHost != null) {
            consoleHost.showContentOnly();
        }
        consoleHost = null;
        consoleSplitPane = null;
    }

    private void installInitialConsoleDivider(JSplitPane splitPane) {
        ComponentAdapter listener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (applyInitialConsoleDivider(splitPane)) {
                    splitPane.removeComponentListener(this);
                }
            }
        };
        splitPane.addComponentListener(listener);
        SwingUtilities.invokeLater(() -> {
            if (applyInitialConsoleDivider(splitPane)) {
                splitPane.removeComponentListener(listener);
            }
        });
    }

    private boolean applyInitialConsoleDivider(JSplitPane splitPane) {
        int splitHeight = splitPane.getHeight();
        int readyHeight = MIN_CONSOLE_HEIGHT + Math.max(0, splitPane.getDividerSize());
        if (splitHeight <= readyHeight || !consoleVisible || splitPane.getParent() == null) {
            return false;
        }
        splitPane.setDividerLocation(defaultConsoleDividerLocation(
                splitHeight,
                splitPane.getDividerSize()
        ));
        return true;
    }

    static int defaultConsoleDividerLocation(int splitHeight, int dividerSize) {
        int consoleHeight = defaultConsoleHeight(splitHeight);
        return Math.max(0, splitHeight - consoleHeight - Math.max(0, dividerSize));
    }

    private static int defaultConsoleHeight(int splitHeight) {
        if (splitHeight <= 0) {
            return DEFAULT_CONSOLE_HEIGHT;
        }
        int maxReasonableHeight = Math.max(MIN_CONSOLE_HEIGHT, splitHeight / 2);
        return Math.min(DEFAULT_CONSOLE_HEIGHT, maxReasonableHeight);
    }
}
