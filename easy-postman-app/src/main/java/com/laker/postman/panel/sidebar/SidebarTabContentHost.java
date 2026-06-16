package com.laker.postman.panel.sidebar;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;

/**
 * Stable tab content host for sidebar tabs.
 *
 * <p>The sidebar tabbed pane keeps this host as the tab component while the host swaps only its
 * internal layout between plain content and content+Console. This avoids changing tab component
 * identity when the bottom Console is toggled.</p>
 */
final class SidebarTabContentHost extends JPanel {
    private Component content;
    private boolean consoleVisible;

    SidebarTabContentHost(Component content) {
        super(new BorderLayout());
        setOpaque(false);
        this.content = content;
        showContentOnly();
    }

    static SidebarTabContentHost from(Component component) {
        return component instanceof SidebarTabContentHost host ? host : null;
    }

    static Component contentOf(Component component) {
        SidebarTabContentHost host = from(component);
        return host != null ? host.content() : component;
    }

    Component content() {
        return content;
    }

    void setContent(Component content) {
        if (this.content == content) {
            return;
        }
        this.content = content;
        showContentOnly();
    }

    void showContentOnly() {
        removeAll();
        consoleVisible = false;
        if (content != null) {
            add(content, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    void showConsoleSplit(JSplitPane splitPane) {
        removeAll();
        consoleVisible = true;
        add(splitPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    boolean isConsoleVisible() {
        return consoleVisible;
    }
}
