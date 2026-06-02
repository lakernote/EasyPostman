package com.laker.postman.panel.sidebar;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.constants.ModernColors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Manages the sidebar console area and its collapsed bottom bar state.
 */
final class SidebarConsoleArea {
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
        consoleContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getDividerBorderColor()));
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

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, consoleContainer);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(1.0);
        splitPane.setMinimumSize(new Dimension(0, 10));
        tabbedPane.setMinimumSize(new Dimension(0, 30));
        consoleContainer.setMinimumSize(new Dimension(0, 30));

        owner.add(splitPane, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(splitPane.getHeight() - 300));
    }

    private void showCollapsedBottomBar() {
        consoleContainer.removeAll();
        consoleContainer.add(bottomBar.leftPanel(), BorderLayout.WEST);
        consoleContainer.add(bottomBar.rightPanel(), BorderLayout.EAST);

        owner.add(tabbedPane, BorderLayout.CENTER);
        owner.add(consoleContainer, BorderLayout.SOUTH);
    }
}
