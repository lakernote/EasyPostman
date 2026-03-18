package com.laker.postman.plugin.redis;

import com.laker.postman.util.I18nUtil;

public final class RedisI18n {

    private static final String BUNDLE_NAME = "redis-messages";

    private RedisI18n() {
    }

    public static String t(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE_NAME, RedisI18n.class.getClassLoader(), key, args);
    }
}
