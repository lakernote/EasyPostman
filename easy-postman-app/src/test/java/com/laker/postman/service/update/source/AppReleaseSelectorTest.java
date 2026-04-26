package com.laker.postman.service.update.source;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class AppReleaseSelectorTest {

    @Test
    public void shouldIgnorePluginReleaseTags() {
        assertTrue(AppReleaseSelector.isAppReleaseTag("v5.3.16"));
        assertFalse(AppReleaseSelector.isAppReleaseTag("plugin-redis-v5.3.16"));
        assertFalse(AppReleaseSelector.isAppReleaseTag("plugin-kafka-v5.3.17"));
        assertFalse(AppReleaseSelector.isAppReleaseTag(""));
    }

    @Test
    public void shouldSelectOnlyStableAppReleases() {
        JSONArray releases = new JSONArray();
        releases.add(release("plugin-redis-v5.3.16", false, false));
        releases.add(release("v5.3.18-rc1", false, true));
        releases.add(release("v5.3.17", false, false));
        releases.add(release("v5.3.16", false, false));
        releases.add(release("v5.3.15", true, false));

        JSONArray filtered = AppReleaseSelector.selectStableAppReleases(releases, 10);

        assertEquals(filtered.size(), 2);
        assertEquals(filtered.getJSONObject(0).getStr("tag_name"), "v5.3.17");
        assertEquals(filtered.getJSONObject(1).getStr("tag_name"), "v5.3.16");
    }

    @Test
    public void shouldPickLatestStableAppReleaseFromMixedReleaseList() {
        JSONArray releases = new JSONArray();
        releases.add(release("plugin-redis-v5.3.16", false, false));
        releases.add(release("plugin-kafka-v5.3.17", false, false));
        releases.add(release("v5.3.17-beta", false, true));
        releases.add(release("v5.3.17", false, false));

        JSONObject latest = AppReleaseSelector.selectLatestStableAppRelease(releases);

        assertNotNull(latest);
        assertEquals(latest.getStr("tag_name"), "v5.3.17");
    }

    @Test
    public void shouldPickNewestStableAppReleaseWhenSourceReturnsOldestFirst() {
        JSONArray releases = new JSONArray();
        releases.add(release("v4.3.40", false, false));
        releases.add(release("plugin-redis-v5.3.18", false, false));
        releases.add(release("plugin-kafka-v5.3.18", false, false));
        releases.add(release("v5.3.18", false, false));
        releases.add(release("plugin-kafka-v5.3.23", false, false));
        releases.add(release("v5.4.14", false, false));
        releases.add(release("v5.4.20", false, false));
        releases.add(release("v5.4.23", false, false));

        JSONObject latest = AppReleaseSelector.selectLatestStableAppRelease(releases);

        assertNotNull(latest);
        assertEquals(latest.getStr("tag_name"), "v5.4.23");
    }

    private static JSONObject release(String tagName, boolean draft, boolean prerelease) {
        JSONObject release = new JSONObject();
        release.set("tag_name", tagName);
        release.set("draft", draft);
        release.set("prerelease", prerelease);
        return release;
    }
}
