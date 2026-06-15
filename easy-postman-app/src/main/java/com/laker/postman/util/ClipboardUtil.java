package com.laker.postman.util;

import java.util.concurrent.CompletableFuture;

public final class ClipboardUtil {

    private ClipboardUtil() {
    }

    /**
     * 检查剪贴板是否有 cURL 命令，有则返回文本，否则返回 null。
     */
    public static CompletableFuture<String> getClipboardCurlTextAsync() {
        return AsyncClipboardUtil.readStringAsync().thenApply(ClipboardUtil::extractCurlText);
    }

    public static String extractCurlText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (startsWithCurlCommand(trimmed)) {
            return trimmed;
        }
        return null;
    }

    private static boolean startsWithCurlCommand(String text) {
        if (text.length() < 4 || !text.regionMatches(true, 0, "curl", 0, 4)) {
            return false;
        }
        return text.length() == 4 || Character.isWhitespace(text.charAt(4));
    }
}
