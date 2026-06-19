package com.laker.postman.panel.update;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONObject;
import com.laker.postman.platform.update.model.UpdateInfo;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

@UtilityClass
class UpdateTextFormatter {
    private static final int NOTIFICATION_DESCRIPTION_MAX_LENGTH = 60;
    private static final double WORD_BREAK_MIN_RATIO = 0.7d;

    String dialogVersionText(String currentVersion, String latestVersion) {
        return I18nUtil.getMessage(MessageKeys.UPDATE_VERSION_PREFIX) + "  "
                + versionTransition(currentVersion, latestVersion);
    }

    String versionTransition(String currentVersion, String latestVersion) {
        return displayVersion(currentVersion) + "  \u2192  " + displayVersion(latestVersion);
    }

    String releasedOnText(String publishedAt) {
        if (publishedAt == null || publishedAt.isBlank()) {
            return "";
        }
        String dateText = publishedAt.length() >= 10 ? publishedAt.substring(0, 10) : publishedAt;
        return I18nUtil.getMessage(MessageKeys.UPDATE_RELEASED_ON, dateText);
    }

    String changelog(JSONObject releaseInfo) {
        if (releaseInfo == null) {
            return I18nUtil.getMessage(MessageKeys.UPDATE_NO_CHANGELOG);
        }
        String body = releaseInfo.getStr("body");
        if (CharSequenceUtil.isBlank(body)) {
            return I18nUtil.getMessage(MessageKeys.UPDATE_DEFAULT_CHANGELOG);
        }
        return body.trim()
                .replaceAll("(?m)^#{1,6}\\s+", "\u25b8 ")
                .replaceAll("(?m)^[-*]\\s+", "  \u2022 ")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("```[\\s\\S]*?```", I18nUtil.getMessage(MessageKeys.UPDATE_CODE_EXAMPLE))
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[(.+?)]\\(.+?\\)", "$1")
                .replaceAll("\\n{3,}", "\n\n");
    }

    String notificationDescription(UpdateInfo updateInfo) {
        if (updateInfo.getReleaseInfo() == null) {
            return I18nUtil.getMessage(MessageKeys.UPDATE_NOTIFICATION_VIEW_DETAILS_DESCRIPTION);
        }
        String body = updateInfo.getReleaseInfo().getStr("body", "");
        if (CharSequenceUtil.isBlank(body)) {
            return I18nUtil.getMessage(MessageKeys.UPDATE_NOTIFICATION_DEFAULT_DESCRIPTION);
        }
        String cleaned = cleanMarkdownToPlainText(body);
        if (cleaned.isEmpty()) {
            return I18nUtil.getMessage(MessageKeys.UPDATE_NOTIFICATION_DEFAULT_DESCRIPTION);
        }
        return truncateForNotification(cleaned);
    }

    String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String cleanMarkdownToPlainText(String body) {
        return body.trim()
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .replaceAll("(?m)^[-*]\\s+", "")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[(.+?)]\\([^)]+\\)", "$1")
                .replaceAll("\\n+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncateForNotification(String value) {
        if (value.length() <= NOTIFICATION_DESCRIPTION_MAX_LENGTH) {
            return value;
        }
        int lastSpace = value.lastIndexOf(' ', NOTIFICATION_DESCRIPTION_MAX_LENGTH);
        int minWordBreak = (int) (NOTIFICATION_DESCRIPTION_MAX_LENGTH * WORD_BREAK_MIN_RATIO);
        return lastSpace > minWordBreak
                ? value.substring(0, lastSpace) + "..."
                : value.substring(0, NOTIFICATION_DESCRIPTION_MAX_LENGTH) + "...";
    }

    private String displayVersion(String version) {
        return version == null || version.isBlank() ? "-" : version;
    }
}
