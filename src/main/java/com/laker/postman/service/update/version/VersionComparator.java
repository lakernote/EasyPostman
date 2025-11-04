package com.laker.postman.service.update.version;

/**
 * 版本比较工具
 */
public class VersionComparator {

    /**
     * 比较两个版本号
     *
     * @param v1 版本1（可以带 'v' 前缀）
     * @param v2 版本2（可以带 'v' 前缀）
     * @return 正数表示 v1 > v2，0 表示相等，负数表示 v1 < v2
     */
    public static int compare(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return 0;
        }

        String s1 = removeVersionPrefix(v1);
        String s2 = removeVersionPrefix(v2);

        String[] arr1 = s1.split("\\.");
        String[] arr2 = s2.split("\\.");
        int len = Math.max(arr1.length, arr2.length);

        for (int i = 0; i < len; i++) {
            int n1 = i < arr1.length ? parseIntSafely(arr1[i]) : 0;
            int n2 = i < arr2.length ? parseIntSafely(arr2[i]) : 0;
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return 0;
    }

    /**
     * 移除版本号前缀（如 'v'）
     */
    private static String removeVersionPrefix(String version) {
        return version.startsWith("v") ? version.substring(1) : version;
    }

    /**
     * 安全解析整数（移除非数字字符）
     */
    private static int parseIntSafely(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 判断版本1是否大于版本2
     */
    public static boolean isNewer(String v1, String v2) {
        return compare(v1, v2) > 0;
    }
}

