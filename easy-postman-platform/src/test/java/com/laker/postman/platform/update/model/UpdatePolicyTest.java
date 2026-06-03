package com.laker.postman.platform.update.model;

import org.testng.annotations.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class UpdatePolicyTest {

    @Test
    public void disabledPolicyShouldNeverScheduleChecks() {
        UpdatePolicy policy = new UpdatePolicy(UpdateTarget.APP, false, UpdateCheckFrequency.STARTUP);
        UpdateCheckState state = UpdateCheckState.of(UpdateTarget.APP, 0L, Set.of());

        assertFalse(policy.shouldCheck(state, System.currentTimeMillis()));
    }

    @Test
    public void startupFrequencyShouldScheduleEveryTimeWhenEnabled() {
        long now = 1_000_000L;
        UpdatePolicy policy = new UpdatePolicy(UpdateTarget.APP, true, UpdateCheckFrequency.STARTUP);
        UpdateCheckState state = UpdateCheckState.of(UpdateTarget.APP, now, Set.of());

        assertTrue(policy.shouldCheck(state, now));
    }

    @Test
    public void neverCheckedTargetShouldScheduleWhenEnabled() {
        UpdatePolicy policy = new UpdatePolicy(UpdateTarget.PLUGIN, true, UpdateCheckFrequency.MONTHLY);
        UpdateCheckState state = UpdateCheckState.of(UpdateTarget.PLUGIN, 0L, Set.of());

        assertTrue(policy.shouldCheck(state, System.currentTimeMillis()));
    }

    @Test
    public void dailyFrequencyShouldWaitUntilEnoughDaysPassed() {
        long now = TimeUnit.DAYS.toMillis(10);
        UpdatePolicy policy = new UpdatePolicy(UpdateTarget.PLUGIN, true, UpdateCheckFrequency.DAILY);
        UpdateCheckState recent = UpdateCheckState.of(UpdateTarget.PLUGIN, now - TimeUnit.HOURS.toMillis(23), Set.of());
        UpdateCheckState due = UpdateCheckState.of(UpdateTarget.PLUGIN, now - TimeUnit.DAYS.toMillis(1), Set.of());

        assertFalse(policy.shouldCheck(recent, now));
        assertTrue(policy.shouldCheck(due, now));
    }

    @Test
    public void notifiedMarkersShouldBeNullSafeAndImmutable() {
        UpdateCheckState state = UpdateCheckState.of(UpdateTarget.PLUGIN, 123L, null);

        assertTrue(state.notifiedMarkers().isEmpty());
        assertFalse(state.wasNotified("plugin@1.0.0"));
    }
}
