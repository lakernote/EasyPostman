package com.laker.postman.http.request;

import com.laker.postman.http.runtime.app.AppHttpRuntimeBootstrap;
import com.laker.postman.http.runtime.config.HttpRequestRuntimeSettingsResolver;
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

public class HttpRequestRuntimeSettingsResolverTest {

    @Test
    public void shouldResolveSslVerificationFromGlobalSettingsOnly() {
        AppHttpRuntimeBootstrap.configure();
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

            assertFalse(HttpRequestRuntimeSettingsResolver.resolveSslVerificationEnabled(new HttpRequestItem()));
            assertTrue(HttpRequestRuntimeSettingsResolver.isProxySslVerificationForcedDisabled());
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
        AppHttpRuntimeBootstrap.configure();
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

            assertFalse(HttpRequestRuntimeSettingsResolver.isProxySslVerificationForcedDisabled("https://bypass.example.com"));
        } finally {
            ProxySelector.setDefault(originalSelector);
            SettingManager.setProxyEnabled(oldProxyEnabled);
            SettingManager.setProxyMode(oldProxyMode);
            SettingManager.setProxySslVerificationDisabled(oldProxySslDisabled);
        }
    }

}
