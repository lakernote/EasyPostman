package com.laker.postman.plugin.decompiler;

import com.laker.postman.util.I18nUtil;

public final class DecompilerI18n {

    public static final String BUNDLE_NAME = "decompiler-messages";

    private DecompilerI18n() {
    }

    public static String t(String key, Object... args) {
        return I18nUtil.getMessage(BUNDLE_NAME, DecompilerI18n.class.getClassLoader(), key, args);
    }
}
