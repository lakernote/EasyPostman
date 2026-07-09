package com.laker.postman.http.runtime.app;

import com.laker.postman.certificate.TrustedCertificateEntry;
import com.laker.postman.http.runtime.config.HttpRuntimeSettings;
import com.laker.postman.service.setting.SettingManager;

import java.util.List;

public class AppHttpRuntimeSettingsAdapter implements HttpRuntimeSettings {
    @Override
    public int getMaxBodySize() {
        return SettingManager.getMaxBodySize();
    }

    @Override
    public int getMaxDownloadSize() {
        return SettingManager.getMaxDownloadSize();
    }

    @Override
    public int getRequestTimeout() {
        return SettingManager.getRequestTimeout();
    }

    @Override
    public boolean isFollowRedirects() {
        return SettingManager.isFollowRedirects();
    }

    @Override
    public boolean isRequestSslVerificationDisabled() {
        return SettingManager.isRequestSslVerificationDisabled();
    }

    @Override
    public boolean isProxyEnabled() {
        return SettingManager.isProxyEnabled();
    }

    @Override
    public String getProxyMode() {
        return SettingManager.getProxyMode();
    }

    @Override
    public String getProxyType() {
        return SettingManager.getProxyType();
    }

    @Override
    public String getProxyHost() {
        return SettingManager.getProxyHost();
    }

    @Override
    public int getProxyPort() {
        return parseRuntimeProxyPort(SettingManager.getProxyPortText());
    }

    @Override
    public String getProxyUsername() {
        return SettingManager.getProxyUsername();
    }

    @Override
    public String getProxyPassword() {
        return SettingManager.getProxyPassword();
    }

    @Override
    public boolean isProxySslVerificationDisabled() {
        return SettingManager.isProxySslVerificationDisabled();
    }

    @Override
    public boolean isCustomTrustMaterialEnabled() {
        return SettingManager.isCustomTrustMaterialEnabled();
    }

    @Override
    public List<TrustedCertificateEntry> getCustomTrustMaterialEntries() {
        return SettingManager.getCustomTrustMaterialEntries();
    }

    private int parseRuntimeProxyPort(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            int port = Integer.parseInt(value.trim());
            return port > 0 && port <= 65535 ? port : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
