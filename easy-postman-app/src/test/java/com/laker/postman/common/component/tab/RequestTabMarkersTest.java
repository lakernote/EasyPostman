package com.laker.postman.common.component.tab;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RequestTabMarkersTest {

    @Test
    public void newRequestMarkerShouldTakePrecedenceOverDirtyMarker() {
        RequestTabMarkers markers = RequestTabMarkers.clean().withNewRequest(true).withDirty(true);

        assertTrue(markers.isNewRequest());
        assertFalse(markers.isDirty());
    }

    @Test
    public void savedShouldClearSaveMarkersAndKeepPreviewMode() {
        RequestTabMarkers markers = RequestTabMarkers.clean()
                .withNewRequest(true)
                .withPreviewMode(true)
                .saved();

        assertFalse(markers.isNewRequest());
        assertFalse(markers.isDirty());
        assertTrue(markers.isPreviewMode());
    }

    @Test
    public void pinnedShouldOnlyClearPreviewMode() {
        RequestTabMarkers markers = RequestTabMarkers.clean()
                .withDirty(true)
                .withPreviewMode(true)
                .pinned();

        assertTrue(markers.isDirty());
        assertFalse(markers.isNewRequest());
        assertFalse(markers.isPreviewMode());
    }
}
