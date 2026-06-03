package com.laker.postman.platform.update;

import com.laker.postman.platform.update.model.UpdateCheckFrequency;
import com.laker.postman.platform.update.model.UpdateCheckState;
import com.laker.postman.platform.update.model.UpdatePolicy;
import com.laker.postman.platform.update.model.UpdateTarget;

import java.util.Objects;
import java.util.Set;

/**
 * UI-neutral coordinator for update scheduling state.
 */
public final class UpdateCenter {

    private final UpdateStateStore stateStore;

    public UpdateCenter(UpdateStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }

    public UpdatePolicy policy(UpdateTarget target) {
        UpdateTarget normalizedTarget = normalizeTarget(target);
        UpdatePolicy policy = stateStore.policy(normalizedTarget);
        return policy == null
                ? new UpdatePolicy(normalizedTarget, false, UpdateCheckFrequency.DAILY)
                : policy;
    }

    public UpdateCheckState state(UpdateTarget target) {
        UpdateTarget normalizedTarget = normalizeTarget(target);
        UpdateCheckState state = stateStore.state(normalizedTarget);
        return state == null ? UpdateCheckState.of(normalizedTarget, 0L, Set.of()) : state;
    }

    public boolean shouldCheck(UpdateTarget target, long currentTimeMillis) {
        UpdateTarget normalizedTarget = normalizeTarget(target);
        return policy(normalizedTarget).shouldCheck(state(normalizedTarget), currentTimeMillis);
    }

    public void recordCheck(UpdateTarget target, long timestampMillis) {
        stateStore.recordCheck(normalizeTarget(target), Math.max(0L, timestampMillis));
    }

    public void rememberNotifiedMarker(UpdateTarget target, String marker) {
        if (marker == null || marker.isBlank()) {
            return;
        }
        stateStore.rememberNotifiedMarker(normalizeTarget(target), marker.trim());
    }

    public boolean shouldNotify(UpdateTarget target, String marker, boolean isManual) {
        return shouldNotify(marker, state(target), isManual);
    }

    public static boolean shouldNotify(String marker, UpdateCheckState state, boolean isManual) {
        if (marker == null || marker.isBlank()) {
            return false;
        }
        return isManual || state == null || !state.wasNotified(marker.trim());
    }

    private static UpdateTarget normalizeTarget(UpdateTarget target) {
        return target == null ? UpdateTarget.APP : target;
    }
}
