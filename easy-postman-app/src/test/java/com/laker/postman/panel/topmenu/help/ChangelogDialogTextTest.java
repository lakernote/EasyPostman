package com.laker.postman.panel.topmenu.help;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.platform.update.changelog.ChangelogFormatter;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ChangelogDialogTextTest {

    @Test
    public void changelogSourceButtonsShouldUseCompactEnglishAndChineseLabels() {
        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.CHINESE);

        assertEquals(en.getString(MessageKeys.CHANGELOG_VIEW_ON_GITHUB), "GitHub");
        assertEquals(en.getString(MessageKeys.CHANGELOG_VIEW_ON_GITEE), "Gitee");
        assertEquals(zh.getString(MessageKeys.CHANGELOG_VIEW_ON_GITHUB), "GitHub");
        assertEquals(zh.getString(MessageKeys.CHANGELOG_VIEW_ON_GITEE), "Gitee");
    }

    @Test
    public void changelogFormatterShouldUsePlainCompactReleaseBlocks() {
        JSONArray releases = new JSONArray();
        JSONObject release = new JSONObject();
        release.set("tag_name", "v6.0.5");
        release.set("published_at", "2026-06-14T08:00:00Z");
        release.set("body", "- Fixed bugs");
        releases.add(release);

        String result = new ChangelogFormatter().format(releases);

        assertTrue(result.startsWith("v6.0.5  2026-06-14\n"));
        assertTrue(result.contains("- Fixed bugs"));
        assertFalse(result.contains("📦"));
        assertFalse(result.contains("📅"));
        assertFalse(result.contains("━━━━"));
    }
}
