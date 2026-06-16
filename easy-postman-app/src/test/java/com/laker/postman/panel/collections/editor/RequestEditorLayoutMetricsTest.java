package com.laker.postman.panel.collections.editor;

import com.laker.postman.common.UiSingletonPanel;
import org.testng.annotations.Test;

import java.awt.Insets;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_HAS_FULL_BORDER;
import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_SHOW_CONTENT_SEPARATOR;
import static org.testng.Assert.assertEquals;

public class RequestEditorLayoutMetricsTest {

    @Test
    public void requestEditorWorkspaceShouldKeepOneConsistentCardInset() {
        assertEquals(RequestEditorPanel.EDITOR_WORKSPACE_INSETS, new Insets(6, 6, 6, 6));
    }

    @Test
    public void requestEditorTabsShouldUseCompactIdeaLikeHeight() {
        assertEquals(RequestEditorPanel.REQUEST_TAB_HEIGHT, 34);
        assertEquals(RequestEditorPanel.REQUEST_TAB_INSETS, new Insets(2, 5, 2, 5));
        assertEquals(RequestEditorPanel.REQUEST_TAB_AREA_INSETS, new Insets(0, 0, 0, 5));
    }

    @Test
    public void requestEditorTabsShouldUseSingleContentSeparatorWithoutFullBorder() {
        UiSingletonPanel.setFactoryCreationAllowed(true);
        try {
            RequestEditorPanel panel = new RequestEditorPanel();
            panel.initUI();

            assertEquals(panel.getTabbedPane().getClientProperty(TABBED_PANE_HAS_FULL_BORDER), Boolean.FALSE);
            assertEquals(panel.getTabbedPane().getClientProperty(TABBED_PANE_SHOW_CONTENT_SEPARATOR), Boolean.TRUE);
        } finally {
            UiSingletonPanel.setFactoryCreationAllowed(false);
        }
    }

    @Test
    public void emptyStateShouldKeepCompactVerticalInsetInsideEditorCard() {
        assertEquals(RequestEditorEmptyStatePanel.EMPTY_STATE_INSETS, new Insets(12, 20, 12, 20));
    }
}
