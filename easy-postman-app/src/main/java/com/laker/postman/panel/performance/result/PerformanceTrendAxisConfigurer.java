package com.laker.postman.panel.performance.result;

import lombok.experimental.UtilityClass;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;

@UtilityClass
class PerformanceTrendAxisConfigurer {

    private static final String WALL_CLOCK_TIME_PATTERN = "HH:mm:ss";

    void configureIntegerAxis(NumberAxis axis) {
        if (axis == null) {
            return;
        }
        // 整数指标必须使用整数刻度，不能只把小数刻度格式化成整数，否则 0.5/1.5 会显示成重复的 0/2。
        axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        axis.setNumberFormatOverride(NumberFormat.getIntegerInstance());
    }

    void configureTimeAxis(DateAxis axis, long visibleDurationMs) {
        if (axis == null) {
            return;
        }
        // 趋势图展示墙钟时间，避免短压测时相对秒数被 JFreeChart 子秒 tick 格式化成重复标签。
        axis.setDateFormatOverride(new SimpleDateFormat(WALL_CLOCK_TIME_PATTERN));
        axis.setTickUnit(selectTimeTickUnit(visibleDurationMs), false, true);
    }

    DateTickUnit selectTimeTickUnit(long visibleDurationMs) {
        long durationMs = Math.max(1_000L, visibleDurationMs);
        if (durationMs <= 20_000L) {
            return new DateTickUnit(DateTickUnitType.SECOND, 1);
        }
        if (durationMs <= 60_000L) {
            return new DateTickUnit(DateTickUnitType.SECOND, 5);
        }
        if (durationMs <= 3 * 60_000L) {
            return new DateTickUnit(DateTickUnitType.SECOND, 15);
        }
        if (durationMs <= 10 * 60_000L) {
            return new DateTickUnit(DateTickUnitType.SECOND, 30);
        }
        if (durationMs <= 60 * 60_000L) {
            return new DateTickUnit(DateTickUnitType.MINUTE, 1);
        }
        return new DateTickUnit(DateTickUnitType.MINUTE, 5);
    }
}
