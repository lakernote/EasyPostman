package com.laker.postman.http.runtime.interaction;

public interface DownloadProgressSink {
    DownloadProgressSink NOOP = new DownloadProgressSink() {
    };

    default void start(int contentLength) {
    }

    default boolean isCancelled() {
        return false;
    }

    default void updateProgress(int bytesRead) {
    }

    default void finish() {
    }
}
