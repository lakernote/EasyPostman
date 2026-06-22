package com.laker.postman.panel.topmenu.setting;

import okhttp3.HttpUrl;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WebDavSyncSettingsPanelTest {

    @Test
    public void shouldWarnOnlyForNonLocalHttpWebDavUrls() {
        assertFalse(WebDavSyncSettingsPanel.shouldWarnInsecureHttp(HttpUrl.parse("https://example.com/dav/")));
        assertFalse(WebDavSyncSettingsPanel.shouldWarnInsecureHttp(HttpUrl.parse("http://localhost:8088/")));
        assertFalse(WebDavSyncSettingsPanel.shouldWarnInsecureHttp(HttpUrl.parse("http://127.0.0.1:8088/")));
        assertFalse(WebDavSyncSettingsPanel.shouldWarnInsecureHttp(HttpUrl.parse("http://[::1]:8088/")));

        assertTrue(WebDavSyncSettingsPanel.shouldWarnInsecureHttp(HttpUrl.parse("http://example.com/dav/")));
        assertTrue(WebDavSyncSettingsPanel.shouldWarnInsecureHttp(HttpUrl.parse("http://192.168.1.20/dav/")));
    }
}
