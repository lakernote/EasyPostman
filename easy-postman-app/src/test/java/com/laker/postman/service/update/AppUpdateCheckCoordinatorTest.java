package com.laker.postman.service.update;

import com.laker.postman.platform.update.UpdateCenter;
import com.laker.postman.platform.update.UpdateStateStore;
import com.laker.postman.platform.update.model.UpdateCheckFrequency;
import com.laker.postman.platform.update.model.UpdateCheckState;
import com.laker.postman.platform.update.model.UpdateInfo;
import com.laker.postman.platform.update.model.UpdatePolicy;
import com.laker.postman.platform.update.model.UpdateTarget;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AppUpdateCheckCoordinatorTest {

    @Test
    public void notificationMarkerShouldIncludeAppVersionAndAvailabilityStatus() {
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);
        UpdateInfo noAssetInfo = UpdateInfo.updateAvailableNoAsset("1.0.0", "v1.2.0", null);

        assertEquals(
                AppUpdateCheckCoordinator.notificationMarker(updateInfo),
                "app@v1.2.0@UPDATE_AVAILABLE"
        );
        assertEquals(
                AppUpdateCheckCoordinator.notificationMarker(noAssetInfo),
                "app@v1.2.0@UPDATE_AVAILABLE_NO_ASSET"
        );
    }

    @Test
    public void backgroundNotificationShouldSkipAlreadyNotifiedMarker() {
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);
        UpdateCheckState state = UpdateCheckState.of(
                UpdateTarget.APP,
                123L,
                Set.of("app@v1.2.0@UPDATE_AVAILABLE")
        );

        assertFalse(AppUpdateCheckCoordinator.shouldNotify(updateInfo, state, false));
    }

    @Test
    public void manualNotificationShouldIgnoreAlreadyNotifiedMarker() {
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);
        UpdateCheckState state = UpdateCheckState.of(
                UpdateTarget.APP,
                123L,
                Set.of("app@v1.2.0@UPDATE_AVAILABLE")
        );

        assertTrue(AppUpdateCheckCoordinator.shouldNotify(updateInfo, state, true));
    }

    @Test
    public void nonUpdateStatusShouldNotCreateNotificationMarker() {
        assertEquals(AppUpdateCheckCoordinator.notificationMarker(UpdateInfo.noUpdateAvailable("ok")), "");
        assertEquals(AppUpdateCheckCoordinator.notificationMarker(UpdateInfo.checkFailed("bad")), "");
    }

    @Test
    public void nonUpdateStatusShouldStillPassNotificationGate() {
        UpdateCheckState state = UpdateCheckState.of(
                UpdateTarget.APP,
                123L,
                Set.of("app@v1.2.0@UPDATE_AVAILABLE")
        );

        assertTrue(AppUpdateCheckCoordinator.shouldNotify(UpdateInfo.noUpdateAvailable("ok"), state, true));
        assertTrue(AppUpdateCheckCoordinator.shouldNotify(UpdateInfo.checkFailed("bad"), state, true));
    }

    @Test
    public void nullUpdateInfoShouldNotNotify() {
        assertFalse(AppUpdateCheckCoordinator.shouldNotify(null, UpdateCheckState.of(UpdateTarget.APP, 0L, Set.of()), false));
    }

    @Test
    public void backgroundNotificationShouldRememberMarkerOnlyAfterNotificationIsShown() throws Exception {
        MemoryUpdateStateStore store = new MemoryUpdateStateStore();
        UpdateCenter updateCenter = new UpdateCenter(store);
        RecordingUpdateUiController uiController = new RecordingUpdateUiController();
        AppUpdateCheckCoordinator coordinator = new AppUpdateCheckCoordinator(uiController, updateCenter);
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);

        coordinator.handleUpdateCheckResult(updateInfo, false);
        flushEdt();

        assertEquals(uiController.notificationRequests, 1);
        assertFalse(updateCenter.state(UpdateTarget.APP).wasNotified("app@v1.2.0@UPDATE_AVAILABLE"));

        uiController.runNotificationShownCallback();

        assertTrue(updateCenter.state(UpdateTarget.APP).wasNotified("app@v1.2.0@UPDATE_AVAILABLE"));
    }

    @Test
    public void backgroundNoAssetNotificationShouldRememberMarkerOnlyAfterNotificationIsShown() throws Exception {
        MemoryUpdateStateStore store = new MemoryUpdateStateStore();
        UpdateCenter updateCenter = new UpdateCenter(store);
        RecordingUpdateUiController uiController = new RecordingUpdateUiController();
        AppUpdateCheckCoordinator coordinator = new AppUpdateCheckCoordinator(uiController, updateCenter);
        UpdateInfo updateInfo = UpdateInfo.updateAvailableNoAsset("1.0.0", "v1.2.0", null);

        coordinator.handleUpdateCheckResult(updateInfo, false);
        flushEdt();

        assertEquals(uiController.noAssetNotificationRequests, 1);
        assertFalse(updateCenter.state(UpdateTarget.APP).wasNotified("app@v1.2.0@UPDATE_AVAILABLE_NO_ASSET"));

        uiController.runNoAssetNotificationShownCallback();

        assertTrue(updateCenter.state(UpdateTarget.APP).wasNotified("app@v1.2.0@UPDATE_AVAILABLE_NO_ASSET"));
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    private static final class RecordingUpdateUiController extends UpdateUiController {
        private int notificationRequests;
        private int noAssetNotificationRequests;
        private Runnable notificationShownCallback;
        private Runnable noAssetNotificationShownCallback;

        @Override
        public void showUpdateNotification(UpdateInfo updateInfo, Runnable onShown) {
            notificationRequests++;
            notificationShownCallback = onShown;
        }

        @Override
        public void showNoAssetNotification(UpdateInfo updateInfo, Runnable onShown) {
            noAssetNotificationRequests++;
            noAssetNotificationShownCallback = onShown;
        }

        private void runNotificationShownCallback() {
            if (notificationShownCallback != null) {
                notificationShownCallback.run();
            }
        }

        private void runNoAssetNotificationShownCallback() {
            if (noAssetNotificationShownCallback != null) {
                noAssetNotificationShownCallback.run();
            }
        }
    }

    private static final class MemoryUpdateStateStore implements UpdateStateStore {
        private final Map<UpdateTarget, UpdatePolicy> policies = new EnumMap<>(UpdateTarget.class);
        private final Map<UpdateTarget, UpdateCheckState> states = new EnumMap<>(UpdateTarget.class);

        @Override
        public UpdatePolicy policy(UpdateTarget target) {
            return policies.getOrDefault(target, new UpdatePolicy(target, true, UpdateCheckFrequency.STARTUP));
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
