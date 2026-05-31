package com.laker.postman.http.request;

import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class HttpRequestSettingsResolverTest {

    @Test
    public void shouldResolveSslVerificationFromGlobalSettingsOnly() {
        boolean oldProxyEnabled = SettingManager.isProxyEnabled();
        String oldProxyMode = SettingManager.getProxyMode();
        String oldProxyHost = SettingManager.getProxyHost();
        boolean oldProxySslDisabled = SettingManager.isProxySslVerificationDisabled();
        boolean oldRequestSslDisabled = SettingManager.isRequestSslVerificationDisabled();

        try {
            SettingManager.setProxyEnabled(true);
            SettingManager.setProxyMode(SettingManager.PROXY_MODE_MANUAL);
            SettingManager.setProxyHost("127.0.0.1");
            SettingManager.setProxySslVerificationDisabled(true);
            SettingManager.setRequestSslVerificationDisabled(true);

            assertFalse(HttpRequestSettingsResolver.resolveSslVerificationEnabled(new HttpRequestItem()));
            assertTrue(HttpRequestSettingsResolver.isProxySslVerificationForcedDisabled());
        } finally {
            SettingManager.setProxyEnabled(oldProxyEnabled);
            SettingManager.setProxyMode(oldProxyMode);
            SettingManager.setProxyHost(oldProxyHost);
            SettingManager.setProxySslVerificationDisabled(oldProxySslDisabled);
            SettingManager.setRequestSslVerificationDisabled(oldRequestSslDisabled);
        }
    }

    @Test
    public void shouldNotForceProxySslDisableWhenSystemProxyBypassesTarget() {
        boolean oldProxyEnabled = SettingManager.isProxyEnabled();
        String oldProxyMode = SettingManager.getProxyMode();
        boolean oldProxySslDisabled = SettingManager.isProxySslVerificationDisabled();
        ProxySelector originalSelector = ProxySelector.getDefault();

        try {
            SettingManager.setProxyEnabled(true);
            SettingManager.setProxyMode(SettingManager.PROXY_MODE_SYSTEM);
            SettingManager.setProxySslVerificationDisabled(true);

            ProxySelector.setDefault(new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    return List.of(Proxy.NO_PROXY);
                }

                @Override
                public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
                }
            });

            assertFalse(HttpRequestSettingsResolver.isProxySslVerificationForcedDisabled("https://bypass.example.com"));
        } finally {
            ProxySelector.setDefault(originalSelector);
            SettingManager.setProxyEnabled(oldProxyEnabled);
            SettingManager.setProxyMode(oldProxyMode);
            SettingManager.setProxySslVerificationDisabled(oldProxySslDisabled);
        }
    }

    @Test
    public void shouldNormalizeLegacyStoredDefaultsForComparison() {
        HttpRequestItem item = new HttpRequestItem();
        item.setCookieJarEnabled(Boolean.TRUE);
        item.setHttpVersion(HttpRequestItem.HTTP_VERSION_AUTO);

        HttpRequestItem normalized = HttpRequestSettingsResolver.normalizeStoredSettings(item);

        assertNull(normalized.getCookieJarEnabled());
        assertNull(normalized.getHttpVersion());
    }

    @Test
    public void shouldKeepExplicitStoredOverridesDuringNormalization() {
        HttpRequestItem item = new HttpRequestItem();
        item.setCookieJarEnabled(Boolean.FALSE);
        item.setHttpVersion(HttpRequestItem.HTTP_VERSION_HTTP_1_1);

        HttpRequestItem normalized = HttpRequestSettingsResolver.normalizeStoredSettings(item);

        assertEquals(normalized.getCookieJarEnabled(), Boolean.FALSE);
        assertEquals(normalized.getHttpVersion(), HttpRequestItem.HTTP_VERSION_HTTP_1_1);
    }
}
