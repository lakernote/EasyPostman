package com.laker.postman.http.runtime.config;

import com.laker.postman.certificate.TrustedCertificateEntry;

import java.util.List;

public interface HttpRuntimeSettings {
    String PROXY_MODE_MANUAL = "MANUAL";
    String PROXY_MODE_SYSTEM = "SYSTEM";
    String PROXY_TYPE_HTTP = "HTTP";
    String PROXY_TYPE_SOCKS = "SOCKS";

    default int getMaxBodySize() {
        return 1024 * 1024;
    }

    default int getMaxDownloadSize() {
        return 10 * 1024 * 1024;
    }

    default int getRequestTimeout() {
        return 0;
    }

    default boolean isFollowRedirects() {
        return true;
    }

    default boolean isRequestSslVerificationDisabled() {
        return false;
    }

    default boolean isProxyEnabled() {
        return false;
    }

    default String getProxyMode() {
        return PROXY_MODE_MANUAL;
    }

    default boolean isSystemProxyMode() {
        return PROXY_MODE_SYSTEM.equalsIgnoreCase(getProxyMode());
    }

    default String getProxyType() {
        return PROXY_TYPE_HTTP;
    }

    default String getProxyHost() {
        return "";
    }

    default int getProxyPort() {
        return 0;
    }

    default String getProxyUsername() {
        return "";
    }

    default String getProxyPassword() {
        return "";
    }

    default boolean isProxySslVerificationDisabled() {
        return false;
    }

    default boolean isCustomTrustMaterialEnabled() {
        return false;
    }

    default List<TrustedCertificateEntry> getCustomTrustMaterialEntries() {
        return List.of();
    }
}
