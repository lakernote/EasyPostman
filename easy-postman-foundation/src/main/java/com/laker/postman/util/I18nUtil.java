package com.laker.postman.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 国际化工具类
 */
@Slf4j
@UtilityClass
public class I18nUtil {
    private static final String BUNDLE_NAME = "messages";
    private static final String COMMON_BUNDLE_NAME = CommonI18n.BUNDLE_NAME;
    private static final Locale FALLBACK_LOCALE = Locale.ENGLISH;
    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.CHINESE);
    private static final ResourceBundle EMPTY_RESOURCE_BUNDLE = new ResourceBundle() {
        @Override
        protected Object handleGetObject(String key) {
            throw new MissingResourceException("Missing resource key", BUNDLE_NAME, key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }
    };
    private static ResourceBundle resourceBundle;
    private static Locale currentLocale;
    private static final ConcurrentMap<BundleCacheKey, ResourceBundle> BUNDLE_CACHE = new ConcurrentHashMap<>();

    static {
        // 从用户设置中读取语言设置，默认使用系统语言
        String savedLocale = UserSettingsUtil.getLanguage();
        if (savedLocale != null && !savedLocale.isEmpty()) {
            try {
                currentLocale = normalizeSupportedLocale(Locale.forLanguageTag(savedLocale.replace('_', '-')));
            } catch (Exception e) {
                log.warn("Failed to parse saved locale: {}", savedLocale, e);
                currentLocale = getSystemDefaultLocale();
            }
        } else {
            currentLocale = getSystemDefaultLocale();
        }

        loadResourceBundle();
        I18nBundleRegistry.registerBundle("app", BUNDLE_NAME, I18nUtil.class.getClassLoader());
        I18nBundleRegistry.registerBundle("foundation", COMMON_BUNDLE_NAME, CommonI18n.class.getClassLoader());
    }

    /**
     * 获取系统默认语言环境
     */
    private static Locale getSystemDefaultLocale() {
        Locale systemLocale = Locale.getDefault();
        String language = systemLocale.getLanguage();
        log.info("Detected system locale: {} ", language);
        return normalizeSupportedLocale(systemLocale);
    }

    public static List<Locale> supportedLocales() {
        return SUPPORTED_LOCALES;
    }

    public static Locale fallbackLocale() {
        return FALLBACK_LOCALE;
    }

    public static Locale currentLocale() {
        return currentLocale;
    }

    public static Locale normalizeSupportedLocale(Locale locale) {
        if (locale == null) {
            return FALLBACK_LOCALE;
        }
        return "zh".equals(locale.getLanguage()) ? Locale.CHINESE : FALLBACK_LOCALE;
    }

    /**
     * 加载资源包
     */
    private static void loadResourceBundle() {
        try {
            resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
            log.info("Loaded resource bundle for locale: {}", currentLocale);
        } catch (MissingResourceException e) {
            log.error("Failed to load resource bundle for locale: {}", currentLocale, e);
            // 如果加载失败，尝试加载默认的英文资源包
            try {
                resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, FALLBACK_LOCALE);
                log.info("Fallback to English resource bundle");
            } catch (MissingResourceException ex) {
                log.error("Failed to load fallback English resource bundle", ex);
                resourceBundle = EMPTY_RESOURCE_BUNDLE;
            }
        }
    }

    private static ResourceBundle getBundle(String bundleName, Locale locale, ClassLoader classLoader) {
        ClassLoader resolvedClassLoader = classLoader != null ? classLoader : I18nUtil.class.getClassLoader();
        BundleCacheKey cacheKey = new BundleCacheKey(bundleName, locale, resolvedClassLoader);
        return BUNDLE_CACHE.computeIfAbsent(cacheKey, key -> ResourceBundle.getBundle(
                key.bundleName(), key.locale(), key.classLoader()));
    }

    /**
     * 获取国际化消息
     *
     * @param key 消息键
     * @return 国际化消息
     */
    public static String getMessage(String key) {
        return getMessagePattern(key);
    }

    /**
     * 获取带参数的国际化消息
     *
     * @param key  消息键
     * @param args 参数
     * @return 格式化后的国际化消息
     */
    public static String getMessage(String key, Object... args) {
        String pattern = getMessagePattern(key);
        return args == null || args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }

    private static String getMessagePattern(String key) {
        String appMessage = getAppMessageOrNull(key);
        if (appMessage != null) {
            return appMessage;
        }

        String commonMessage = getBundleMessageOrNull(COMMON_BUNDLE_NAME, CommonI18n.class.getClassLoader(), key);
        if (commonMessage != null) {
            return commonMessage;
        }

        log.warn("Missing resource key: {}", key);
        return "!" + key + "!";
    }

    private static String getAppMessageOrNull(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException ignored) {
            if (!FALLBACK_LOCALE.getLanguage().equals(currentLocale.getLanguage())) {
                return getMessageOrNull(BUNDLE_NAME, FALLBACK_LOCALE, I18nUtil.class.getClassLoader(), key);
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to load resource key: {}", key, e);
            return null;
        }
    }

    public static String getMessage(String bundleName, ClassLoader classLoader, String key, Object... args) {
        I18nBundleRegistry.registerBundle(bundleName, bundleName, classLoader);
        String pattern = getBundleMessageOrNull(bundleName, classLoader, key);
        if (pattern == null) {
            log.warn("Missing resource key: {} in bundle: {}", key, bundleName);
            pattern = key;
        }
        return args == null || args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    }

    private static String getBundleMessageOrNull(String bundleName, ClassLoader classLoader, String key) {
        String currentMessage = getMessageOrNull(bundleName, currentLocale, classLoader, key);
        if (currentMessage != null) {
            return currentMessage;
        }
        if (!FALLBACK_LOCALE.getLanguage().equals(currentLocale.getLanguage())) {
            return getMessageOrNull(bundleName, FALLBACK_LOCALE, classLoader, key);
        }
        return null;
    }

    private static String getMessageOrNull(String bundleName, Locale locale, ClassLoader classLoader, String key) {
        try {
            ResourceBundle bundle = getBundle(bundleName, locale, classLoader);
            return bundle.getString(key);
        } catch (MissingResourceException ignored) {
            return null;
        } catch (Exception e) {
            log.warn("Failed to load resource key: {} in bundle: {}", key, bundleName, e);
            return null;
        }
    }

    /**
     * 设置语言环境
     *
     * @param locale 语言环境
     */
    public static void setLocale(Locale locale) {
        Locale normalizedLocale = normalizeSupportedLocale(locale);
        if (!normalizedLocale.equals(currentLocale)) {
            currentLocale = normalizedLocale;
            BUNDLE_CACHE.clear();
            loadResourceBundle();

            // 保存到用户设置
            String localeCode = normalizedLocale.getLanguage();
            UserSettingsUtil.saveLanguage(localeCode);

            log.info("Locale changed to: {}", normalizedLocale);
        }
    }

    /**
     * 设置语言环境（通过语言代码）
     *
     * @param languageCode 语言代码 (zh, en)
     */
    public static void setLocale(String languageCode) {
        Locale locale = switch (languageCode == null ? "" : languageCode.toLowerCase(Locale.ROOT)) {
            case "zh", "zh_cn", "zh-cn", "chinese" -> Locale.CHINESE;
            case "en", "en_us", "en-us", "english" -> Locale.ENGLISH;
            default -> {
                log.warn("Unsupported language code: {}, using English as default", languageCode);
                yield FALLBACK_LOCALE;
            }
        };
        setLocale(locale);
    }

    /**
     * 是否为中文环境
     *
     * @return true if current locale is Chinese
     */
    public static boolean isChinese() {
        return Locale.CHINESE.equals(currentLocale) ||
                "zh".equals(currentLocale.getLanguage());
    }

    private record BundleCacheKey(String bundleName, Locale locale, ClassLoader classLoader) {
        private BundleCacheKey {
            Objects.requireNonNull(bundleName, "bundleName");
            Objects.requireNonNull(locale, "locale");
            Objects.requireNonNull(classLoader, "classLoader");
        }
    }
}
