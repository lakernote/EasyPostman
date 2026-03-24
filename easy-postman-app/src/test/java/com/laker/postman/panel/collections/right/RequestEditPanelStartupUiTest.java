package com.laker.postman.panel.collections.right;

import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RequestEditPanelStartupUiTest extends AbstractSwingUiTest {

    @Test
    public void shouldShowStartupPlaceholderBeforeAnyTabIsAdded() throws Exception {
        RequestEditPanel panel = createPanel();

        assertTrue(panel.isShowingStartupPlaceholder());
        assertEquals(panel.getTabbedPane().getTabCount(), 0);
    }

    @Test
    public void shouldSwitchToTabsCardWhenPlusTabIsAdded() throws Exception {
        RequestEditPanel panel = createPanel();

        SwingUtilities.invokeAndWait(panel::addPlusTab);

        assertFalse(panel.isShowingStartupPlaceholder());
        assertEquals(panel.getTabbedPane().getTabCount(), 1);
        assertEquals(panel.getTabbedPane().getTitleAt(0), RequestEditPanel.PLUS_TAB);
    }

    @Test
    public void shouldKeepStartupPlaceholderWhenAutoRevealIsDisabled() throws Exception {
        RequestEditPanel panel = createPanel();

        SwingUtilities.invokeAndWait(() -> {
            panel.setAutoRevealTabsCard(false);
            panel.addPlusTab();
        });

        assertTrue(panel.isShowingStartupPlaceholder());
        assertEquals(panel.getTabbedPane().getTabCount(), 1);
        assertEquals(panel.getTabbedPane().getTitleAt(0), RequestEditPanel.PLUS_TAB);
    }

    @Test
    public void shouldCancelStartupRestoreSelectionWhenUserOpensAnotherTab() throws Exception {
        RequestEditPanel panel = createPanel();

        SwingUtilities.invokeAndWait(() -> {
            panel.beginStartupRestoreSelectionTracking();
            assertTrue(panel.shouldSelectRestoredStartupTab());
            panel.addNewTab("Manual");
        });

        assertFalse(panel.shouldSelectRestoredStartupTab());
    }

    private RequestEditPanel createPanel() throws Exception {
        final RequestEditPanel[] holder = new RequestEditPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            SingletonBasePanel.setCreatingAllowed(true);
            try {
                RequestEditPanel panel = new RequestEditPanel();
                panel.safeInit();
                holder[0] = panel;
            } finally {
                SingletonBasePanel.setCreatingAllowed(false);
            }
        });
        return holder[0];
    }
}
