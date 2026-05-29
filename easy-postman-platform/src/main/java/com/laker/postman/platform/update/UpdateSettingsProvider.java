package com.laker.postman.platform.update;

/**
 * Minimal settings contract needed by platform update discovery.
 *
 * <p>The app adapts its concrete SettingManager to this interface; platform code
 * must not depend on app settings classes directly.</p>
 */
@FunctionalInterface
public interface UpdateSettingsProvider {

    String DEFAULT_SOURCE_PREFERENCE = "auto";

    String getUpdateSourcePreference();

    static UpdateSettingsProvider defaults() {
        return () -> DEFAULT_SOURCE_PREFERENCE;
    }
}
