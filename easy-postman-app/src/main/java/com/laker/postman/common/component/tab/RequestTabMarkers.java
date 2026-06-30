package com.laker.postman.common.component.tab;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Immutable marker state for a request editor tab.
 * <p>
 * New-request and dirty are mutually exclusive save markers; preview is an
 * independent visual mode used for transient tabs.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestTabMarkers {
    private static final RequestTabMarkers CLEAN = new RequestTabMarkers(SaveMarker.NONE, false);

    private final SaveMarker saveMarker;
    private final boolean previewMode;

    static RequestTabMarkers clean() {
        return CLEAN;
    }

    public RequestTabMarkers withDirty(boolean dirty) {
        if (saveMarker == SaveMarker.NEW_REQUEST) {
            return this;
        }
        return new RequestTabMarkers(dirty ? SaveMarker.DIRTY : SaveMarker.NONE, previewMode);
    }

    public RequestTabMarkers withNewRequest(boolean newRequest) {
        return new RequestTabMarkers(newRequest ? SaveMarker.NEW_REQUEST : SaveMarker.NONE, previewMode);
    }

    public RequestTabMarkers withPreviewMode(boolean previewMode) {
        return new RequestTabMarkers(saveMarker, previewMode);
    }

    public RequestTabMarkers saved() {
        return new RequestTabMarkers(SaveMarker.NONE, previewMode);
    }

    public RequestTabMarkers pinned() {
        return new RequestTabMarkers(saveMarker, false);
    }

    public boolean isDirty() {
        return saveMarker == SaveMarker.DIRTY;
    }

    public boolean isNewRequest() {
        return saveMarker == SaveMarker.NEW_REQUEST;
    }

    public boolean isPreviewMode() {
        return previewMode;
    }

    private enum SaveMarker {
        NONE,
        NEW_REQUEST,
        DIRTY
    }
}
