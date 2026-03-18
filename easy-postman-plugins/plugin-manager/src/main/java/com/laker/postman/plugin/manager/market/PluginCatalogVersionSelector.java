package com.laker.postman.plugin.manager.market;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 catalog 历史版本里为当前宿主挑选“最合适的一条”。
 * <p>
 * 规则：
 * 1. 同一 plugin id 优先选当前宿主可兼容的最高版本
 * 2. 如果一个兼容版本都没有，回退为最新版本，用于向用户表达“需要升级宿主”
 * </p>
 */
public final class PluginCatalogVersionSelector {

    private static final Comparator<PluginCatalogEntry> VERSION_DESC =
            (left, right) -> compareVersions(right.version(), left.version());

    private PluginCatalogVersionSelector() {
    }

    public static List<PluginCatalogEntry> selectForHost(List<PluginCatalogEntry> entries,
                                                         String currentAppVersion,
                                                         String currentPlatformVersion) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        Map<String, List<PluginCatalogEntry>> grouped = new LinkedHashMap<>();
        for (PluginCatalogEntry entry : entries) {
            if (entry == null || entry.id() == null || entry.id().isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(entry.id(), ignored -> new ArrayList<>()).add(entry);
        }

        List<PluginCatalogEntry> selected = new ArrayList<>();
        for (List<PluginCatalogEntry> candidates : grouped.values()) {
            candidates.sort(VERSION_DESC);
            PluginCatalogEntry compatible = candidates.stream()
                    .filter(entry -> isCompatibleWithHost(entry, currentAppVersion, currentPlatformVersion))
                    .findFirst()
                    .orElse(null);
            selected.add(compatible != null ? compatible : candidates.get(0));
        }

        selected.sort(Comparator.comparing(PluginCatalogEntry::id));
        return selected;
    }

    private static boolean isCompatibleWithHost(PluginCatalogEntry entry,
                                                String currentAppVersion,
                                                String currentPlatformVersion) {
        return isVersionInRange(currentAppVersion, entry.minAppVersion(), entry.maxAppVersion())
                && isVersionInRange(currentPlatformVersion, entry.minPlatformVersion(), entry.maxPlatformVersion());
    }

    private static boolean isVersionInRange(String currentVersion, String minVersion, String maxVersion) {
        if (minVersion != null && !minVersion.isBlank()
                && compareVersions(currentVersion, minVersion) < 0) {
            return false;
        }
        return maxVersion == null || maxVersion.isBlank()
                || compareVersions(currentVersion, maxVersion) <= 0;
    }

    private static int compareVersions(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return 0;
        }
        String normalizedV1 = trimPrefix(v1);
        String normalizedV2 = trimPrefix(v2);
        String[] arr1 = coreVersion(normalizedV1).split("\\.");
        String[] arr2 = coreVersion(normalizedV2).split("\\.");
        int len = Math.max(arr1.length, arr2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < arr1.length ? parse(arr1[i]) : 0;
            int n2 = i < arr2.length ? parse(arr2[i]) : 0;
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return compareQualifier(qualifier(normalizedV1), qualifier(normalizedV2));
    }

    private static String trimPrefix(String version) {
        return version.startsWith("v") ? version.substring(1) : version;
    }

    private static String coreVersion(String version) {
        int separatorIndex = qualifierSeparatorIndex(version);
        return separatorIndex >= 0 ? version.substring(0, separatorIndex) : version;
    }

    private static String qualifier(String version) {
        int separatorIndex = qualifierSeparatorIndex(version);
        return separatorIndex >= 0 && separatorIndex + 1 < version.length()
                ? version.substring(separatorIndex + 1)
                : "";
    }

    private static int qualifierSeparatorIndex(String version) {
        int dash = version.indexOf('-');
        int plus = version.indexOf('+');
        if (dash < 0) {
            return plus;
        }
        if (plus < 0) {
            return dash;
        }
        return Math.min(dash, plus);
    }

    private static int compareQualifier(String q1, String q2) {
        boolean blank1 = q1 == null || q1.isBlank();
        boolean blank2 = q2 == null || q2.isBlank();
        if (blank1 && blank2) {
            return 0;
        }
        if (blank1) {
            return 1;
        }
        if (blank2) {
            return -1;
        }
        return q1.compareToIgnoreCase(q2);
    }

    private static int parse(String token) {
        try {
            return Integer.parseInt(token.replaceAll("\\D", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
