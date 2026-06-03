package com.laker.postman.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UiI18n {

    private static final String BUNDLE_NAME = "ui-messages";

    public static String get(String key, Object... args) {
        I18nBundleRegistry.registerBundle("ui", BUNDLE_NAME, UiI18n.class.getClassLoader());
        return I18nUtil.getMessage(BUNDLE_NAME, UiI18n.class.getClassLoader(), key, args);
    }
}
