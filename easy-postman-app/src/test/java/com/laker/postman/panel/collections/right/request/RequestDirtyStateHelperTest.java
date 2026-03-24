package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RequestDirtyStateHelperTest {

    @Test
    public void shouldTreatLegacyDefaultEncodingsAsUnmodified() {
        HttpRequestItem original = createBaseItem();

        HttpRequestItem current = createBaseItem();
        current.setCookieJarEnabled(Boolean.TRUE);
        current.setHttpVersion(HttpRequestItem.HTTP_VERSION_AUTO);

        AtomicReference<HttpRequestItem> ref = new AtomicReference<>(current);
        RequestDirtyStateHelper helper = new RequestDirtyStateHelper(ref::get, dirty -> {
        });
        helper.setOriginalRequestItem(original);

        assertFalse(helper.isModified());
    }

    @Test
    public void shouldTreatExplicitTimeoutMatchingGlobalValueAsModified() {
        int oldRequestTimeout = SettingManager.getRequestTimeout();

        try {
            SettingManager.setRequestTimeout(5000);

            HttpRequestItem original = createBaseItem();

            HttpRequestItem current = createBaseItem();
            current.setRequestTimeoutMs(5000);

            AtomicReference<HttpRequestItem> ref = new AtomicReference<>(current);
            RequestDirtyStateHelper helper = new RequestDirtyStateHelper(ref::get, dirty -> {
            });
            helper.setOriginalRequestItem(original);

            assertTrue(helper.isModified());
        } finally {
            SettingManager.setRequestTimeout(oldRequestTimeout);
        }
    }

    @Test
    public void shouldTreatExplicitFollowRedirectsMatchingGlobalValueAsModified() {
        boolean oldFollowRedirects = SettingManager.isFollowRedirects();

        try {
            SettingManager.setFollowRedirects(true);

            HttpRequestItem original = createBaseItem();

            HttpRequestItem current = createBaseItem();
            current.setFollowRedirects(Boolean.TRUE);

            AtomicReference<HttpRequestItem> ref = new AtomicReference<>(current);
            RequestDirtyStateHelper helper = new RequestDirtyStateHelper(ref::get, dirty -> {
            });
            helper.setOriginalRequestItem(original);

            assertTrue(helper.isModified());
        } finally {
            SettingManager.setFollowRedirects(oldFollowRedirects);
        }
    }

    private static HttpRequestItem createBaseItem() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-1");
        item.setName("Request 1");
        item.setUrl("https://api.example.com");
        item.setMethod("GET");
        return item;
    }
}
