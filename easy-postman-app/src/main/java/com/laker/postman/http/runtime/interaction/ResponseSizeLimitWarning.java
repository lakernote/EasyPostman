package com.laker.postman.http.runtime.interaction;

public record ResponseSizeLimitWarning(Kind kind, int contentLengthBytes, int maxDownloadBytes) {
    public enum Kind {
        TEXT,
        BINARY
    }

    public int contentLengthMegabytes() {
        return contentLengthBytes / 1024 / 1024;
    }

    public int maxDownloadMegabytes() {
        return maxDownloadBytes / 1024 / 1024;
    }
}
