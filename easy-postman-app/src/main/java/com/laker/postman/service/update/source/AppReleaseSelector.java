package com.laker.postman.service.update.source;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.core.util.StrUtil;

/**
 * 只选择宿主应用正式版 release，排除插件 release、草稿和预发布。
 */
final class AppReleaseSelector {

    private AppReleaseSelector() {
    }

    static JSONObject selectLatestStableAppRelease(JSONArray releases) {
        JSONArray filtered = selectStableAppReleases(releases, 1);
        if (filtered == null || filtered.isEmpty()) {
            return null;
        }
        return filtered.getJSONObject(0);
    }

    static JSONArray selectStableAppReleases(JSONArray releases, int limit) {
        if (releases == null || releases.isEmpty()) {
            return releases;
        }

        JSONArray filtered = new JSONArray();
        for (int i = 0; i < releases.size(); i++) {
            JSONObject release = releases.getJSONObject(i);
            if (!isStableAppRelease(release)) {
                continue;
            }
            filtered.add(release);
            if (limit > 0 && filtered.size() >= limit) {
                break;
            }
        }
        return filtered;
    }

    static boolean isStableAppRelease(JSONObject release) {
        if (release == null) {
            return false;
        }
        if (release.getBool("draft", false) || release.getBool("prerelease", false)) {
            return false;
        }
        return isAppReleaseTag(release.getStr("tag_name"));
    }

    static boolean isAppReleaseTag(String tagName) {
        if (StrUtil.isBlank(tagName)) {
            return false;
        }
        return tagName.startsWith("v");
    }
}
