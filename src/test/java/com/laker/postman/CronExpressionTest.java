package com.laker.postman;

import com.laker.postman.util.CronExpressionUtil;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Cron表达式测试 - 使用TestNG
 */
public class CronExpressionTest {

    @DataProvider(name = "cronExpressions")
    public Object[][] cronExpressions() {
        return new Object[][]{
                {"0 0 12 * * ?", "每天中午12点", 12, 0, -1},            // 小时=12, 分钟=0
                {"0 */5 * * * ?", "每5分钟", -1, -1, 5},                // 分钟是5的倍数
                {"0 0 */2 * * ?", "每2小时", -1, 0, -1},                // 小时是2的倍数, 分钟=0
                {"0 0 9 ? * MON-FRI", "工作日早上9点", 9, 0, -1},       // 小时=9, 分钟=0, 周一到周五
                {"0 0 0 1 * ?", "每月1号零点", 0, 0, -1},               // 小时=0, 分钟=0, 日=1
                {"0 15 10 * * ?", "每天10:15", 10, 15, -1},             // 小时=10, 分钟=15
                {"0 0/30 * * * ?", "每30分钟", -1, -1, 30},             // 分钟是30的倍数
                {"0 0 0 * * ?", "每天零点", 0, 0, -1},                  // 小时=0, 分钟=0
                {"*/10 * * * * ?", "每10秒", -1, -1, 10},               // 秒是10的倍数
                {"0 0 8-18/2 * * ?", "8点到18点每2小时", -1, 0, -1}     // 特殊：范围+步长, 分钟=0
        };
    }

    @Test(dataProvider = "cronExpressions")
    public void testCronExpression(String cronExpr, String description, int expectedHour, int expectedMinute, int expectedStep) {
        System.out.println("\n=== Testing: " + description + " ===");
        System.out.println("Cron: " + cronExpr);

        // 验证表达式有效性
        assertTrue(CronExpressionUtil.isValid(cronExpr),
                "Cron expression should be valid: " + cronExpr);

        // 获取描述
        String desc = CronExpressionUtil.describe(cronExpr);
        assertNotNull(desc, "Description should not be null");
        System.out.println("Description: " + desc);

        // 计算下次执行时间
        List<Date> times = CronExpressionUtil.getNextExecutionTimes(cronExpr, 5);
        assertNotNull(times, "Execution times should not be null");
        assertFalse(times.isEmpty(), "Should have at least one execution time");
        assertEquals(times.size(), 5, "Should return 5 execution times");

        System.out.println("Next 5 executions:");
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < times.size(); i++) {
            Date time = times.get(i);
            System.out.println("  " + (i + 1) + ". " + CronExpressionUtil.formatDate(time));

            // 验证时间在未来
            assertTrue(time.after(new Date()),
                    "Execution time should be in the future: " + time);

            // 验证时间递增
            if (i > 0) {
                assertTrue(time.after(times.get(i - 1)),
                        "Execution times should be in ascending order");
            }

            // 验证特定字段（如果指定）
            cal.setTime(time);
            if (expectedHour >= 0) {
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                // 对于范围+步长的情况，只验证时间合理性
                if (!"0 0 8-18/2 * * ?".equals(cronExpr)) {
                    assertEquals(hour, expectedHour,
                            "Hour should be " + expectedHour + " for: " + cronExpr);
                }
            }
            if (expectedMinute >= 0) {
                assertEquals(cal.get(Calendar.MINUTE), expectedMinute,
                        "Minute should be " + expectedMinute + " for: " + cronExpr);
            }
        }
    }

    @Test
    public void testWeekdayExpression() {
        String cronExpr = "0 0 9 ? * MON-FRI";
        System.out.println("\n=== Testing Weekday Expression ===");
        System.out.println("Cron: " + cronExpr);

        List<Date> times = CronExpressionUtil.getNextExecutionTimes(cronExpr, 10);
        assertNotNull(times);
        assertEquals(times.size(), 10);

        Calendar cal = Calendar.getInstance();
        System.out.println("Next 10 executions (should be Mon-Fri only):");

        for (int i = 0; i < times.size(); i++) {
            cal.setTime(times.get(i));
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            String dayName = getDayName(dayOfWeek);

            System.out.println("  " + (i + 1) + ". " + CronExpressionUtil.formatDate(times.get(i)) + " [" + dayName + "]");

            // 验证是工作日（周一到周五）
            assertTrue(dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY,
                    "Day should be Monday-Friday, but got: " + dayName);

            // 验证时间是早上9点
            assertEquals(cal.get(Calendar.HOUR_OF_DAY), 9, "Hour should be 9");
            assertEquals(cal.get(Calendar.MINUTE), 0, "Minute should be 0");
            assertEquals(cal.get(Calendar.SECOND), 0, "Second should be 0");
        }

        System.out.println("✅ All executions are on weekdays (Mon-Fri) at 9:00 AM");
    }

    @Test
    public void testRangeWithStepExpression() {
        String cronExpr = "0 0 8-18/2 * * ?";
        System.out.println("\n=== Testing Range with Step Expression ===");
        System.out.println("Cron: " + cronExpr);
        System.out.println("Expected: Execute at 8:00, 10:00, 12:00, 14:00, 16:00, 18:00");

        List<Date> times = CronExpressionUtil.getNextExecutionTimes(cronExpr, 6);
        assertNotNull(times);
        assertEquals(times.size(), 6, "Should return 6 execution times in one day");

        Calendar cal = Calendar.getInstance();
        int[] expectedHours = {8, 10, 12, 14, 16, 18};

        System.out.println("Next 6 executions:");
        for (int i = 0; i < times.size(); i++) {
            cal.setTime(times.get(i));
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            System.out.println("  " + (i + 1) + ". " + CronExpressionUtil.formatDate(times.get(i)) +
                    " (Hour: " + hour + ")");

            // 验证小时在8-18范围内，且是2的步长
            assertTrue(hour >= 8 && hour <= 18, "Hour should be between 8 and 18");
            assertEquals((hour - 8) % 2, 0, "Hour should be 8 + 2*n");
        }

        System.out.println("✅ All executions are at correct hours with 2-hour steps");
    }

    @Test
    public void testInvalidExpression() {
        String[] invalidExpressions = {
                "",                    // 空表达式
                "* * * *",            // 字段太少
                "* * * * * * * *",    // 字段太多
                "invalid",            // 无效格式
                "abc def * * * ?"     // 包含非法字符
        };

        System.out.println("\n=== Testing Invalid Expressions ===");
        for (String expr : invalidExpressions) {
            System.out.println("Testing invalid: '" + expr + "'");
            assertFalse(CronExpressionUtil.isValid(expr),
                    "Expression should be invalid: " + expr);
        }
        System.out.println("✅ All invalid expressions correctly rejected");
    }

    private String getDayName(int dayOfWeek) {
        String[] days = {"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[dayOfWeek];
    }
}

