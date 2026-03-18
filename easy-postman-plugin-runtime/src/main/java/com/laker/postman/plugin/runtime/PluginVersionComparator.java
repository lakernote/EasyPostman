package com.laker.postman.plugin.runtime;

final class PluginVersionComparator {

    private PluginVersionComparator() {
    }

    static int compare(String v1, String v2) {
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
