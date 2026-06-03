package com.laker.postman.platform.update.model;

import java.util.concurrent.TimeUnit;

public record UpdatePolicy(
        UpdateTarget target,
        boolean enabled,
        UpdateCheckFrequency frequency
) {

    public UpdatePolicy {
        if (target == null) {
            target = UpdateTarget.APP;
        }
        if (frequency == null) {
            frequency = UpdateCheckFrequency.DAILY;
        }
    }

    public boolean shouldCheck(UpdateCheckState state, long currentTimeMillis) {
        if (!enabled) {
            return false;
        }
        if (frequency.isAlwaysCheck()) {
            return true;
        }
        long lastCheckTimeMillis = state == null ? 0L : state.lastCheckTimeMillis();
        if (lastCheckTimeMillis <= 0L) {
            return true;
        }
        long elapsedMillis = Math.max(0L, currentTimeMillis - lastCheckTimeMillis);
        long daysSinceLastCheck = TimeUnit.MILLISECONDS.toDays(elapsedMillis);
        return daysSinceLastCheck >= frequency.getDays();
    }
}
