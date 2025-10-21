package com.laker.postman.util;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Cron表达式工具类 - 简化版本
 * 用于解析和计算Cron表达式的下次执行时间
 */
@Slf4j
public class CronExpressionUtil {

    /**
     * 计算Cron表达式的下N次执行时间
     *
     * @param cronExpression Cron表达式
     * @param count          计算次数
     * @return 执行时间列表
     */
    public static List<Date> getNextExecutionTimes(String cronExpression, int count) {
        List<Date> times = new ArrayList<>();

        try {
            String[] parts = cronExpression.trim().split("\\s+");
            if (parts.length < 6) {
                return times;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MILLISECOND, 0);

            // 简化版本：基于当前时间向后推算
            for (int i = 0; i < count && i < 100; i++) {
                Date nextTime = calculateNext(calendar, parts);
                if (nextTime != null) {
                    times.add(nextTime);
                    calendar.setTime(nextTime);
                    calendar.add(Calendar.SECOND, 1); // 移动到下一秒
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Calculate cron execution times error", e);
        }

        return times;
    }

    /**
     * 计算下一次执行时间
     */
    private static Date calculateNext(Calendar calendar, String[] parts) {
        try {
            String second = parts[0];
            String minute = parts[1];
            String hour = parts[2];
            String day = parts[3];
            String month = parts[4];
            String week = parts[5];

            // 简化实现：处理常见的Cron模式
            Calendar next = (Calendar) calendar.clone();

            // 处理每秒、每分钟、每小时等常见模式
            if ("*".equals(second) && "*".equals(minute) && "*".equals(hour)) {
                // 每秒执行
                next.add(Calendar.SECOND, 1);
                return next.getTime();
            }

            if ("*".equals(minute) && "*".equals(hour)) {
                // 每分钟的指定秒数
                int sec = parseValue(second);
                next.set(Calendar.SECOND, sec);
                next.add(Calendar.MINUTE, 1);
                return next.getTime();
            }

            if ("*".equals(hour)) {
                // 每小时的指定分钟
                int min = parseValue(minute);
                int sec = parseValue(second);
                next.set(Calendar.SECOND, sec);
                next.set(Calendar.MINUTE, min);
                next.add(Calendar.HOUR_OF_DAY, 1);
                return next.getTime();
            }

            // 处理固定时间执行
            int sec = parseValue(second);
            int min = parseValue(minute);
            int hr = parseValue(hour);

            next.set(Calendar.SECOND, sec);
            next.set(Calendar.MINUTE, min);
            next.set(Calendar.HOUR_OF_DAY, hr);

            // 如果时间已经过了，移动到明天
            if (next.before(calendar)) {
                next.add(Calendar.DAY_OF_MONTH, 1);
            }

            return next.getTime();

        } catch (Exception e) {
            log.error("Calculate next execution time error", e);
            return null;
        }
    }

    /**
     * 解析Cron字段值
     */
    private static int parseValue(String value) {
        if ("*".equals(value) || "?".equals(value)) {
            return 0;
        }

        // 处理 */n 格式
        if (value.contains("/")) {
            String[] split = value.split("/");
            return Integer.parseInt(split[1]);
        }

        // 处理范围 n-m
        if (value.contains("-")) {
            String[] split = value.split("-");
            return Integer.parseInt(split[0]);
        }

        // 处理列表 n,m,k
        if (value.contains(",")) {
            String[] split = value.split(",");
            return Integer.parseInt(split[0]);
        }

        return Integer.parseInt(value);
    }

    /**
     * 验证Cron表达式是否有效
     */
    public static boolean isValid(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length < 6 || parts.length > 7) {
            return false;
        }

        try {
            // 简单验证每个字段
            for (String part : parts) {
                if (!isValidField(part)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证单个字段是否有效
     */
    private static boolean isValidField(String field) {
        if (field == null || field.isEmpty()) {
            return false;
        }

        // 允许的字符：数字、*、?、-、,、/、L、W、#
        return field.matches("[0-9*?\\-,/LW#A-Z]+");
    }

    /**
     * 格式化日期
     */
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * 获取Cron表达式的描述（简化版）
     */
    public static String describe(String cronExpression) {
        try {
            String[] parts = cronExpression.trim().split("\\s+");
            if (parts.length < 6) {
                return "Invalid Cron expression";
            }

            StringBuilder desc = new StringBuilder();

            // 秒
            desc.append(describeField(parts[0], "second"));
            desc.append(", ");

            // 分钟
            desc.append(describeField(parts[1], "minute"));
            desc.append(", ");

            // 小时
            desc.append(describeField(parts[2], "hour"));
            desc.append(", ");

            // 日
            if (!"?".equals(parts[3])) {
                desc.append(describeField(parts[3], "day"));
                desc.append(", ");
            }

            // 月
            if (!"*".equals(parts[4])) {
                desc.append("in ").append(describeField(parts[4], "month"));
                desc.append(", ");
            }

            // 周
            if (!"?".equals(parts[5]) && !"*".equals(parts[5])) {
                desc.append("on ").append(describeField(parts[5], "weekday"));
            }

            return desc.toString().replaceAll(", $", "");

        } catch (Exception e) {
            return "Failed to parse Cron expression";
        }
    }

    /**
     * 描述单个字段
     */
    private static String describeField(String value, String unit) {
        if ("*".equals(value)) {
            return "every " + unit;
        }
        if ("?".equals(value)) {
            return "any " + unit;
        }
        if (value.contains("/")) {
            String[] split = value.split("/");
            return "every " + split[1] + " " + unit + "s";
        }
        if (value.contains("-")) {
            return "from " + value + " " + unit;
        }
        if (value.contains(",")) {
            return "at " + value + " " + unit;
        }
        return "at " + unit + " " + value;
    }
}

