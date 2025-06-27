package com.laker.postman.service.http;

import java.util.Map;

public class HttpUtil {

    /**
     * Postman 的行为是：对于 params，只有空格和部分特殊字符会被编码，像 : 这样的字符不会被编码。
     * 只对空格、&、=、?、# 这些必须编码的字符进行编码，: 不编码。
     * 对已编码的 %XX 不再重复编码。
     */
    public static String encodeURIComponent(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            // 检查是否为已编码的 %XX
            if (c == '%' && i + 2 < s.length()
                    && isHexChar(s.charAt(i + 1))
                    && isHexChar(s.charAt(i + 2))) {
                sb.append(s, i, i + 3);
                i += 3;
            } else if (c == ' ') {
                sb.append("%20");
                i++;
            } else if (c == '&' || c == '=' || c == '?' || c == '#') {
                sb.append(String.format("%%%02X", (int) c));
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }


    /**
     * 判断headers中是否已包含指定Content-Type（忽略大小写和内容）
     */
    public static boolean containsContentType(Map<String, String> headers, String targetType) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if ("Content-Type".equalsIgnoreCase(entry.getKey()) &&
                    entry.getValue() != null &&
                    entry.getValue().toLowerCase().contains(targetType.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将字节数转换为人类可读的格式
     *
     * @param bytes 字节数
     * @return 格式化后的字符串，例如 "1.5 MB" 或 "512 KB"
     */
    public static String getSizeText(int bytes) {
        String sizeText = "";
        if (bytes < 1024) {
            sizeText = String.format("%d B", bytes);
        } else if (bytes < 1024 * 1024) {
            sizeText = String.format("%.1f KB", bytes / 1024.0);
        } else {
            sizeText = String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
        return sizeText;
    }
}
