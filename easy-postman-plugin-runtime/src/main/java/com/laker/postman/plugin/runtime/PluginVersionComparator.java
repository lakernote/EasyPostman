package com.laker.postman.plugin.runtime;

final class PluginVersionComparator {

    private PluginVersionComparator() {
    }

    static int compare(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return 0;
        }
        String[] arr1 = trimPrefix(v1).split("\\.");
        String[] arr2 = trimPrefix(v2).split("\\.");
        int len = Math.max(arr1.length, arr2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < arr1.length ? parse(arr1[i]) : 0;
            int n2 = i < arr2.length ? parse(arr2[i]) : 0;
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return 0;
    }

    private static String trimPrefix(String version) {
        return version.startsWith("v") ? version.substring(1) : version;
    }

    private static int parse(String token) {
        try {
            return Integer.parseInt(token.replaceAll("\\D", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
