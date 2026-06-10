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
    public void backgroundNotificationShouldSkipIgnoredMarker() throws Exception {
        MemoryUpdateStateStore store = new MemoryUpdateStateStore();
        UpdateCenter updateCenter = new UpdateCenter(store);
        RecordingUpdateUiController uiController = new RecordingUpdateUiController();
        AppUpdateCheckCoordinator coordinator = new AppUpdateCheckCoordinator(uiController, updateCenter);
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);
        store.rememberIgnoredMarker(UpdateTarget.APP, "app@v1.2.0@UPDATE_AVAILABLE");

        coordinator.handleUpdateCheckResult(updateInfo, false);
        flushEdt();

        assertEquals(uiController.notificationRequests, 0);
    }

    @Test
    public void backgroundNotificationShouldShowWhenMarkerIsNotIgnored() throws Exception {
        MemoryUpdateStateStore store = new MemoryUpdateStateStore();
        UpdateCenter updateCenter = new UpdateCenter(store);
        RecordingUpdateUiController uiController = new RecordingUpdateUiController();
        AppUpdateCheckCoordinator coordinator = new AppUpdateCheckCoordinator(uiController, updateCenter);
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);

        coordinator.handleUpdateCheckResult(updateInfo, false);
        flushEdt();

        assertEquals(uiController.notificationRequests, 1);
    }

    @Test
    public void manualNotificationShouldIgnoreIgnoredMarker() throws Exception {
        MemoryUpdateStateStore store = new MemoryUpdateStateStore();
        UpdateCenter updateCenter = new UpdateCenter(store);
        RecordingUpdateUiController uiController = new RecordingUpdateUiController();
        AppUpdateCheckCoordinator coordinator = new AppUpdateCheckCoordinator(uiController, updateCenter);
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);
        store.rememberIgnoredMarker(UpdateTarget.APP, "app@v1.2.0@UPDATE_AVAILABLE");

        coordinator.handleUpdateCheckResult(updateInfo, true);
        flushEdt();

        assertEquals(uiController.updateDialogRequests, 1);
    }

    @Test
    public void updateDialogIgnoreActionShouldRememberIgnoredMarker() throws Exception {
        MemoryUpdateStateStore store = new MemoryUpdateStateStore();
        UpdateCenter updateCenter = new UpdateCenter(store);
        RecordingUpdateUiController uiController = new RecordingUpdateUiController();
        AppUpdateCheckCoordinator coordinator = new AppUpdateCheckCoordinator(uiController, updateCenter);
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);

        coordinator.handleUpdateCheckResult(updateInfo, true);
        flushEdt();
        uiController.runUpdateDialogIgnoreCallback();

        assertTrue(updateCenter.ignoredMarkers(UpdateTarget.APP).contains("app@v1.2.0@UPDATE_AVAILABLE"));
    }

    @Test
    public void nonUpdateStatusShouldNotCreateNotificationMarker() {
        assertEquals(AppUpdateCheckCoordinator.notificationMarker(UpdateInfo.noUpdateAvailable("ok")), "");
        assertEquals(AppUpdateCheckCoordinator.notificationMarker(UpdateInfo.checkFailed("bad")), "");
    }

    @Test
    public void nonUpdateStatusShouldStillPassNotificationGate() {
        assertTrue(AppUpdateCheckCoordinator.shouldNotify(UpdateInfo.noUpdateAvailable("ok"), Set.of(), true));
        assertTrue(AppUpdateCheckCoordinator.shouldNotify(UpdateInfo.checkFailed("bad"), Set.of(), true));
    }

    @Test
    public void nullUpdateInfoShouldNotNotify() {
        assertFalse(AppUpdateCheckCoordinator.shouldNotify(null, Set.of(), false));
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
        });
    }

    private static final class RecordingUpdateUiController extends UpdateUiController {
        private int notificationRequests;
        private int noAssetNotificationRequests;
        private int updateDialogRequests;
        private Runnable updateDialogIgnoreCallback;

        @Override
        public void showUpdateNotification(UpdateInfo updateInfo, Runnable onIgnoreVersion) {
            notificationRequests++;
        }

        @Override
        public void showNoAssetNotification(UpdateInfo updateInfo) {
            noAssetNotificationRequests++;
        }

        @Override
        public void showUpdateDialog(UpdateInfo updateInfo, Runnable onIgnoreVersion) {
            updateDialogRequests++;
            updateDialogIgnoreCallback = onIgnoreVersion;
        }

        private void runUpdateDialogIgnoreCallback() {
            if (updateDialogIgnoreCallback != null) {
                updateDialogIgnoreCallback.run();
            }
        }
    }

    private static final class MemoryUpdateStateStore implements UpdateStateStore {
        private final Map<UpdateTarget, UpdatePolicy> policies = new EnumMap<>(UpdateTarget.class);
        private final Map<UpdateTarget, UpdateCheckState> states = new EnumMap<>(UpdateTarget.class);
        private final Map<UpdateTarget, Set<String>> ignoredMarkers = new EnumMap<>(UpdateTarget.class);

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

        @Override
        public Set<String> ignoredMarkers(UpdateTarget target) {
            return ignoredMarkers.getOrDefault(target, Set.of());
        }

        @Override
        public void rememberIgnoredMarker(UpdateTarget target, String marker) {
            if (marker == null || marker.isBlank()) {
                return;
            }
            Set<String> markers = new LinkedHashSet<>(ignoredMarkers(target));
            markers.add(marker.trim());
            ignoredMarkers.put(target, markers);
        }
    }
}
