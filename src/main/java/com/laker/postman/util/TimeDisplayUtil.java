package com.laker.postman.util;

/**
 * 时间显示工具类
 * 提供将毫秒时间转换为更友好的显示格式
 */
public class TimeDisplayUtil {

    /**
     * 将毫秒时间转换为更友好的显示格式
     * - 小于1秒显示为 "xx ms"
     * - 小于1分钟显示为 "x.x s"
     * - 大于等于1分钟显示为 "xm xs"
     *
     * @param elapsedTimeMillis 毫秒时间
     * @return 格式化后的时间字符串
     */
    public static String formatElapsedTime(long elapsedTimeMillis) {
        if (elapsedTimeMillis < 1000) {
            return elapsedTimeMillis + " ms";
        } else if (elapsedTimeMillis < 60000) {
            return String.format("%.1f s", elapsedTimeMillis / 1000.0);
        } else {
            int minutes = (int) (elapsedTimeMillis / 60000);
            int seconds = (int) ((elapsedTimeMillis % 60000) / 1000);
            return minutes + "m " + seconds + "s";
        }
    }
}
