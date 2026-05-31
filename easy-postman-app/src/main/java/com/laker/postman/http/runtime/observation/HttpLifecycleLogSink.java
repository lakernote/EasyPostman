package com.laker.postman.http.runtime.observation;

@FunctionalInterface
public interface HttpLifecycleLogSink {
    HttpLifecycleLogSink NOOP = (message, level) -> {
    };

    void append(String message, Level level);

    static HttpLifecycleLogSink noop() {
        return NOOP;
    }

    enum Level {
        DEBUG,
        INFO,
        SUCCESS,
        WARN,
        ERROR
    }
}
