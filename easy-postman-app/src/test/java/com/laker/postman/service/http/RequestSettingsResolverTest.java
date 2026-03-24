package com.laker.postman.service.http;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

public class RequestSettingsResolverTest {

    @Test
    public void shouldReflectProxyForcedSslDisableInEffectiveResolution() {
        boolean oldProxyEnabled = SettingManager.isProxyEnabled();
        boolean oldProxySslDisabled = SettingManager.isProxySslVerificationDisabled();

        try {
            SettingManager.setProxyEnabled(true);
            SettingManager.setProxySslVerificationDisabled(true);

            HttpRequestItem item = new HttpRequestItem();
            item.setSslVerificationEnabled(Boolean.TRUE);

            assertFalse(RequestSettingsResolver.resolveSslVerificationEnabled(item));
        } finally {
            SettingManager.setProxyEnabled(oldProxyEnabled);
            SettingManager.setProxySslVerificationDisabled(oldProxySslDisabled);
        }
    }

    @Test
    public void shouldNormalizeLegacyStoredDefaultsForComparison() {
        HttpRequestItem item = new HttpRequestItem();
        item.setCookieJarEnabled(Boolean.TRUE);
        item.setHttpVersion(HttpRequestItem.HTTP_VERSION_AUTO);

        HttpRequestItem normalized = RequestSettingsResolver.normalizeStoredSettings(item);

        assertNull(normalized.getCookieJarEnabled());
        assertNull(normalized.getHttpVersion());
    }

    @Test
    public void shouldKeepExplicitStoredOverridesDuringNormalization() {
        HttpRequestItem item = new HttpRequestItem();
        item.setCookieJarEnabled(Boolean.FALSE);
        item.setHttpVersion(HttpRequestItem.HTTP_VERSION_HTTP_1_1);

        HttpRequestItem normalized = RequestSettingsResolver.normalizeStoredSettings(item);

        assertEquals(normalized.getCookieJarEnabled(), Boolean.FALSE);
        assertEquals(normalized.getHttpVersion(), HttpRequestItem.HTTP_VERSION_HTTP_1_1);
    }
}
