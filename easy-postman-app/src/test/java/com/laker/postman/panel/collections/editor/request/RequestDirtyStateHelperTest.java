package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.defaults.GeneratedRequestHeaderPolicy;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.service.setting.SettingManager;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RequestDirtyStateHelperTest {
    private static final String USER_AGENT_VALUE = "EasyPostman/test";
    private static final GeneratedRequestHeaderPolicy GENERATED_HEADER_POLICY =
            GeneratedRequestHeaderPolicy.standard(USER_AGENT_VALUE);

    @Test
    public void shouldTreatDefaultRequestSettingsAsUnmodified() {
        HttpRequestItem original = createBaseItem();

        HttpRequestItem current = createBaseItem();
        current.setCookieJarEnabled(Boolean.TRUE);
        current.setHttpVersion(HttpRequestItem.HTTP_VERSION_AUTO);

        AtomicReference<HttpRequestItem> ref = new AtomicReference<>(current);
        RequestDirtyStateHelper helper = newHelper(ref);
        helper.setOriginalRequestItem(original);

        assertFalse(helper.isModified());
    }

    @Test
    public void shouldTreatGeneratedDefaultHeadersAsUnmodified() {
        HttpRequestItem original = createBaseItem();

        HttpRequestItem current = createBaseItem();
        current.setHeadersList(List.of(
                new HttpHeader(true, "User-Agent", USER_AGENT_VALUE),
                new HttpHeader(true, "Accept", "*/*"),
                new HttpHeader(true, "Accept-Encoding", "gzip, deflate, br"),
                new HttpHeader(true, "Connection", "keep-alive")
        ));

        AtomicReference<HttpRequestItem> ref = new AtomicReference<>(current);
        RequestDirtyStateHelper helper = newHelper(ref);
        helper.setOriginalRequestItem(original);

        assertFalse(helper.isModified());
    }

    @Test
    public void shouldTreatChangedDefaultHeaderAsModified() {
        HttpRequestItem original = createBaseItem();

        HttpRequestItem current = createBaseItem();
        current.setHeadersList(List.of(
                new HttpHeader(true, "User-Agent", "custom-client")
        ));

        AtomicReference<HttpRequestItem> ref = new AtomicReference<>(current);
        RequestDirtyStateHelper helper = newHelper(ref);
        helper.setOriginalRequestItem(original);

        assertTrue(helper.isModified());
    }

    @Test
    public void shouldTreatDeletingPersistedDefaultHeaderAsModified() {
        HttpRequestItem original = createBaseItem();
        original.setHeadersList(List.of(
                new HttpHeader(true, "Accept", "*/*")
        ));

        HttpRequestItem current = createBaseItem();

        AtomicReference<HttpRequestItem> ref = new AtomicReference<>(current);
        RequestDirtyStateHelper helper = newHelper(ref);
        helper.setOriginalRequestItem(original);

        assertTrue(helper.isModified());
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
            RequestDirtyStateHelper helper = newHelper(ref);
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
            RequestDirtyStateHelper helper = newHelper(ref);
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

    private static RequestDirtyStateHelper newHelper(AtomicReference<HttpRequestItem> ref) {
        return new RequestDirtyStateHelper(ref::get, dirty -> {
        }, GENERATED_HEADER_POLICY);
    }
}
