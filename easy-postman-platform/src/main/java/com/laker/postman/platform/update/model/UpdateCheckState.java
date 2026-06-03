package com.laker.postman.platform.update.model;

import java.util.Collection;
import java.util.Set;

public record UpdateCheckState(
        UpdateTarget target,
        long lastCheckTimeMillis,
        Set<String> notifiedMarkers
) {

    public UpdateCheckState {
        if (target == null) {
            target = UpdateTarget.APP;
        }
        lastCheckTimeMillis = Math.max(0L, lastCheckTimeMillis);
        notifiedMarkers = notifiedMarkers == null ? Set.of() : Set.copyOf(notifiedMarkers);
    }

    public static UpdateCheckState of(UpdateTarget target,
                                      long lastCheckTimeMillis,
                                      Collection<String> notifiedMarkers) {
        Set<String> markers = notifiedMarkers == null ? Set.of() : Set.copyOf(notifiedMarkers);
        return new UpdateCheckState(target, lastCheckTimeMillis, markers);
    }

    public boolean wasNotified(String marker) {
        return marker != null && notifiedMarkers.contains(marker);
    }
}
