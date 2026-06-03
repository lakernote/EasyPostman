package com.laker.postman.platform.update;

import com.laker.postman.platform.update.model.UpdateCheckState;
import com.laker.postman.platform.update.model.UpdatePolicy;
import com.laker.postman.platform.update.model.UpdateTarget;

/**
 * Storage boundary for per-target update policy and check state.
 */
public interface UpdateStateStore {

    UpdatePolicy policy(UpdateTarget target);

    UpdateCheckState state(UpdateTarget target);

    void recordCheck(UpdateTarget target, long timestampMillis);

    void rememberNotifiedMarker(UpdateTarget target, String marker);
}
