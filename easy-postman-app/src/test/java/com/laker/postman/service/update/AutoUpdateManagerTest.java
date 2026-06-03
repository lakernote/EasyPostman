package com.laker.postman.service.update;

import com.laker.postman.platform.update.model.UpdateCheckState;
import com.laker.postman.platform.update.model.UpdateInfo;
import com.laker.postman.platform.update.model.UpdateTarget;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AutoUpdateManagerTest {

    @Test
    public void notificationMarkerShouldIncludeAppVersionAndAvailabilityStatus() {
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);
        UpdateInfo noAssetInfo = UpdateInfo.updateAvailableNoAsset("1.0.0", "v1.2.0", null);

        assertEquals(
                AutoUpdateManager.notificationMarker(updateInfo),
                "app@v1.2.0@UPDATE_AVAILABLE"
        );
        assertEquals(
                AutoUpdateManager.notificationMarker(noAssetInfo),
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

        assertFalse(AutoUpdateManager.shouldNotify(updateInfo, state, false));
    }

    @Test
    public void manualNotificationShouldIgnoreAlreadyNotifiedMarker() {
        UpdateInfo updateInfo = UpdateInfo.updateAvailable("1.0.0", "v1.2.0", null);
        UpdateCheckState state = UpdateCheckState.of(
                UpdateTarget.APP,
                123L,
                Set.of("app@v1.2.0@UPDATE_AVAILABLE")
        );

        assertTrue(AutoUpdateManager.shouldNotify(updateInfo, state, true));
    }

    @Test
    public void nonUpdateStatusShouldNotCreateNotificationMarker() {
        assertEquals(AutoUpdateManager.notificationMarker(UpdateInfo.noUpdateAvailable("ok")), "");
        assertEquals(AutoUpdateManager.notificationMarker(UpdateInfo.checkFailed("bad")), "");
    }

    @Test
    public void nonUpdateStatusShouldStillPassNotificationGate() {
        UpdateCheckState state = UpdateCheckState.of(
                UpdateTarget.APP,
                123L,
                Set.of("app@v1.2.0@UPDATE_AVAILABLE")
        );

        assertTrue(AutoUpdateManager.shouldNotify(UpdateInfo.noUpdateAvailable("ok"), state, true));
        assertTrue(AutoUpdateManager.shouldNotify(UpdateInfo.checkFailed("bad"), state, true));
    }

    @Test
    public void nullUpdateInfoShouldNotNotify() {
        assertFalse(AutoUpdateManager.shouldNotify(null, UpdateCheckState.of(UpdateTarget.APP, 0L, Set.of()), false));
    }
}
