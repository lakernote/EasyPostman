package com.laker.postman.util;

public final class UiI18n {

    private static final String BUNDLE_NAME = "ui-messages";

    private UiI18n() {
    }

    public static String get(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE_NAME, UiI18n.class.getClassLoader(), key, args);
    }
}
