package com.laker.postman.model;

/**
 * 更新检查频率枚举
 */
public enum UpdateCheckFrequency {
    STARTUP("startup", 0),   // 每次启动
    DAILY("daily", 1),       // 每日
    WEEKLY("weekly", 7),     // 每周
    MONTHLY("monthly", 30);  // 每月

    private final String code;
    private final int days;

    UpdateCheckFrequency(String code, int days) {
        this.code = code;
        this.days = days;
    }

    public String getCode() {
        return code;
    }

    public int getDays() {
        return days;
    }

    /**
     * 是否每次启动都检查
     */
    public boolean isAlwaysCheck() {
        return this == STARTUP;
    }

    public static UpdateCheckFrequency fromCode(String code) {
        for (UpdateCheckFrequency frequency : values()) {
            if (frequency.code.equals(code)) {
                return frequency;
            }
        }
        return DAILY; // 默认每日
    }
}