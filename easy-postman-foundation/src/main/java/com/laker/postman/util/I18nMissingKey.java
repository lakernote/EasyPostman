package com.laker.postman.util;

import java.util.Locale;

public record I18nMissingKey(String ownerId, String bundleName, Locale locale, String key) {
}
