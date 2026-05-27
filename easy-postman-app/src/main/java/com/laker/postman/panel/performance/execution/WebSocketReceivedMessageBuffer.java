package com.laker.postman.panel.performance.execution;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

final class WebSocketReceivedMessageBuffer {
    static final int DEFAULT_MAX_RETAINED_AWAIT_MESSAGES = 1024;

    private final int maxRetainedBytes;
    private final int maxRetainedMessages;
    private final boolean retainPayload;
    private final Deque<Message> messages = new ArrayDeque<>();
    private final AtomicLong retainedBytes = new AtomicLong(0);

    WebSocketReceivedMessageBuffer(int maxRetainedBytes) {
        this(maxRetainedBytes, true);
    }

    WebSocketReceivedMessageBuffer(int maxRetainedBytes, boolean retainPayload) {
        this(maxRetainedBytes, retainPayload, DEFAULT_MAX_RETAINED_AWAIT_MESSAGES);
    }

    WebSocketReceivedMessageBuffer(int maxRetainedBytes, boolean retainPayload, int maxRetainedMessages) {
        this.maxRetainedBytes = Math.max(1, maxRetainedBytes);
        this.maxRetainedMessages = Math.max(1, maxRetainedMessages);
        this.retainPayload = retainPayload;
    }

    void add(String payload, long receivedAtMs) {
        String retainedPayload = retainPayload ? retainUtf8Prefix(payload, maxRetainedBytes) : "";
        int messageBytes = retainPayload ? utf8Length(retainedPayload) : 0;
        while (!messages.isEmpty()
                && (messages.size() >= maxRetainedMessages
                || retainedBytes.get() + messageBytes > maxRetainedBytes)) {
            Message removed = messages.removeFirst();
            retainedBytes.addAndGet(-removed.retainedUtf8Bytes());
        }
        messages.addLast(new Message(retainedPayload, receivedAtMs, messageBytes));
        retainedBytes.addAndGet(messageBytes);
    }

    boolean isEmpty() {
        return messages.isEmpty();
    }

    Message removeFirst() {
        Message message = messages.removeFirst();
        retainedBytes.addAndGet(-message.retainedUtf8Bytes());
        return message;
    }

    static String retainUtf8Prefix(String value, int maxUtf8Bytes) {
        if (value == null || value.isEmpty() || maxUtf8Bytes <= 0) {
            return "";
        }
        int bytes = 0;
        int index = 0;
        while (index < value.length()) {
            CharSpan span = charSpan(value, index, value.length());
            if (bytes + span.utf8Bytes > maxUtf8Bytes) {
                break;
            }
            bytes += span.utf8Bytes;
            index += span.charCount;
        }
        return value.substring(0, index);
    }

    private static int utf8Length(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int bytes = 0;
        for (int i = 0; i < value.length(); ) {
            CharSpan span = charSpan(value, i, value.length());
            bytes += span.utf8Bytes;
            i += span.charCount;
        }
        return bytes;
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

    record Message(String payload, long receivedAtMs, int retainedUtf8Bytes) {
    }

    private record CharSpan(int charCount, int utf8Bytes) {
    }
}
