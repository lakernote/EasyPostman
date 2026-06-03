package com.laker.postman.platform.update;

import com.laker.postman.platform.update.model.UpdateCheckFrequency;
import com.laker.postman.platform.update.model.UpdateCheckState;
import com.laker.postman.platform.update.model.UpdatePolicy;
import com.laker.postman.platform.update.model.UpdateTarget;
import org.testng.annotations.Test;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class UpdateCenterTest {

    @Test
    public void shouldUseTargetPolicyAndStateForScheduling() {
        MemoryUpdateStateStore store = new MemoryUpdateStateStore();
        store.policies.put(UpdateTarget.APP, new UpdatePolicy(UpdateTarget.APP, true, UpdateCheckFrequency.DAILY));
        store.states.put(UpdateTarget.APP, UpdateCheckState.of(UpdateTarget.APP, 0L, Set.of()));

        UpdateCenter updateCenter = new UpdateCenter(store);

        assertEquals(updateCenter.policy(UpdateTarget.APP).target(), UpdateTarget.APP);
        assertTrue(updateCenter.shouldCheck(UpdateTarget.APP, 1_000L));
    }

    @Test
    public void shouldRecordCheckTimeAndNotifiedMarkersThroughStore() {
        MemoryUpdateStateStore store = new MemoryUpdateStateStore();
        UpdateCenter updateCenter = new UpdateCenter(store);

        updateCenter.recordCheck(UpdateTarget.PLUGIN, 12_345L);
        updateCenter.rememberNotifiedMarker(UpdateTarget.PLUGIN, "plugin-a@1.2.0");
        updateCenter.rememberNotifiedMarker(UpdateTarget.PLUGIN, " ");

        UpdateCheckState state = updateCenter.state(UpdateTarget.PLUGIN);
        assertEquals(state.target(), UpdateTarget.PLUGIN);
        assertEquals(state.lastCheckTimeMillis(), 12_345L);
        assertTrue(state.wasNotified("plugin-a@1.2.0"));
        assertFalse(state.wasNotified(" "));
    }

    @Test
    public void shouldSkipKnownMarkersOnlyForBackgroundNotifications() {
        UpdateCheckState state = UpdateCheckState.of(
                UpdateTarget.APP,
                100L,
                Set.of("app@1.2.0@UPDATE_AVAILABLE")
        );

        assertFalse(UpdateCenter.shouldNotify("app@1.2.0@UPDATE_AVAILABLE", state, false));
        assertTrue(UpdateCenter.shouldNotify("app@1.2.0@UPDATE_AVAILABLE", state, true));
        assertTrue(UpdateCenter.shouldNotify("app@1.3.0@UPDATE_AVAILABLE", state, false));
        assertFalse(UpdateCenter.shouldNotify("", state, false));
    }

    private static final class MemoryUpdateStateStore implements UpdateStateStore {
        private final Map<UpdateTarget, UpdatePolicy> policies = new EnumMap<>(UpdateTarget.class);
        private final Map<UpdateTarget, UpdateCheckState> states = new EnumMap<>(UpdateTarget.class);

        @Override
        public UpdatePolicy policy(UpdateTarget target) {
            return policies.getOrDefault(target, new UpdatePolicy(target, true, UpdateCheckFrequency.DAILY));
        }

        @Override
        public UpdateCheckState state(UpdateTarget target) {
            return states.getOrDefault(target, UpdateCheckState.of(target, 0L, Set.of()));
        }

        @Override
        public void recordCheck(UpdateTarget target, long timestampMillis) {
            UpdateCheckState current = state(target);
            states.put(target, UpdateCheckState.of(target, timestampMillis, current.notifiedMarkers()));
        }

        @Override
        public void rememberNotifiedMarker(UpdateTarget target, String marker) {
            if (marker == null || marker.isBlank()) {
                return;
            }
            UpdateCheckState current = state(target);
            Set<String> markers = new LinkedHashSet<>(current.notifiedMarkers());
            markers.add(marker.trim());
            states.put(target, UpdateCheckState.of(target, current.lastCheckTimeMillis(), markers));
        }
    }
}
