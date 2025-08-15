package com.laker.postman.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UserSettingsUtil {
    private static final String KEY_LAST_OPEN_REQUEST_ID = "lastOpenRequestId";
    private static final String KEY_SIDEBAR_EXPANDED = "sidebarExpanded";
    private static final String SETTINGS_PATH = SystemUtil.getUserHomeEasyPostmanPath() + "user_settings.json";
    private static final Object lock = new Object();
    private static Map<String, Object> settingsCache = null;

    private UserSettingsUtil() {
        // 私有构造函数，禁止实例化
    }

    private static Map<String, Object> readSettings() {
        synchronized (lock) {
            if (settingsCache != null) return settingsCache;
            File file = new File(SETTINGS_PATH);
            if (!file.exists()) {
                settingsCache = new HashMap<>();
                return settingsCache;
            }
            try {
                String json = FileUtil.readString(file, StandardCharsets.UTF_8);
                if (json == null || json.isBlank()) {
                    settingsCache = new HashMap<>();
                } else {
                    settingsCache = JSONUtil.parseObj(json);
                }
            } catch (Exception e) {
                log.warn("读取用户设置失败", e);
                settingsCache = new HashMap<>();
            }
            return settingsCache;
        }
    }

    private static void saveSettings() {
        synchronized (lock) {
            try {
                String json = JSONUtil.toJsonPrettyStr(settingsCache);
                FileUtil.writeString(json, new File(SETTINGS_PATH), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("保存用户设置失败", e);
            }
        }
    }

    public static void set(String key, Object value) {
        synchronized (lock) {
            readSettings();
            settingsCache.put(key, value);
            saveSettings();
        }
    }

    public static Object get(String key) {
        return readSettings().get(key);
    }

    public static String getString(String key) {
        Object v = get(key);
        return v == null ? null : v.toString();
    }

    public static Boolean getBoolean(String key) {
        Object v = get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return null;
    }

    public static Integer getInt(String key) {
        Object v = get(key);
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof String) try {
            return Integer.parseInt((String) v);
        } catch (Exception ignore) {
        }
        return null;
    }

    public static void remove(String key) {
        synchronized (lock) {
            readSettings();
            settingsCache.remove(key);
            saveSettings();
        }
    }

    public static Map<String, Object> getAll() {
        return Collections.unmodifiableMap(readSettings());
    }


    // lastOpenRequestId 专用方法
    public static void saveLastOpenRequestId(String id) {
        set(KEY_LAST_OPEN_REQUEST_ID, id);
    }

    public static String getLastOpenRequestId() {
        return getString(KEY_LAST_OPEN_REQUEST_ID);
    }

    // sidebarExpanded 专用方法
    public static void saveSidebarExpanded(boolean expanded) {
        set(KEY_SIDEBAR_EXPANDED, expanded);
    }

    public static boolean isSidebarExpanded() {
        Boolean v = getBoolean(KEY_SIDEBAR_EXPANDED);
        return v != null ? v : true; // 默认展开
    }
}