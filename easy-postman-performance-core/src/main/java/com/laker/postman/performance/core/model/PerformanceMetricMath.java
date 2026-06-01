package com.laker.postman.performance.core.model;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
final class PerformanceMetricMath {

    double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    double rate(long amount, long elapsedMs) {
        double seconds = Math.max(0.001, elapsedMs / 1000.0);
        return round(Math.max(0L, amount) / seconds);
    }

    double rate(long amount, double seconds) {
        return seconds > 0 ? round(Math.max(0L, amount) / seconds) : 0;
    }

    int clampToInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0, value);
    }
}
