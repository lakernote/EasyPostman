package com.laker.postman.plugin.capture;

import com.laker.postman.util.I18nUtil;

public final class CaptureI18n {

    private static final String BUNDLE_NAME = "capture-messages";

    private CaptureI18n() {
    }

    public static String t(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE_NAME, CaptureI18n.class.getClassLoader(), key, args);
    }
}
