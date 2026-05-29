package com.laker.postman.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class CommonI18n {

    public static final String BUNDLE_NAME = "common-messages";

    public static String get(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE_NAME, CommonI18n.class.getClassLoader(), key, args);
    }
}
