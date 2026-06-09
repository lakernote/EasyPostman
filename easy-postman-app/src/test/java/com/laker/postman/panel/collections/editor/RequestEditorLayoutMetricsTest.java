package com.laker.postman.panel.collections.editor;

import org.testng.annotations.Test;

import java.awt.Insets;

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
    public void emptyStateShouldKeepCompactVerticalInsetInsideEditorCard() {
        assertEquals(RequestEditorEmptyStatePanel.EMPTY_STATE_INSETS, new Insets(12, 20, 12, 20));
    }
}
