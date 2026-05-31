package com.laker.postman.http.runtime.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpRuntimeSettingsProvider {
    private static final HttpRuntimeSettings DEFAULT_SETTINGS = new HttpRuntimeSettings() {
    };
    private static volatile HttpRuntimeSettings settings = new AppHttpRuntimeSettingsAdapter();

    public static HttpRuntimeSettings get() {
        return settings;
    }

    public static void set(HttpRuntimeSettings newSettings) {
        settings = newSettings == null ? DEFAULT_SETTINGS : newSettings;
    }

    public static void resetToAppSettings() {
        settings = new AppHttpRuntimeSettingsAdapter();
    }
}
