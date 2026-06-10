package com.laker.postman.panel.sidebar;

import com.laker.postman.common.component.AppToolWindowChrome;
import org.testng.annotations.Test;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import static org.testng.Assert.assertEquals;

public class SidebarConsoleAreaTest {

    @Test
    public void initialExpandedDividerShouldReserveDefaultConsoleHeight() {
        int splitHeight = 1000;

        int dividerLocation = SidebarConsoleArea.defaultExpandedConsoleDividerLocation(
                splitHeight,
                AppToolWindowChrome.DIVIDER_SIZE
        );

        assertEquals(dividerLocation,
                splitHeight - SidebarConsoleArea.DEFAULT_EXPANDED_CONSOLE_HEIGHT
                        - AppToolWindowChrome.DIVIDER_SIZE);
    }

    @Test
    public void initialExpandedDividerShouldStayUsableInShortWindows() {
        int dividerLocation = SidebarConsoleArea.defaultExpandedConsoleDividerLocation(
                320,
                AppToolWindowChrome.DIVIDER_SIZE
        );

        assertEquals(dividerLocation, 155);
    }

    @Test
    public void expandedConsoleSplitShouldUseStackedDragGapDivider() {
        JSplitPane splitPane = SidebarConsoleArea.createExpandedConsoleSplitPane(new JTabbedPane(), new JPanel());

        assertEquals(splitPane.getDividerSize(), SidebarConsoleArea.EXPANDED_CONSOLE_DIVIDER_SIZE);
    }
}
