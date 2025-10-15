package com.laker.postman.util;

import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 国际化工具类
 */
@Slf4j
public class I18nUtil {
    private static final String BUNDLE_NAME = "messages";
    private static ResourceBundle resourceBundle;
    private static Locale currentLocale;

    // 私有构造函数，防止实例化
    private I18nUtil() {
        // 工具类不应该被实例化
    }

    static {
        // 从用户设置中读取语言设置，默认使用系统语言
        String savedLocale = UserSettingsUtil.getLanguage();
        if (savedLocale != null && !savedLocale.isEmpty()) {
            try {
                if ("zh".equals(savedLocale)) {
                    currentLocale = Locale.CHINESE;
                } else if ("en".equals(savedLocale)) {
                    currentLocale = Locale.ENGLISH;
                } else {
                    currentLocale = getSystemDefaultLocale();
                }
            } catch (Exception e) {
                log.warn("Failed to parse saved locale: {}", savedLocale, e);
                currentLocale = getSystemDefaultLocale();
            }
        } else {
            currentLocale = getSystemDefaultLocale();
        }

        loadResourceBundle();
    }

    /**
     * 获取系统默认语言环境
     */
    private static Locale getSystemDefaultLocale() {
        Locale systemLocale = Locale.getDefault();
        String language = systemLocale.getLanguage();
        log.info("Detected system locale: {} ", language);
        // 支持中文和英文，其他语言默认使用英文
        if ("zh".equals(language)) {
            return Locale.CHINESE;
        } else {
            return Locale.ENGLISH;
        }
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
                resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
                log.info("Fallback to English resource bundle");
            } catch (MissingResourceException ex) {
                log.error("Failed to load fallback English resource bundle", ex);
                throw new IllegalStateException("Unable to load any resource bundle", ex);
            }
        }
    }

    /**
     * 获取国际化消息
     *
     * @param key 消息键
     * @return 国际化消息
     */
    public static String getMessage(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception e) {
            log.warn("Missing resource key: {}", key);
            return "!" + key + "!";
        }
    }

    /**
     * 获取带参数的国际化消息
     *
     * @param key  消息键
     * @param args 参数
     * @return 格式化后的国际化消息
     */
    public static String getMessage(String key, Object... args) {
        try {
            String pattern = resourceBundle.getString(key);
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            log.warn("Missing resource key: {}", key);
            return "!" + key + "!";
        }
    }

    /**
     * 设置语言环境
     *
     * @param locale 语言环境
     */
    public static void setLocale(Locale locale) {
        if (locale != null && !locale.equals(currentLocale)) {
            currentLocale = locale;
            loadResourceBundle();

            // 保存到用户设置
            String localeCode = locale.getLanguage();
            UserSettingsUtil.saveLanguage(localeCode);

            log.info("Locale changed to: {}", locale);
        }
    }

    /**
     * 设置语言环境（通过语言代码）
     *
     * @param languageCode 语言代码 (zh, en)
     */
    public static void setLocale(String languageCode) {
        Locale locale = switch (languageCode.toLowerCase()) {
            case "zh", "chinese" -> Locale.CHINESE;
            case "en", "english" -> Locale.ENGLISH;
            default -> {
                log.warn("Unsupported language code: {}, using English as default", languageCode);
                yield Locale.ENGLISH;
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
}
