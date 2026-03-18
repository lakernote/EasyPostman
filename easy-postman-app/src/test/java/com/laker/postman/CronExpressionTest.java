package com.laker.postman;

import com.laker.postman.util.CronExpressionUtil;
import com.laker.postman.util.CronExpressionUtil.CronMode;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Cron表达式工具测试 - 覆盖 Spring/Quartz (6/7位) 和 Linux Crontab (5位) 两种模式
 */
public class CronExpressionTest {


    /**
     * Spring/Quartz 有效表达式 (6/7位: 秒 分 时 日 月 周 [年])
     */
    @DataProvider(name = "validSpringExpressions")
    public Object[][] validSpringExpressions() {
        return new Object[][]{
                // expr,                 description,          expectedHour, expectedMinute, expectedSecond
                {"0 0 12 * * ?", "每天中午12点", 12, 0, 0},
                {"0 */5 * * * ?", "每5分钟整秒", -1, -1, 0},
                {"0 0 */2 * * ?", "每2小时整点", -1, 0, 0},
                {"0 0 9 ? * MON-FRI", "工作日09:00", 9, 0, 0},
                {"0 0 0 1 * ?", "每月1日零点", 0, 0, 0},
                {"0 15 10 * * ?", "每天10:15", 10, 15, 0},
                {"0 0/30 * * * ?", "每30分钟", -1, -1, 0},
                {"0 0 0 * * ?", "每天零点", 0, 0, 0},
                {"*/10 * * * * ?", "每10秒", -1, -1, -1},
                {"0 0 8-18/2 * * ?", "8-18点每2小时", -1, 0, 0},
                {"0 0 9 ? * 2", "每周一09:00", 9, 0, 0},
                {"0 0 0 L * ?", "每月最后一天零点", 0, 0, 0},
        };
    }

    /**
     * Linux Crontab 有效表达式 (5位: 分 时 日 月 周)
     */
    @DataProvider(name = "validLinuxExpressions")
    public Object[][] validLinuxExpressions() {
        return new Object[][]{
                // expr,            description,       expectedHour, expectedMinute
                {"* * * * *", "每分钟", -1, -1},
                {"*/5 * * * *", "每5分钟", -1, -1},
                {"0 * * * *", "每小时整点", -1, 0},
                {"0 12 * * *", "每天12:00", 12, 0},
                {"0 0 * * *", "每天午夜", 0, 0},
                {"0 9 * * 1-5", "工作日09:00", 9, 0},
                {"0 9 * * 1", "每周一09:00", 9, 0},
                {"0 0 1 * *", "每月1日零点", 0, 0},
                {"30 8 * * 0", "每周日08:30", 8, 30},
                {"0 */2 * * *", "每2小时整点", -1, 0},
        };
    }

    /**
     * 无效 Spring/Quartz 表达式
     */
    @DataProvider(name = "invalidSpringExpressions")
    public Object[][] invalidSpringExpressions() {
        return new Object[][]{
                {""},
                {"* * * *"},           // 4字段不够
                {"* * * * *"},         // 5字段(Linux格式)对Spring无效
                {"* * * * * * * *"},   // 8字段太多
                {"invalid"},
        };
    }

    /**
     * 无效 Linux Crontab 表达式
     */
    @DataProvider(name = "invalidLinuxExpressions")
    public Object[][] invalidLinuxExpressions() {
        return new Object[][]{
                {""},
                {"* * * *"},           // 4字段不够
                {"* * * * * *"},       // 6字段(Spring格式)对Linux无效
                {"invalid cron"},
        };
    }

    /**
     * 模式自动检测
     */
    @DataProvider(name = "modeDetection")
    public Object[][] modeDetection() {
        return new Object[][]{
                {"0 0 12 * * ?", CronMode.SPRING_QUARTZ},
                {"0 0 12 * * ? 2026", CronMode.SPRING_QUARTZ},
                {"* * * * * ?", CronMode.SPRING_QUARTZ},
                {"0 12 * * *", CronMode.LINUX_CRONTAB},
                {"*/5 * * * *", CronMode.LINUX_CRONTAB},
                {"0 9 * * 1-5", CronMode.LINUX_CRONTAB},
        };
    }

    // =========================================================
    // Spring/Quartz 有效表达式
    // =========================================================

    @Test(dataProvider = "validSpringExpressions")
    public void testSpringExpressionValid(String expr, String description,
                                          int expectedHour, int expectedMinute, int expectedSecond) {
        System.out.println("\n[Spring] " + description + "  =>  " + expr);
        assertTrue(CronExpressionUtil.isValid(expr, CronMode.SPRING_QUARTZ),
                "Should be valid Spring/Quartz: " + expr);

        String desc = CronExpressionUtil.describe(expr, CronMode.SPRING_QUARTZ);
        assertNotNull(desc);
        assertFalse(desc.isEmpty());
        System.out.println("  describe: " + desc);

        List<Date> times = CronExpressionUtil.getNextExecutionTimes(expr, 5, CronMode.SPRING_QUARTZ);
        assertNotNull(times);
        assertFalse(times.isEmpty(), "Should produce execution times: " + expr);

        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        for (int i = 0; i < times.size(); i++) {
            Date t = times.get(i);
            assertTrue(t.after(now), "Time must be in the future");
            if (i > 0) assertTrue(t.after(times.get(i - 1)), "Times must be ascending");
            cal.setTime(t);
            if (expectedHour >= 0 && !expr.contains("8-18/2")) {
                assertEquals(cal.get(Calendar.HOUR_OF_DAY), expectedHour, "Hour: " + expr);
            }
            if (expectedMinute >= 0) {
                assertEquals(cal.get(Calendar.MINUTE), expectedMinute, "Minute: " + expr);
            }
            if (expectedSecond >= 0 && !expr.startsWith("*/")) {
                assertEquals(cal.get(Calendar.SECOND), expectedSecond, "Second: " + expr);
            }
            System.out.println("  #" + (i + 1) + "  " + CronExpressionUtil.formatDate(t));
        }
    }

    // =========================================================
    // Spring/Quartz 无效表达式
    // =========================================================

    @Test(dataProvider = "invalidSpringExpressions")
    public void testSpringExpressionInvalid(String expr) {
        assertFalse(CronExpressionUtil.isValid(expr, CronMode.SPRING_QUARTZ),
                "Should be INVALID for Spring/Quartz: '" + expr + "'");
        System.out.println("[Spring invalid OK] '" + expr + "'");
    }

    // =========================================================
    // Linux Crontab 有效表达式
    // =========================================================

    @Test(dataProvider = "validLinuxExpressions")
    public void testLinuxExpressionValid(String expr, String description,
                                         int expectedHour, int expectedMinute) {
        System.out.println("\n[Linux] " + description + "  =>  " + expr);
        assertTrue(CronExpressionUtil.isValid(expr, CronMode.LINUX_CRONTAB),
                "Should be valid Linux crontab: " + expr);

        String desc = CronExpressionUtil.describe(expr, CronMode.LINUX_CRONTAB);
        assertNotNull(desc);
        assertFalse(desc.isEmpty());
        System.out.println("  describe: " + desc);

        List<Date> times = CronExpressionUtil.getNextExecutionTimes(expr, 5, CronMode.LINUX_CRONTAB);
        assertNotNull(times);
        assertFalse(times.isEmpty(), "Should produce execution times: " + expr);

        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        for (int i = 0; i < times.size(); i++) {
            Date t = times.get(i);
            assertTrue(t.after(now), "Time must be in the future");
            if (i > 0) assertTrue(t.after(times.get(i - 1)), "Times must be ascending");
            cal.setTime(t);
            // Linux crontab 最小粒度是分钟，秒必须为 0
            assertEquals(cal.get(Calendar.SECOND), 0, "Linux cron second must be 0: " + expr);
            if (expectedHour >= 0) {
                assertEquals(cal.get(Calendar.HOUR_OF_DAY), expectedHour, "Hour: " + expr);
            }
            if (expectedMinute >= 0) {
                assertEquals(cal.get(Calendar.MINUTE), expectedMinute, "Minute: " + expr);
            }
            System.out.println("  #" + (i + 1) + "  " + CronExpressionUtil.formatDate(t));
        }
    }

    // =========================================================
    // Linux Crontab 无效表达式
    // =========================================================

    @Test(dataProvider = "invalidLinuxExpressions")
    public void testLinuxExpressionInvalid(String expr) {
        assertFalse(CronExpressionUtil.isValid(expr, CronMode.LINUX_CRONTAB),
                "Should be INVALID for Linux crontab: '" + expr + "'");
        System.out.println("[Linux invalid OK] '" + expr + "'");
    }

    // =========================================================
    // 模式自动检测: detectMode()
    // =========================================================

    @Test(dataProvider = "modeDetection")
    public void testDetectMode(String expr, CronMode expectedMode) {
        CronMode detected = CronExpressionUtil.detectMode(expr);
        assertEquals(detected, expectedMode, "detectMode() mismatch: " + expr);
        System.out.println("[detectMode] " + expr + "  =>  " + detected);
    }

    // =========================================================
    // normalizeCron: Linux 5位 → 补秒 → 6位
    // =========================================================

    @Test
    public void testNormalizeCronLinux() {
        String linux = "0 12 * * *";
        String normalized = CronExpressionUtil.normalizeCron(linux, CronMode.LINUX_CRONTAB);
        assertEquals(normalized, "0 " + linux,
                "Linux cron should be prefixed with '0' (second field)");
        System.out.println("[normalize] " + linux + "  =>  " + normalized);
    }

    @Test
    public void testNormalizeCronSpringUnchanged() {
        String spring = "0 0 12 * * ?";
        assertEquals(CronExpressionUtil.normalizeCron(spring, CronMode.SPRING_QUARTZ), spring,
                "Spring/Quartz should not be changed by normalize");
    }

    // =========================================================
    // 自动检测 isValid (无 mode 参数)
    // =========================================================

    @Test
    public void testAutoDetectValidSpring() {
        assertTrue(CronExpressionUtil.isValid("0 0 12 * * ?"), "Auto-detect: valid Spring");
    }

    @Test
    public void testAutoDetectValidLinux() {
        assertTrue(CronExpressionUtil.isValid("0 12 * * *"), "Auto-detect: valid Linux");
    }

    // =========================================================
    // 自动检测 getNextExecutionTimes (无 mode 参数)
    // =========================================================

    @Test
    public void testAutoDetectTimesSpring() {
        List<Date> times = CronExpressionUtil.getNextExecutionTimes("0 0 12 * * ?", 3);
        assertEquals(times.size(), 3);
        for (Date t : times) {
            Calendar c = Calendar.getInstance();
            c.setTime(t);
            assertEquals(c.get(Calendar.HOUR_OF_DAY), 12);
            assertEquals(c.get(Calendar.MINUTE), 0);
            assertEquals(c.get(Calendar.SECOND), 0);
        }
        System.out.println("[autoDetect Spring] 0 0 12 * * ? -> 3 times verified");
    }

    @Test
    public void testAutoDetectTimesLinux() {
        List<Date> times = CronExpressionUtil.getNextExecutionTimes("0 12 * * *", 3);
        assertEquals(times.size(), 3);
        for (Date t : times) {
            Calendar c = Calendar.getInstance();
            c.setTime(t);
            assertEquals(c.get(Calendar.HOUR_OF_DAY), 12);
            assertEquals(c.get(Calendar.MINUTE), 0);
            assertEquals(c.get(Calendar.SECOND), 0, "Linux second must be 0");
        }
        System.out.println("[autoDetect Linux] 0 12 * * * -> 3 times verified");
    }

    // =========================================================
    // Linux: 工作日过滤 (1-5 = Mon-Fri)
    // =========================================================

    @Test
    public void testLinuxWeekdayFilter() {
        List<Date> times = CronExpressionUtil.getNextExecutionTimes("0 9 * * 1-5", 10, CronMode.LINUX_CRONTAB);
        assertEquals(times.size(), 10);
        Calendar cal = Calendar.getInstance();
        for (Date t : times) {
            cal.setTime(t);
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            System.out.println("  " + CronExpressionUtil.formatDate(t) + " [" + getDayName(dow) + "]");
            assertTrue(dow >= Calendar.MONDAY && dow <= Calendar.FRIDAY,
                    "Should be Mon-Fri, got: " + getDayName(dow));
            assertEquals(cal.get(Calendar.HOUR_OF_DAY), 9);
            assertEquals(cal.get(Calendar.MINUTE), 0);
            assertEquals(cal.get(Calendar.SECOND), 0);
        }
        System.out.println("✅ Linux Mon-Fri at 09:00 correct");
    }

    // =========================================================
    // Linux: Sunday 别名 - 0 和 7 均表示周日
    // =========================================================

    @Test
    public void testLinuxSundayAlias() {
        List<Date> times0 = CronExpressionUtil.getNextExecutionTimes("0 8 * * 0", 3, CronMode.LINUX_CRONTAB);
        List<Date> times7 = CronExpressionUtil.getNextExecutionTimes("0 8 * * 7", 3, CronMode.LINUX_CRONTAB);
        assertFalse(times0.isEmpty(), "Sunday via '0' should produce results");
        assertFalse(times7.isEmpty(), "Sunday via '7' should produce results");
        Calendar cal = Calendar.getInstance();
        for (Date t : times0) {
            cal.setTime(t);
            assertEquals(cal.get(Calendar.DAY_OF_WEEK), Calendar.SUNDAY, "'0' should be Sunday");
        }
        for (Date t : times7) {
            cal.setTime(t);
            assertEquals(cal.get(Calendar.DAY_OF_WEEK), Calendar.SUNDAY, "'7' should be Sunday");
        }
        System.out.println("✅ Linux Sunday alias (0 and 7) correct");
    }

    // =========================================================
    // Spring: 工作日过滤 MON-FRI
    // =========================================================

    @Test
    public void testSpringWeekdayFilter() {
        List<Date> times = CronExpressionUtil.getNextExecutionTimes("0 0 9 ? * MON-FRI", 10, CronMode.SPRING_QUARTZ);
        assertEquals(times.size(), 10);
        Calendar cal = Calendar.getInstance();
        for (Date t : times) {
            cal.setTime(t);
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            System.out.println("  " + CronExpressionUtil.formatDate(t) + " [" + getDayName(dow) + "]");
            assertTrue(dow >= Calendar.MONDAY && dow <= Calendar.FRIDAY,
                    "Should be Mon-Fri, got: " + getDayName(dow));
            assertEquals(cal.get(Calendar.HOUR_OF_DAY), 9);
            assertEquals(cal.get(Calendar.MINUTE), 0);
            assertEquals(cal.get(Calendar.SECOND), 0);
        }
        System.out.println("✅ Spring MON-FRI at 9:00 correct");
    }

    // =========================================================
    // Spring: 范围+步长  8-18/2
    // =========================================================

    @Test
    public void testSpringRangeWithStep() {
        List<Date> times = CronExpressionUtil.getNextExecutionTimes("0 0 8-18/2 * * ?", 6, CronMode.SPRING_QUARTZ);
        assertFalse(times.isEmpty());
        Calendar cal = Calendar.getInstance();
        for (Date t : times) {
            cal.setTime(t);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            System.out.println("  hour=" + hour + "  " + CronExpressionUtil.formatDate(t));
            assertTrue(hour >= 8 && hour <= 18, "Hour in [8,18], got " + hour);
            assertEquals((hour - 8) % 2, 0, "(h-8) % 2 must be 0");
            assertEquals(cal.get(Calendar.MINUTE), 0);
            assertEquals(cal.get(Calendar.SECOND), 0);
        }
        System.out.println("✅ Range+step 8-18/2 correct");
    }

    // =========================================================
    // Spring: L - 每月最后一天
    // =========================================================

    @Test
    public void testSpringLastDayOfMonth() {
        List<Date> times = CronExpressionUtil.getNextExecutionTimes("0 0 0 L * ?", 3, CronMode.SPRING_QUARTZ);
        assertFalse(times.isEmpty(), "L expression should produce results");
        Calendar cal = Calendar.getInstance();
        for (Date t : times) {
            cal.setTime(t);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            System.out.println("  day=" + day + "/" + lastDay + "  " + CronExpressionUtil.formatDate(t));
            assertEquals(day, lastDay, "Should execute on last day of month");
        }
        System.out.println("✅ L (last day of month) correct");
    }

    // =========================================================
    // Spring: 年份字段
    // =========================================================

    @Test
    public void testSpringYearField() {
        // 未来年份 2099 应有结果
        List<Date> future = CronExpressionUtil.getNextExecutionTimes("0 0 12 1 1 ? 2099", 1, CronMode.SPRING_QUARTZ);
        assertFalse(future.isEmpty(), "Future year 2099 should have results");
        Calendar cal = Calendar.getInstance();
        cal.setTime(future.get(0));
        assertEquals(cal.get(Calendar.YEAR), 2099, "Year should be 2099");

        // 过去年份 2000 应为空
        List<Date> past = CronExpressionUtil.getNextExecutionTimes("0 0 12 1 1 ? 2000", 1, CronMode.SPRING_QUARTZ);
        assertTrue(past.isEmpty(), "Past year 2000 should return empty list");
        System.out.println("✅ Year field works correctly");
    }

    // =========================================================
    // describe()
    // =========================================================

    @Test
    public void testDescribeSpring() {
        String desc = CronExpressionUtil.describe("0 0 12 * * ?", CronMode.SPRING_QUARTZ);
        System.out.println("[describe Spring] " + desc);
        assertNotNull(desc);
        assertFalse(desc.isEmpty());
        assertTrue(desc.contains("12"), "Should mention hour 12");
    }

    @Test
    public void testDescribeLinux() {
        String desc = CronExpressionUtil.describe("0 12 * * *", CronMode.LINUX_CRONTAB);
        System.out.println("[describe Linux] " + desc);
        assertNotNull(desc);
        assertFalse(desc.isEmpty());
        assertTrue(desc.contains("12"), "Should mention hour 12");
    }

    @Test
    public void testDescribeAutoDetect() {
        String s = CronExpressionUtil.describe("0 0 9 ? * MON-FRI");
        assertNotNull(s);
        System.out.println("[describe auto Spring] " + s);

        String l = CronExpressionUtil.describe("0 9 * * 1-5");
        assertNotNull(l);
        System.out.println("[describe auto Linux] " + l);
    }

    // =========================================================
    // formatDate()
    // =========================================================

    @Test
    public void testFormatDate() {
        String fmt = CronExpressionUtil.formatDate(new Date());
        assertNotNull(fmt);
        assertFalse(fmt.isEmpty());
        assertTrue(fmt.contains("-"), "Format should contain '-'");
        assertTrue(fmt.contains(":"), "Format should contain ':'");
        System.out.println("[formatDate] " + fmt);
    }

    // =========================================================
    // 边界: null / 空字符串
    // =========================================================

    @Test
    public void testNullAndEmptyExpressions() {
        assertFalse(CronExpressionUtil.isValid(null), "null auto-detect");
        assertFalse(CronExpressionUtil.isValid(""), "empty auto-detect");
        assertFalse(CronExpressionUtil.isValid(null, CronMode.SPRING_QUARTZ), "null Spring");
        assertFalse(CronExpressionUtil.isValid("", CronMode.LINUX_CRONTAB), "empty Linux");

        List<Date> times = CronExpressionUtil.getNextExecutionTimes("", 5);
        assertNotNull(times);
        assertTrue(times.isEmpty(), "Empty expr returns empty list");
        System.out.println("✅ null/empty edge cases OK");
    }

    // =========================================================
    // 跨模式互斥: Linux 5位在Spring下无效，Spring 6位在Linux下无效
    // =========================================================

    @Test
    public void testCrossModeInvalidity() {
        assertFalse(CronExpressionUtil.isValid("0 12 * * *", CronMode.SPRING_QUARTZ),
                "Linux 5-field should be INVALID in Spring mode");
        assertFalse(CronExpressionUtil.isValid("0 0 12 * * ?", CronMode.LINUX_CRONTAB),
                "Spring 6-field should be INVALID in Linux mode");
        System.out.println("✅ Cross-mode validation correct");
    }

    // =========================================================
    // Helper
    // =========================================================

    private String getDayName(int dayOfWeek) {
        String[] days = {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return (dayOfWeek >= 1 && dayOfWeek <= 7) ? days[dayOfWeek] : "Unknown(" + dayOfWeek + ")";
    }
}

