package com.laker.postman.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.constants.ConfigPathConstants;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User-level UI preferences stored in {@code user_settings.json}.
 */
@Slf4j
@UtilityClass
public class UserPreferencesStore {
    private static final String KEY_WINDOW_WIDTH = "windowWidth";
    private static final String KEY_WINDOW_HEIGHT = "windowHeight";
    private static final String KEY_WINDOW_EXTENDED_STATE = "windowExtendedState";  // 窗口状态
    private static final String KEY_LANGUAGE = "language";
    private static final String PREFERENCES_PATH = ConfigPathConstants.USER_SETTINGS;
    private static final Object LOCK = new Object();
    private static Map<String, Object> preferencesCache = null;

    private static Map<String, Object> readPreferences() {
        synchronized (LOCK) {
            if (preferencesCache != null) return preferencesCache;
            File file = new File(PREFERENCES_PATH);
            if (!file.exists()) {
                preferencesCache = new HashMap<>();
                return preferencesCache;
            }
            try {
                String json = FileUtil.readString(file, StandardCharsets.UTF_8);
                if (json == null || json.isBlank()) {
                    preferencesCache = new HashMap<>();
                } else {
                    preferencesCache = JSONUtil.parseObj(json);
                }
            } catch (Exception e) {
                log.warn("读取用户偏好失败", e);
                preferencesCache = new HashMap<>();
            }
            return preferencesCache;
        }
    }

    private static void writePreferences() {
        synchronized (LOCK) {
            try {
                String json = JSONUtil.toJsonPrettyStr(preferencesCache);
                FileUtil.writeString(json, new File(PREFERENCES_PATH), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("保存用户偏好失败", e);
            }
        }
    }

    public static void put(String key, Object value) {
        synchronized (LOCK) {
            readPreferences();
            preferencesCache.put(key, value);
            writePreferences();
        }
    }

    public static Object get(String key) {
        return readPreferences().get(key);
    }

    public static String getString(String key) {
        Object v = get(key);
        return v == null ? null : v.toString();
    }

    public static Boolean getBoolean(String key) {
        Object v = get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return Boolean.FALSE;
    }

    public static Integer getInt(String key) {
        Object v = get(key);
        if (v instanceof Integer i) return i;
        if (v instanceof String s) try {
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            // 忽略转换异常
        }
        return null;
    }

    public static void remove(String key) {
        synchronized (LOCK) {
            readPreferences();
            preferencesCache.remove(key);
            writePreferences();
        }
    }

    public static Map<String, Object> getAll() {
        return Collections.unmodifiableMap(readPreferences());
    }

    // 窗口状态专用方法
    public static void saveWindowState(int width, int height, int extendedState) {
        synchronized (LOCK) {
            readPreferences();
            preferencesCache.put(KEY_WINDOW_WIDTH, width);
            preferencesCache.put(KEY_WINDOW_HEIGHT, height);
            preferencesCache.put(KEY_WINDOW_EXTENDED_STATE, extendedState);
            writePreferences();
        }
    }


    public static Integer getWindowWidth() {
        return getInt(KEY_WINDOW_WIDTH);
    }

    public static Integer getWindowHeight() {
        return getInt(KEY_WINDOW_HEIGHT);
    }

    public static Integer getWindowExtendedState() {
        return getInt(KEY_WINDOW_EXTENDED_STATE);
    }

    public static boolean hasWindowState() {
        return getWindowWidth() != null && getWindowHeight() != null;
    }

    // 语言设置专用方法
    public static void saveLanguage(String language) {
        put(KEY_LANGUAGE, language);
    }

    public static String getLanguage() {
        return getString(KEY_LANGUAGE);
    }
}
