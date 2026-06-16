package com.laker.postman.panel.sidebar;

import com.laker.postman.common.component.RoundedToolWindowPanel;
import org.testng.annotations.Test;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class SidebarConsoleAreaTest {

    @Test
    public void initialConsoleDividerShouldReserveDefaultConsoleHeight() {
        int splitHeight = 1000;

        int dividerLocation = SidebarConsoleArea.defaultConsoleDividerLocation(
                splitHeight,
                SidebarConsoleArea.CONSOLE_DIVIDER_SIZE
        );

        assertEquals(dividerLocation,
                splitHeight - SidebarConsoleArea.DEFAULT_CONSOLE_HEIGHT
                        - SidebarConsoleArea.CONSOLE_DIVIDER_SIZE);
    }

    @Test
    public void initialConsoleDividerShouldStayUsableInShortWindows() {
        int dividerLocation = SidebarConsoleArea.defaultConsoleDividerLocation(
                320,
                SidebarConsoleArea.CONSOLE_DIVIDER_SIZE
        );

        assertEquals(dividerLocation, 159);
    }

    @Test
    public void consoleSplitShouldUseStackedDragGapDivider() {
        JSplitPane splitPane = SidebarConsoleArea.createConsoleSplitPane(new JTabbedPane(), new JPanel());

        assertEquals(splitPane.getDividerSize(), SidebarConsoleArea.CONSOLE_DIVIDER_SIZE);
    }

    @Test
    public void consoleSplitShouldWrapConsoleInRoundedToolWindow() {
        JPanel console = new JPanel();

        JSplitPane splitPane = SidebarConsoleArea.createConsoleSplitPane(new JTabbedPane(), console);

        assertTrue(containsComponentOfType(splitPane.getBottomComponent(), RoundedToolWindowPanel.class));
    }

    @Test
    public void consoleSplitShouldInitializeDividerNearBottomWhenContentIsShowing() {
        JPanel content = new JPanel();
        content.setSize(800, 1000);
        JPanel console = new JPanel();

        JSplitPane splitPane = SidebarConsoleArea.createConsoleSplitPane(content, console);

        assertEquals(splitPane.getDividerLocation(), 1000
                - SidebarConsoleArea.DEFAULT_CONSOLE_HEIGHT
                - SidebarConsoleArea.CONSOLE_DIVIDER_SIZE);
    }

    @Test
    public void visibleConsoleShouldStayInsideSelectedTabHost() {
        JPanel owner = new JPanel(new BorderLayout());
        SidebarConsoleArea consoleArea = new SidebarConsoleArea(owner, noopBottomBar(), new JPanel());
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        JPanel selectedContent = new JPanel();
        SidebarTabContentHost selectedHost = new SidebarTabContentHost(selectedContent);
        tabbedPane.addTab("Requests", selectedHost);

        consoleArea.setTabbedPane(tabbedPane);
        consoleArea.showConsole();

        Component center = ((BorderLayout) owner.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        assertSame(center, tabbedPane);
        assertTrue(((BorderLayout) owner.getLayout()).getLayoutComponent(BorderLayout.SOUTH) instanceof JPanel);
        assertSame(tabbedPane.getComponentAt(0), selectedHost);
        assertTrue(selectedHost.isConsoleVisible());

        JSplitPane selectedSplit = findFirstComponent(selectedHost, JSplitPane.class);
        assertTrue(selectedSplit != null);
        assertTrue(containsComponent(selectedSplit.getTopComponent(), selectedContent));
    }

    @Test
    public void visibleConsoleShouldKeepBottomToolbarAtWindowBottom() {
        JPanel owner = new JPanel(new BorderLayout());
        SidebarConsoleArea consoleArea = new SidebarConsoleArea(owner, noopBottomBar(), new JPanel());
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.addTab("Requests", new SidebarTabContentHost(new JPanel()));

        consoleArea.setTabbedPane(tabbedPane);
        consoleArea.showConsole();

        Component bottom = ((BorderLayout) owner.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        assertTrue(bottom instanceof JPanel);
        assertEquals(((JPanel) bottom).getComponentCount(), 2);
    }

    @Test
    public void visibleConsoleShouldMoveWhenSelectedTabChanges() {
        JPanel owner = new JPanel(new BorderLayout());
        SidebarConsoleArea consoleArea = new SidebarConsoleArea(
                owner,
                noopBottomBar(),
                new JPanel()
        );
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        JPanel firstContent = new JPanel();
        JPanel secondContent = new JPanel();
        SidebarTabContentHost firstHost = new SidebarTabContentHost(firstContent);
        SidebarTabContentHost secondHost = new SidebarTabContentHost(secondContent);
        tabbedPane.addTab("Requests", firstHost);
        tabbedPane.addTab("Environments", secondHost);

        consoleArea.setTabbedPane(tabbedPane);
        consoleArea.showConsole();
        tabbedPane.setSelectedIndex(1);
        consoleArea.handleSelectedTabChanged();

        assertSame(tabbedPane.getComponentAt(0), firstHost);
        assertSame(tabbedPane.getComponentAt(1), secondHost);
        assertSame(firstHost.content(), firstContent);
        assertSame(secondHost.content(), secondContent);
        assertFalse(firstHost.isConsoleVisible());
        assertTrue(secondHost.isConsoleVisible());

        JSplitPane selectedSplit = findFirstComponent(secondHost, JSplitPane.class);
        assertTrue(selectedSplit != null);
        assertTrue(containsComponent(selectedSplit.getTopComponent(), secondContent));
    }

    @Test
    public void toggleConsoleShouldHideVisibleConsole() {
        JPanel owner = new JPanel(new BorderLayout());
        SidebarConsoleArea consoleArea = new SidebarConsoleArea(owner, noopBottomBar(), new JPanel());
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        JPanel selectedContent = new JPanel();
        SidebarTabContentHost selectedHost = new SidebarTabContentHost(selectedContent);
        tabbedPane.addTab("Requests", selectedHost);

        consoleArea.setTabbedPane(tabbedPane);
        consoleArea.toggleConsole();
        consoleArea.toggleConsole();

        assertSame(tabbedPane.getComponentAt(0), selectedHost);
        assertSame(selectedHost.content(), selectedContent);
        assertFalse(selectedHost.isConsoleVisible());
    }

    @Test
    public void visibleConsoleShouldNotOverwriteSelectedContentMinimumSize() {
        JPanel owner = new JPanel(new BorderLayout());
        SidebarConsoleArea consoleArea = new SidebarConsoleArea(owner, noopBottomBar(), new JPanel());
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        JPanel selectedContent = new JPanel();
        Dimension originalMinimumSize = new Dimension(500, 300);
        selectedContent.setMinimumSize(originalMinimumSize);
        SidebarTabContentHost selectedHost = new SidebarTabContentHost(selectedContent);
        tabbedPane.addTab("Requests", selectedHost);

        consoleArea.setTabbedPane(tabbedPane);
        consoleArea.toggleConsole();
        consoleArea.toggleConsole();

        assertEquals(selectedContent.getMinimumSize(), originalMinimumSize);
        assertSame(selectedHost.content(), selectedContent);
        assertFalse(selectedHost.isConsoleVisible());
    }

    @Test
    public void visibleConsoleShouldNotReplaceSidebarTabComponent() {
        JPanel owner = new JPanel(new BorderLayout());
        SidebarConsoleArea consoleArea = new SidebarConsoleArea(owner, noopBottomBar(), new JPanel());
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        SidebarTabContentHost selectedHost = new SidebarTabContentHost(new JPanel());
        tabbedPane.addTab("Requests", selectedHost);

        consoleArea.setTabbedPane(tabbedPane);
        Component before = tabbedPane.getComponentAt(0);
        consoleArea.showConsole();

        assertSame(tabbedPane.getComponentAt(0), before);
    }

    private static boolean containsComponentOfType(Component component, Class<?> type) {
        if (type.isInstance(component)) {
            return true;
        }
        if (!(component instanceof Container container)) {
            return false;
        }
        for (Component child : container.getComponents()) {
            if (containsComponentOfType(child, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsComponent(Component root, Component target) {
        if (root == target) {
            return true;
        }
        if (!(root instanceof Container container)) {
            return false;
        }
        for (Component child : container.getComponents()) {
            if (containsComponent(child, target)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Component> T findFirstComponent(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (!(component instanceof Container container)) {
            return null;
        }
        for (Component child : container.getComponents()) {
            T nested = findFirstComponent(child, type);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static SidebarBottomBar noopBottomBar() {
        return new SidebarBottomBar(
                false,
                () -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                }
        );
    }
}
