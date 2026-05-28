package com.laker.postman.panel.performance.execution;


final class BoundedTextAccumulator {

    static final int DEFAULT_PREVIEW_BYTES = PerformanceExecutionConfig.DEFAULT_RESPONSE_BODY_PREVIEW_LIMIT_BYTES;

    private final int maxUtf8Bytes;
    private final StringBuilder retained;
    private long totalUtf8Bytes;
    private int retainedUtf8Bytes;
    private boolean truncated;

    BoundedTextAccumulator(int maxUtf8Bytes) {
        this.maxUtf8Bytes = Math.max(0, maxUtf8Bytes);
        this.retained = new StringBuilder(Math.min(this.maxUtf8Bytes, 1024));
    }

    synchronized void append(String value) {
        append(value, 0, value == null ? 0 : value.length());
    }

    synchronized void append(CharSequence value, int start, int end) {
        CharSequence text = value == null ? "" : value;
        int safeStart = Math.max(0, Math.min(start, text.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, text.length()));
        totalUtf8Bytes += utf8Length(text, safeStart, safeEnd);
        if (safeStart == safeEnd) {
            return;
        }
        if (retainedUtf8Bytes >= maxUtf8Bytes) {
            truncated = true;
            return;
        }

        int index = safeStart;
        while (index < safeEnd && retainedUtf8Bytes < maxUtf8Bytes) {
            CharSpan span = charSpan(text, index, safeEnd);
            if (retainedUtf8Bytes + span.utf8Bytes > maxUtf8Bytes) {
                truncated = true;
                return;
            }
            retained.append(text, index, index + span.charCount);
            retainedUtf8Bytes += span.utf8Bytes;
            index += span.charCount;
        }
        if (index < safeEnd) {
            truncated = true;
        }
    }

    synchronized String value() {
        if (!truncated) {
            return retained.toString();
        }
        return retained + "\n\n[truncated; total bytes: " + totalUtf8Bytes
                + ", retained bytes: " + retainedUtf8Bytes + "]";
    }

    synchronized long totalUtf8Bytes() {
        return totalUtf8Bytes;
    }

    synchronized boolean isTruncated() {
        return truncated;
    }

    private static CharSpan charSpan(CharSequence text, int index, int end) {
        char ch = text.charAt(index);
        if (ch <= 0x7F) {
            return new CharSpan(1, 1);
        }
        if (ch <= 0x7FF) {
            return new CharSpan(1, 2);
        }
        if (Character.isHighSurrogate(ch)
                && index + 1 < end
                && Character.isLowSurrogate(text.charAt(index + 1))) {
            return new CharSpan(2, 4);
        }
        return new CharSpan(1, 3);
    }

    private static long utf8Length(CharSequence value, int start, int end) {
        long bytes = 0;
        for (int i = start; i < end; i++) {
            char ch = value.charAt(i);
            if (ch <= 0x7F) {
                bytes += 1;
            } else if (ch <= 0x7FF) {
                bytes += 2;
            } else if (Character.isHighSurrogate(ch)
                    && i + 1 < end
                    && Character.isLowSurrogate(value.charAt(i + 1))) {
                bytes += 4;
                i++;
            } else {
                bytes += 3;
            }
        }
        return bytes;
    }

    private record CharSpan(int charCount, int utf8Bytes) {
    }
}
