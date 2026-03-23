package com.laker.postman.service.http;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;

/**
 * Centralizes resolution of request-level settings against global defaults so
 * the send path, dirty-state comparison, and settings UI stay in sync.
 */
@UtilityClass
public class RequestSettingsResolver {

    public static boolean resolveFollowRedirects(HttpRequestItem item) {
        return item != null && item.getFollowRedirects() != null
                ? item.getFollowRedirects()
                : SettingManager.isFollowRedirects();
    }

    public static boolean resolveCookieJarEnabled(HttpRequestItem item) {
        return item == null || item.getCookieJarEnabled() == null || item.getCookieJarEnabled();
    }

    public static boolean resolveSslVerificationEnabled(HttpRequestItem item) {
        return item != null && item.getSslVerificationEnabled() != null
                ? item.getSslVerificationEnabled()
                : !SettingManager.isRequestSslVerificationDisabled();
    }

    public static String resolveHttpVersion(HttpRequestItem item) {
        return item != null ? item.resolveHttpVersion() : HttpRequestItem.HTTP_VERSION_AUTO;
    }

    public static int resolveRequestTimeoutMs(HttpRequestItem item) {
        return item != null && item.getRequestTimeoutMs() != null
                ? item.getRequestTimeoutMs()
                : SettingManager.getRequestTimeout();
    }

    public static HttpRequestItem normalizeForComparison(HttpRequestItem item) {
        HttpRequestItem copy = JsonUtil.deepCopy(item, HttpRequestItem.class);
        if (copy == null) {
            return null;
        }

        copy.setFollowRedirects(resolveFollowRedirects(copy));
        copy.setCookieJarEnabled(resolveCookieJarEnabled(copy));
        copy.setSslVerificationEnabled(resolveSslVerificationEnabled(copy));
        copy.setHttpVersion(resolveHttpVersion(copy));
        copy.setRequestTimeoutMs(resolveRequestTimeoutMs(copy));
        return copy;
    }
}
