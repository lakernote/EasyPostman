package com.laker.postman.plugin.redis;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

final class I18nUtil {

    private static final String BUNDLE_NAME = "redis-messages";

    private I18nUtil() {
    }

    static String getMessage(String key, Object... args) {
        String pattern = key;
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault(),
                    I18nUtil.class.getClassLoader());
            pattern = bundle.getString(key);
        } catch (MissingResourceException ignore) {
            // fall back to the key when resource is absent
        }
        return args == null || args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }
}
