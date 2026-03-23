package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertFalse;

public class RequestDirtyStateHelperTest {

    @Test
    public void testDefaultRequestSettingsDoNotMarkRequestDirty() {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();
        boolean oldSslVerificationDisabled = SettingManager.isRequestSslVerificationDisabled();
        int oldRequestTimeout = SettingManager.getRequestTimeout();

        try {
            SettingManager.setFollowRedirects(true);
            SettingManager.setRequestSslVerificationDisabled(false);
            SettingManager.setRequestTimeout(5000);

            HttpRequestItem original = new HttpRequestItem();
            original.setId("request-1");
            original.setName("Request 1");
            original.setUrl("https://api.example.com");
            original.setMethod("GET");

            HttpRequestItem current = new HttpRequestItem();
            current.setId("request-1");
            current.setName("Request 1");
            current.setUrl("https://api.example.com");
            current.setMethod("GET");
            current.setFollowRedirects(Boolean.TRUE);
            current.setCookieJarEnabled(Boolean.TRUE);
            current.setSslVerificationEnabled(Boolean.TRUE);
            current.setHttpVersion(HttpRequestItem.HTTP_VERSION_AUTO);
            current.setRequestTimeoutMs(5000);

            AtomicReference<HttpRequestItem> ref = new AtomicReference<>(current);
            RequestDirtyStateHelper helper = new RequestDirtyStateHelper(ref::get, dirty -> {
            });
            helper.setOriginalRequestItem(original);

            assertFalse(helper.isModified());
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
            SettingManager.setRequestSslVerificationDisabled(oldSslVerificationDisabled);
            SettingManager.setRequestTimeout(oldRequestTimeout);
        }
    }
}
