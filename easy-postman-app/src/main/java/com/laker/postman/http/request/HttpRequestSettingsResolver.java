package com.laker.postman.http.request;

import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.http.runtime.config.HttpRuntimeSettings;
import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;

/**
 * Centralizes resolution of request-level settings against global defaults so
 * the send path, dirty-state comparison, and settings UI stay in sync.
 */
@UtilityClass
public class HttpRequestSettingsResolver {
    public static boolean resolveFollowRedirects(HttpRequestItem item) {
        return item != null && item.getFollowRedirects() != null
                ? item.getFollowRedirects()
                : settings().isFollowRedirects();
    }

    public static boolean resolveCookieJarEnabled(HttpRequestItem item) {
        return item == null || item.getCookieJarEnabled() == null || item.getCookieJarEnabled();
    }

    public static boolean resolveSslVerificationEnabled(HttpRequestItem item) {
        return !settings().isRequestSslVerificationDisabled();
    }

    public static boolean isProxySslVerificationForcedDisabled() {
        HttpRuntimeSettings settings = settings();
        if (!settings.isProxyEnabled() || !settings.isProxySslVerificationDisabled()) {
            return false;
        }
        if (settings.isSystemProxyMode()) {
            return true;
        }
        String host = settings.getProxyHost();
        return !host.trim().isEmpty();
    }

    public static boolean isProxySslVerificationForcedDisabled(String url) {
        if (!isProxySslVerificationForcedDisabled()) {
            return false;
        }
        if (!settings().isSystemProxyMode()) {
            return true;
        }
        return OkHttpClientManager.isProxyActiveForUrl(url);
    }

    public static String resolveHttpVersion(HttpRequestItem item) {
        return item != null ? item.resolveHttpVersion() : HttpRequestItem.HTTP_VERSION_AUTO;
    }

    public static int resolveRequestTimeoutMs(HttpRequestItem item) {
        return item != null && item.getRequestTimeoutMs() != null
                ? item.getRequestTimeoutMs()
                : settings().getRequestTimeout();
    }

    public static HttpRequestItem normalizeStoredSettings(HttpRequestItem item) {
        HttpRequestItem copy = JsonUtil.deepCopy(item, HttpRequestItem.class);
        if (copy == null) {
            return null;
        }

        normalizeStoredSettingsInPlace(copy);
        return copy;
    }

    public static HttpRequestItem normalizeForComparison(HttpRequestItem item) {
        return normalizeStoredSettings(item);
    }

    public static void normalizeStoredSettingsInPlace(HttpRequestItem item) {
        if (item == null) {
            return;
        }

        item.setCookieJarEnabled(normalizeStoredCookieJarEnabled(item.getCookieJarEnabled()));
        item.setHttpVersion(normalizeStoredHttpVersion(item.getHttpVersion()));
    }

    public static Boolean normalizeStoredCookieJarEnabled(Boolean cookieJarEnabled) {
        return Boolean.TRUE.equals(cookieJarEnabled) ? null : cookieJarEnabled;
    }

    public static String normalizeStoredHttpVersion(String httpVersion) {
        if (httpVersion == null || httpVersion.trim().isEmpty()) {
            return null;
        }
        return HttpRequestItem.HTTP_VERSION_AUTO.equals(httpVersion) ? null : httpVersion;
    }

    private static HttpRuntimeSettings settings() {
        return HttpRuntimeSettingsProvider.get();
    }
}
