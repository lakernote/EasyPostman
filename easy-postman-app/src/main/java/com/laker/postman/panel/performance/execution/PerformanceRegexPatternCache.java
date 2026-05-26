package com.laker.postman.panel.performance.execution;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@UtilityClass
final class PerformanceRegexPatternCache {
    private static final int MAX_CACHE_SIZE = 256;
    private static final ConcurrentMap<String, Pattern> CACHE = new ConcurrentHashMap<>();

    Pattern compileDotAll(String expression) {
        if (CACHE.size() >= MAX_CACHE_SIZE) {
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(expression, key -> Pattern.compile(key, Pattern.DOTALL));
    }
}
