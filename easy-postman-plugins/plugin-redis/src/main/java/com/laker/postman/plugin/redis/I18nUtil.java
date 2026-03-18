package com.laker.postman.plugin.redis;

import com.laker.postman.util.UserSettingsUtil;

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
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, resolveLocale(),
                    I18nUtil.class.getClassLoader());
            pattern = bundle.getString(key);
        } catch (MissingResourceException ignore) {
            // fall back to the key when resource is absent
        }
        return args == null || args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }

    private static Locale resolveLocale() {
        String savedLocale = UserSettingsUtil.getLanguage();
        if ("zh".equalsIgnoreCase(savedLocale)) {
            return Locale.CHINESE;
        }
        if ("en".equalsIgnoreCase(savedLocale)) {
            return Locale.ENGLISH;
        }
        Locale systemLocale = Locale.getDefault();
        return "zh".equalsIgnoreCase(systemLocale.getLanguage()) ? Locale.CHINESE : Locale.ENGLISH;
    }
}
