package com.laker.postman.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Cron表达式工具类
 * 用于解析和计算Cron表达式的下次执行时间
 * 支持:
 * - Spring/Quartz: 6位/7位 (Second Minute Hour Day Month Week [Year])
 * - Linux Crontab: 5位 (Minute Hour Day Month Week)
 */
@Slf4j
@UtilityClass
public class CronExpressionUtil {

    public enum CronMode {
        SPRING_QUARTZ, // 6/7 fields: Second Minute Hour Day Month Week [Year]
        LINUX_CRONTAB   // 5 fields:  Minute Hour Day Month Week
    }

    /**
     * 计算Cron表达式的下N次执行时间（自动检测模式）
     */
    public static List<Date> getNextExecutionTimes(String cronExpression, int count) {
        CronMode mode = detectMode(cronExpression);
        return getNextExecutionTimes(cronExpression, count, mode);
    }

    /**
     * 计算Cron表达式的下N次执行时间
     *
     * @param cronExpression Cron表达式
     * @param count          计算次数
     * @param mode           Cron模式
     * @return 执行时间列表
     */
    public static List<Date> getNextExecutionTimes(String cronExpression, int count, CronMode mode) {
        List<Date> times = new ArrayList<>();

        try {
            String normalized = normalizeCron(cronExpression, mode);
            String[] parts = normalized.trim().split("\\s+");
            if (parts.length < 6) {
                return times;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MILLISECOND, 0);
            // For Linux crontab, align to next minute boundary
            if (mode == CronMode.LINUX_CRONTAB) {
                calendar.set(Calendar.SECOND, 0);
                calendar.add(Calendar.MINUTE, 1);
            } else {
                calendar.add(Calendar.SECOND, 1);
            }

            int attempts = 0;
            int maxAttempts = 10000;

            while (times.size() < count && attempts < maxAttempts) {
                attempts++;
                Date nextTime = findNextExecution(calendar, parts, mode);
                if (nextTime != null) {
                    times.add(nextTime);
                    calendar.setTime(nextTime);
                    if (mode == CronMode.LINUX_CRONTAB) {
                        calendar.add(Calendar.MINUTE, 1);
                    } else {
                        calendar.add(Calendar.SECOND, 1);
                    }
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
     * 自动检测Cron模式
     */
    public static CronMode detectMode(String cronExpression) {
        if (cronExpression == null) return CronMode.SPRING_QUARTZ;
        String[] parts = cronExpression.trim().split("\\s+");
        return parts.length == 5 ? CronMode.LINUX_CRONTAB : CronMode.SPRING_QUARTZ;
    }

    /**
     * 将Linux 5位crontab转为内部统一的6位格式（补0秒）
     */
    public static String normalizeCron(String cronExpression, CronMode mode) {
        if (mode == CronMode.LINUX_CRONTAB) {
            String[] parts = cronExpression.trim().split("\\s+");
            if (parts.length == 5) {
                // Minute Hour Day Month Week -> 0 Minute Hour Day Month Week
                return "0 " + cronExpression.trim();
            }
        }
        return cronExpression;
    }

    /**
     * 验证Cron表达式是否有效（自动检测模式）
     */
    public static boolean isValid(String cronExpression) {
        return isValid(cronExpression, detectMode(cronExpression));
    }

    /**
     * 验证Cron表达式是否有效
     */
    public static boolean isValid(String cronExpression, CronMode mode) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        String[] parts = cronExpression.trim().split("\\s+");

        if (mode == CronMode.LINUX_CRONTAB) {
            if (parts.length != 5) return false;
        } else {
            if (parts.length < 6 || parts.length > 7) return false;
        }

        try {
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
     * 获取Cron表达式的描述（自动检测模式）
     */
    public static String describe(String cronExpression) {
        return describe(cronExpression, detectMode(cronExpression));
    }

    /**
     * 获取Cron表达式的描述
     */
    public static String describe(String cronExpression, CronMode mode) {
        try {
            String normalized = normalizeCron(cronExpression, mode);
            String[] parts = normalized.trim().split("\\s+");
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

            desc.append("Executes ");

            if (mode == CronMode.LINUX_CRONTAB) {
                // Linux crontab: minute granularity
                if ("*".equals(minuteExpr) && "*".equals(hourExpr)) {
                    desc.append("every minute");
                } else if ("*".equals(hourExpr)) {
                    desc.append("every hour at minute ").append(minuteExpr);
                } else {
                    desc.append("at ").append(describeTime(hourExpr, minuteExpr, "00"));
                }
            } else {
                if ("*".equals(secondExpr) && "*".equals(minuteExpr) && "*".equals(hourExpr)) {
                    desc.append("every second");
                } else if ("*".equals(minuteExpr) && "*".equals(hourExpr)) {
                    desc.append("every minute at second ").append(secondExpr);
                } else if ("*".equals(hourExpr)) {
                    desc.append("every hour at ").append(describeTime(minuteExpr, secondExpr));
                } else {
                    desc.append("at ").append(describeTime(hourExpr, minuteExpr, secondExpr));
                }
            }

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
     * 查找下一次执行时间（模式感知版本）
     */
    private static Date findNextExecution(Calendar from, String[] parts, CronMode mode) {
        try {
            String secondExpr = parts[0];
            String minuteExpr = parts[1];
            String hourExpr = parts[2];
            String dayExpr = parts[3];
            String monthExpr = parts[4];
            String weekExpr = parts[5];
            String yearExpr = parts.length > 6 ? parts[6] : "*";

            Calendar calendar = (Calendar) from.clone();

            // Compute look-ahead window: for specific year expressions extend as needed
            Calendar maxTime = (Calendar) calendar.clone();
            int lookaheadYears = 2;
            if (parts.length > 6 && !"*".equals(parts[6])) {
                try {
                    int targetYear = Integer.parseInt(parts[6].trim());
                    lookaheadYears = Math.max(2, targetYear - calendar.get(Calendar.YEAR) + 1);
                } catch (NumberFormatException ignored) {
                }
            }
            maxTime.add(Calendar.YEAR, lookaheadYears);

            int maxIterations = 366 * 24 * 60 * 60;
            for (int i = 0; i < maxIterations; i++) {
                if (calendar.after(maxTime)) {
                    return null;
                }

                if (matchesAll(calendar, secondExpr, minuteExpr, hourExpr,
                        dayExpr, monthExpr, weekExpr, yearExpr, mode)) {
                    return calendar.getTime();
                }

                if (!matchesField(calendar.get(Calendar.MINUTE), minuteExpr, 0, 59)) {
                    calendar.set(Calendar.SECOND, 0);
                    calendar.add(Calendar.MINUTE, 1);
                } else if (!matchesField(calendar.get(Calendar.HOUR_OF_DAY), hourExpr, 0, 23)) {
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                } else if (!matchesDayAll(calendar, dayExpr, monthExpr, weekExpr, mode)) {
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                } else {
                    calendar.add(Calendar.SECOND, 1);
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Find next execution error", e);
            return null;
        }
    }

    private static boolean matchesAll(Calendar calendar, String secondExpr, String minuteExpr,
                                      String hourExpr, String dayExpr, String monthExpr,
                                      String weekExpr, String yearExpr, CronMode mode) {
        return matchesField(calendar.get(Calendar.SECOND), secondExpr, 0, 59)
                && matchesField(calendar.get(Calendar.MINUTE), minuteExpr, 0, 59)
                && matchesField(calendar.get(Calendar.HOUR_OF_DAY), hourExpr, 0, 23)
                && matchesDayAll(calendar, dayExpr, monthExpr, weekExpr, mode)
                && matchesField(calendar.get(Calendar.MONTH) + 1, monthExpr, 1, 12)
                && matchesYearField(calendar.get(Calendar.YEAR), yearExpr);
    }

    private static boolean matchesDayAll(Calendar calendar, String dayExpr,
                                         String monthExpr, String weekExpr, CronMode mode) {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int week = calendar.get(Calendar.DAY_OF_WEEK);

        boolean dayMatch;
        boolean weekMatch;

        if ("?".equals(dayExpr)) {
            dayMatch = true;
        } else if ("L".equals(dayExpr)) {
            dayMatch = (day == calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else if (dayExpr.endsWith("W")) {
            dayMatch = matchesField(day, dayExpr.replace("W", ""), 1, 31);
        } else {
            dayMatch = matchesField(day, dayExpr, 1, 31);
        }

        if ("?".equals(weekExpr) || "*".equals(weekExpr)) {
            weekMatch = true; // ? = don't care, * = every day
        } else {
            weekMatch = matchesWeekField(week, weekExpr, mode);
        }

        // If both day-of-month and day-of-week are specified (neither ? nor *),
        // use OR semantics (standard cron behaviour)
        if (!"?".equals(dayExpr) && !"*".equals(dayExpr)
                && !"?".equals(weekExpr) && !"*".equals(weekExpr)) {
            return (dayMatch || weekMatch) && matchesField(month, monthExpr, 1, 12);
        }
        return dayMatch && weekMatch && matchesField(month, monthExpr, 1, 12);
    }

    /**
     * Match weekday field.
     * <p>
     * Calendar: SUNDAY=1, MONDAY=2 … SATURDAY=7
     * <p>
     * Spring/Quartz: 1=SUN .. 7=SAT  (same as Calendar — no shift)
     * Linux crontab: 0=SUN, 1=MON .. 6=SAT, 7=SUN alias (+1 shift needed)
     */
    private static boolean matchesWeekField(int week, String weekExpr, CronMode mode) {
        // Replace named days with Spring/Quartz values (Calendar-compatible 1-7)
        String expr = weekExpr.toUpperCase()
                .replace("SUN", "1").replace("MON", "2").replace("TUE", "3")
                .replace("WED", "4").replace("THU", "5").replace("FRI", "6")
                .replace("SAT", "7");

        boolean hadNames = !expr.equals(weekExpr.toUpperCase());

        if (hadNames || mode == CronMode.SPRING_QUARTZ) {
            // Named days or Spring numeric (1=Sun..7=Sat) — Calendar-compatible, no shift
            return matchesField(week, expr, 1, 7);
        }

        // Linux crontab: 0-based (0=Sun,1=Mon…6=Sat,7=Sun alias) → shift each digit +1
        StringBuilder shifted = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c)) {
                int v = Character.getNumericValue(c);
                shifted.append(v == 7 ? 1 : v + 1); // 7→1(Sun), 0→1(Sun), 1→2(Mon)…
            } else {
                shifted.append(c);
            }
        }
        return matchesField(week, shifted.toString(), 1, 7);
    }


    private static boolean matchesField(int value, String expr, int min, int max) {
        if ("*".equals(expr) || "?".equals(expr)) return true;

        if (expr.contains(",")) {
            for (String part : expr.split(",")) {
                if (matchesField(value, part.trim(), min, max)) return true;
            }
            return false;
        }

        if (expr.contains("/")) {
            String[] p = expr.split("/");
            int step = Integer.parseInt(p[1]);
            if (p[0].contains("-")) {
                String[] r = p[0].split("-");
                int s = Integer.parseInt(r[0]), e = Integer.parseInt(r[1]);
                return value >= s && value <= e && (value - s) % step == 0;
            }
            int start = "*".equals(p[0]) ? min : Integer.parseInt(p[0]);
            return value >= start && (value - start) % step == 0;
        }

        if (expr.contains("-")) {
            String[] p = expr.split("-");
            return value >= Integer.parseInt(p[0]) && value <= Integer.parseInt(p[1]);
        }

        try {
            return value == Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean matchesYearField(int year, String yearExpr) {
        if (yearExpr == null || yearExpr.isEmpty() || "*".equals(yearExpr)) return true;
        return matchesField(year, yearExpr, 1970, 2099);
    }

    private static boolean isValidField(String field) {
        if (field == null || field.isEmpty()) return false;
        return field.matches("[0-9*?/,\\-LW#A-Za-z]+");
    }

    /**
     * 格式化日期
     */
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E", Locale.getDefault());
        return sdf.format(date);
    }

    private static String describeTime(String hour, String minute, String second) {
        return String.format("%s:%s:%s",
                describeValue(hour),
                describeValuePadded(minute),
                describeValuePadded(second));
    }

    private static String describeTime(String minute, String second) {
        return String.format("%s:%s",
                describeValue(minute),
                describeValuePadded(second));
    }

    private static String describeValue(String value) {
        if ("*".equals(value)) return "every";
        if (value.contains("/")) {
            String[] parts = value.split("/");
            return "every " + parts[1];
        }
        if (value.contains("-")) return value;
        if (value.contains(",")) return value;
        return value;
    }

    /** Like describeValue but zero-pads plain numeric values (e.g. "0" → "00", "5" → "05"). */
    private static String describeValuePadded(String value) {
        String v = describeValue(value);
        try {
            int n = Integer.parseInt(v);
            return String.format("%02d", n);
        } catch (NumberFormatException e) {
            return v;
        }
    }

    private static String describeWeek(String weekExpr) {
        // Use word-level replacement to avoid "1" corrupting "10","11","12" etc.
        String[] names = {"", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        String expr = weekExpr.toUpperCase()
                .replace("SUN", "Sun").replace("MON", "Mon").replace("TUE", "Tue")
                .replace("WED", "Wed").replace("THU", "Thu").replace("FRI", "Fri")
                .replace("SAT", "Sat");
        // Replace remaining digits with day names
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isDigit(c)) {
                int v = c - '0';
                if (v >= 1 && v <= 7) {
                    sb.append(names[v]);
                } else if (v == 0) {
                    sb.append("Sun"); // Linux 0=Sunday
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        String result = sb.toString();
        if (result.contains("Mon-Fri")) return "weekdays (Mon-Fri)";
        return result;
    }

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
            // ignore
        }

        return monthExpr;
    }
}
