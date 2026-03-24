package com.laker.postman.startup;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class StartupDiagnostics {
    private static final long APP_START_NANOS = System.nanoTime();

    public static void mark(String message) {
        log.info("[startup +{} ms] {}", elapsedMillis(), message);
    }

    public static long elapsedMillis() {
        return (System.nanoTime() - APP_START_NANOS) / 1_000_000L;
    }

    public static String formatSince(long startNanos) {
        return ((System.nanoTime() - startNanos) / 1_000_000L) + " ms";
    }
}
