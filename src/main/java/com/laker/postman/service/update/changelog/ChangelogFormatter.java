package com.laker.postman.service.update.changelog;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 更新日志格式化器 - 将发布信息格式化为可读文本
 */
@Slf4j
public class ChangelogFormatter {

    private static final int MAX_RELEASES = 10;

    /**
     * 格式化发布信息列表
     *
     * @param releases 发布信息数组
     * @return 格式化后的文本
     */
    public String format(JSONArray releases) {
        if (releases == null || releases.isEmpty()) {
            return I18nUtil.getMessage(MessageKeys.CHANGELOG_NO_RELEASES);
        }

        // 按发布时间降序排序
        List<JSONObject> releaseList = sortReleases(releases);

        StringBuilder sb = new StringBuilder();
        int count = Math.min(releaseList.size(), MAX_RELEASES);

        for (int i = 0; i < count; i++) {
            JSONObject release = releaseList.get(i);
            appendRelease(sb, release);
        }

        return sb.toString();
    }

    /**
     * 排序发布信息（最新的在前）
     */
    private List<JSONObject> sortReleases(JSONArray releases) {
        List<JSONObject> releaseList = new ArrayList<>();
        for (int i = 0; i < releases.size(); i++) {
            releaseList.add(releases.getJSONObject(i));
        }

        releaseList.sort((a, b) -> {
            String dateA = getPublishDate(a);
            String dateB = getPublishDate(b);
            return dateB.compareTo(dateA); // 降序
        });

        return releaseList;
    }

    /**
     * 获取发布日期（兼容 GitHub 和 Gitee）
     */
    private String getPublishDate(JSONObject release) {
        // GitHub 使用 published_at，Gitee 使用 created_at
        String date = release.getStr("published_at");
        if (StrUtil.isBlank(date)) {
            date = release.getStr("created_at");
        }
        return StrUtil.isNotBlank(date) ? date : "";
    }

    /**
     * 添加单个发布信息
     */
    private void appendRelease(StringBuilder sb, JSONObject release) {
        String tagName = release.getStr("tag_name");
        String name = release.getStr("name");
        String body = release.getStr("body");
        String date = getPublishDate(release);

        // 标题分隔线
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // 版本号
        String displayName = StrUtil.isNotBlank(name) ? name : tagName;
        sb.append("📦 ").append(displayName).append("\n");

        // 发布日期
        String displayDate = date.length() >= 10 ? date.substring(0, 10) : "Unknown";
        sb.append("📅 ").append(displayDate).append("\n");

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // 更新说明
        if (StrUtil.isNotBlank(body)) {
            sb.append(body.trim()).append("\n\n\n");
        } else {
            String noNotes = I18nUtil.isChinese() ? "无更新说明" : "No release notes provided";
            sb.append(noNotes).append("\n\n\n");
        }
    }
}

