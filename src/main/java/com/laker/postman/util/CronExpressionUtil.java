package com.laker.postman.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Cron表达式工具类
 * 用于解析和计算Cron表达式的下次执行时间
 * 支持标准6位和7位Cron表达式：Second Minute Hour Day Month Week [Year]
 */
@Slf4j
@UtilityClass
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

            // 向前移动1秒，确保第一次计算是未来时间
            calendar.add(Calendar.SECOND, 1);

            // 最多尝试计算1000次，避免死循环
            int attempts = 0;
            int maxAttempts = 10000;

            while (times.size() < count && attempts < maxAttempts) {
                attempts++;
                Date nextTime = findNextExecution(calendar, parts);
                if (nextTime != null) {
                    times.add(nextTime);
                    calendar.setTime(nextTime);
                    calendar.add(Calendar.SECOND, 1); // 移动到下一秒，查找下次执行
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
     * 查找下一次执行时间
     */
    private static Date findNextExecution(Calendar from, String[] parts) {
        try {
            String secondExpr = parts[0];
            String minuteExpr = parts[1];
            String hourExpr = parts[2];
            String dayExpr = parts[3];
            String monthExpr = parts[4];
            String weekExpr = parts[5];
            String yearExpr = parts.length > 6 ? parts[6] : "*";

            Calendar calendar = (Calendar) from.clone();

            // 最多向前查找2年
            Calendar maxTime = (Calendar) calendar.clone();
            maxTime.add(Calendar.YEAR, 2);

            // 逐秒递增，找到第一个匹配的时间点
            int maxIterations = 366 * 24 * 60 * 60; // 最多一年的秒数
            for (int i = 0; i < maxIterations; i++) {
                if (calendar.after(maxTime)) {
                    return null; // 超过最大时间范围
                }

                if (matches(calendar, secondExpr, minuteExpr, hourExpr, dayExpr, monthExpr, weekExpr, yearExpr)) {
                    return calendar.getTime();
                }

                // 优化：根据不匹配的字段跳过
                if (!matchesMinute(calendar, minuteExpr)) {
                    // 分钟不匹配，跳到下一分钟
                    calendar.set(Calendar.SECOND, 0);
                    calendar.add(Calendar.MINUTE, 1);
                } else if (!matchesHour(calendar, hourExpr)) {
                    // 小时不匹配，跳到下一小时
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                } else if (!matchesDay(calendar, dayExpr, monthExpr, weekExpr)) {
                    // 日期不匹配，跳到第二天
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                } else {
                    // 秒不匹配，向前一秒
                    calendar.add(Calendar.SECOND, 1);
                }
            }

            return null; // 未找到匹配时间
        } catch (Exception e) {
            log.error("Find next execution error", e);
            return null;
        }
    }

    /**
     * 检查时间是否匹配Cron表达式
     */
    private static boolean matches(Calendar calendar, String secondExpr, String minuteExpr,
                                   String hourExpr, String dayExpr, String monthExpr,
                                   String weekExpr, String yearExpr) {
        int second = calendar.get(Calendar.SECOND);
        int minute = calendar.get(Calendar.MINUTE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1; // Calendar的月份从0开始
        int week = calendar.get(Calendar.DAY_OF_WEEK); // 1=Sunday, 7=Saturday
        int year = calendar.get(Calendar.YEAR);

        return matchesField(second, secondExpr, 0, 59)
                && matchesField(minute, minuteExpr, 0, 59)
                && matchesField(hour, hourExpr, 0, 23)
                && matchesDayField(calendar, day, dayExpr, month, week, weekExpr)
                && matchesField(month, monthExpr, 1, 12)
                && matchesYearField(year, yearExpr);
    }

    /**
     * 检查分钟是否匹配
     */
    private static boolean matchesMinute(Calendar calendar, String minuteExpr) {
        return matchesField(calendar.get(Calendar.MINUTE), minuteExpr, 0, 59);
    }

    /**
     * 检查小时是否匹配
     */
    private static boolean matchesHour(Calendar calendar, String hourExpr) {
        return matchesField(calendar.get(Calendar.HOUR_OF_DAY), hourExpr, 0, 23);
    }

    /**
     * 检查日期是否匹配
     */
    private static boolean matchesDay(Calendar calendar, String dayExpr, String monthExpr, String weekExpr) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int week = calendar.get(Calendar.DAY_OF_WEEK);
        return matchesDayField(calendar, day, dayExpr, month, week, weekExpr)
                && matchesField(month, monthExpr, 1, 12);
    }

    /**
     * 检查字段值是否匹配表达式
     */
    private static boolean matchesField(int value, String expr, int min, int max) {
        if ("*".equals(expr) || "?".equals(expr)) {
            return true;
        }

        // 处理列表：1,3,5
        if (expr.contains(",")) {
            for (String part : expr.split(",")) {
                if (matchesField(value, part.trim(), min, max)) {
                    return true;
                }
            }
            return false;
        }

        // 处理步长：*/5 或 0/5 或 8-18/2
        if (expr.contains("/")) {
            String[] parts = expr.split("/");
            int step = Integer.parseInt(parts[1]);

            // 处理范围+步长：8-18/2
            if (parts[0].contains("-")) {
                String[] rangeParts = parts[0].split("-");
                int rangeStart = Integer.parseInt(rangeParts[0]);
                int rangeEnd = Integer.parseInt(rangeParts[1]);

                // 检查值是否在范围内，且符合步长
                if (value >= rangeStart && value <= rangeEnd && (value - rangeStart) % step == 0) {
                    return true;
                }
                return false;
            }

            // 处理普通步长：*/5 或 0/5
            int start = "*".equals(parts[0]) ? min : Integer.parseInt(parts[0]);
            return value >= start && (value - start) % step == 0;
        }

        // 处理范围：1-5
        if (expr.contains("-")) {
            String[] parts = expr.split("-");
            int rangeStart = Integer.parseInt(parts[0]);
            int rangeEnd = Integer.parseInt(parts[1]);
            return value >= rangeStart && value <= rangeEnd;
        }

        // 处理单个值
        try {
            return value == Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 检查日期字段（处理Day和Week的互斥关系）
     */
    private static boolean matchesDayField(Calendar calendar, int day, String dayExpr,
                                          int month, int week, String weekExpr) {
        boolean dayMatch = false;
        boolean weekMatch = false;

        // Day字段
        if ("?".equals(dayExpr)) {
            dayMatch = true; // ? 表示不指定
        } else if ("L".equals(dayExpr)) {
            // L表示最后一天
            int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            dayMatch = (day == lastDay);
        } else if (dayExpr.endsWith("W")) {
            // W表示工作日，这里简化处理
            dayMatch = matchesField(day, dayExpr.replace("W", ""), 1, 31);
        } else if (!"*".equals(dayExpr)) {
            dayMatch = matchesField(day, dayExpr, 1, 31);
        } else {
            dayMatch = true;
        }

        // Week字段（1=Sunday, 2=Monday, ..., 7=Saturday）
        if ("?".equals(weekExpr)) {
            weekMatch = true; // ? 表示不指定
        } else if (!"*".equals(weekExpr)) {
            // 转换星期表达式（支持MON-SUN或1-7）
            weekMatch = matchesWeekField(week, weekExpr);
        } else {
            weekMatch = true;
        }

        // Day和Week至少有一个匹配（如果都指定了值）
        if (!"?".equals(dayExpr) && !"*".equals(dayExpr) && !"?".equals(weekExpr) && !"*".equals(weekExpr)) {
            return dayMatch || weekMatch;
        } else {
            return dayMatch && weekMatch;
        }
    }

    /**
     * 检查星期字段
     */
    private static boolean matchesWeekField(int week, String weekExpr) {
        // 转换星期名称
        String expr = weekExpr.toUpperCase()
                .replace("SUN", "1")
                .replace("MON", "2")
                .replace("TUE", "3")
                .replace("WED", "4")
                .replace("THU", "5")
                .replace("FRI", "6")
                .replace("SAT", "7");

        return matchesField(week, expr, 1, 7);
    }

    /**
     * 检查年份字段
     */
    private static boolean matchesYearField(int year, String yearExpr) {
        if ("*".equals(yearExpr) || yearExpr == null || yearExpr.isEmpty()) {
            return true;
        }
        return matchesField(year, yearExpr, 1970, 2099);
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * 获取Cron表达式的描述
     */
    public static String describe(String cronExpression) {
        try {
            String[] parts = cronExpression.trim().split("\\s+");
            if (parts.length < 6) {
                return "Invalid Cron expression";
            }

            StringBuilder desc = new StringBuilder();

            String secondExpr = parts[0];
            String minuteExpr = parts[1];
            String hourExpr = parts[2];
            String dayExpr = parts[3];
            String monthExpr = parts[4];
            String weekExpr = parts[5];

            // 构建可读描述
            desc.append("Executes ");

            // 处理时间部分
            if ("*".equals(secondExpr) && "*".equals(minuteExpr) && "*".equals(hourExpr)) {
                desc.append("every second");
            } else if ("*".equals(minuteExpr) && "*".equals(hourExpr)) {
                desc.append("every minute at second ").append(secondExpr);
            } else if ("*".equals(hourExpr)) {
                desc.append("every hour at ").append(describeTime(minuteExpr, secondExpr));
            } else {
                desc.append("at ").append(describeTime(hourExpr, minuteExpr, secondExpr));
            }

            // 处理日期部分
            if (!"*".equals(dayExpr) && !"?".equals(dayExpr)) {
                desc.append(" on day ").append(dayExpr);
            }

            if (!"*".equals(weekExpr) && !"?".equals(weekExpr)) {
                desc.append(" on ").append(describeWeek(weekExpr));
            }

            if (!"*".equals(monthExpr)) {
                desc.append(" in ").append(describeMonth(monthExpr));
            }

            if (parts.length > 6 && !"*".equals(parts[6])) {
                desc.append(" in year ").append(parts[6]);
            }

            return desc.toString();

        } catch (Exception e) {
            log.error("Describe cron error", e);
            return "Failed to parse Cron expression";
        }
    }

    /**
     * 描述时间
     */
    private static String describeTime(String hour, String minute, String second) {
        return String.format("%s:%s:%s",
                describeValue(hour),
                describeValue(minute),
                describeValue(second));
    }

    /**
     * 描述时间（时:分）
     */
    private static String describeTime(String minute, String second) {
        return String.format("%s:%s",
                describeValue(minute),
                describeValue(second));
    }

    /**
     * 描述值
     */
    private static String describeValue(String value) {
        if ("*".equals(value)) {
            return "every";
        }
        if (value.contains("/")) {
            String[] parts = value.split("/");
            return "every " + parts[1];
        }
        if (value.contains("-")) {
            return value;
        }
        if (value.contains(",")) {
            return value;
        }
        return value;
    }

    /**
     * 描述星期
     */
    private static String describeWeek(String weekExpr) {
        String desc = weekExpr.toUpperCase()
                .replace("1", "Sunday")
                .replace("2", "Monday")
                .replace("3", "Tuesday")
                .replace("4", "Wednesday")
                .replace("5", "Thursday")
                .replace("6", "Friday")
                .replace("7", "Saturday");

        if (desc.contains("MONDAY-FRIDAY")) {
            return "weekdays";
        }

        return desc;
    }

    /**
     * 描述月份
     */
    private static String describeMonth(String monthExpr) {
        if (monthExpr.contains("/")) {
            String[] parts = monthExpr.split("/");
            return "every " + parts[1] + " months";
        }

        String[] months = {"", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        try {
            int month = Integer.parseInt(monthExpr);
            if (month >= 1 && month <= 12) {
                return months[month];
            }
        } catch (NumberFormatException e) {
            // 忽略
        }

        return monthExpr;
    }
}
