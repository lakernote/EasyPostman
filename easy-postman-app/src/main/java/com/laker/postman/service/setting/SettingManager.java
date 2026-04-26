package com.laker.postman.service.setting;

import cn.hutool.json.JSONUtil;
import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.model.NotificationPosition;
import com.laker.postman.model.SidebarTab;
import com.laker.postman.model.TrustedCertificateEntry;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class SettingManager {
    private static final String CONFIG_FILE = ConfigPathConstants.EASY_POSTMAN_SETTINGS;
    private static final String SCRIPT_REMOTE_REQUIRE_ENABLED = "script_remote_require_enabled";
    private static final String SCRIPT_REMOTE_REQUIRE_ALLOW_HTTP = "script_remote_require_allow_http";
    private static final String SCRIPT_REMOTE_REQUIRE_ALLOWED_HOSTS = "script_remote_require_allowed_hosts";
    private static final String SCRIPT_REMOTE_REQUIRE_CONNECT_TIMEOUT_MS = "script_remote_require_connect_timeout_ms";
    private static final String SCRIPT_REMOTE_REQUIRE_READ_TIMEOUT_MS = "script_remote_require_read_timeout_ms";
    private static final String SCRIPT_REMOTE_REQUIRE_MAX_BYTES = "script_remote_require_max_bytes";
    private static final int DEFAULT_SCRIPT_REMOTE_CONNECT_TIMEOUT_MS = 3000;
    private static final int DEFAULT_SCRIPT_REMOTE_READ_TIMEOUT_MS = 5000;
    private static final int DEFAULT_SCRIPT_REMOTE_MAX_BYTES = 512 * 1024;
    private static final int DEFAULT_JMETER_SLOW_REQUEST_THRESHOLD_MS = 10000;
    private static final String AUTO_UPDATE_CHECK_ENABLED_KEY = "auto_update_check_enabled";
    private static final String AUTO_UPDATE_CHECK_FREQUENCY_KEY = "auto_update_check_frequency";
    private static final String LAST_UPDATE_CHECK_TIME_KEY = "last_update_check_time";
    private static final String UPDATE_SOURCE_PREFERENCE_KEY = "update_source_preference";
    private static final String JMETER_SLOW_REQUEST_THRESHOLD_KEY = "jmeter_slow_request_threshold";
    private static final Object SETTINGS_IO_LOCK = new Object();
    public static final String PROXY_MODE_MANUAL = "MANUAL";
    public static final String PROXY_MODE_SYSTEM = "SYSTEM";
    public static final String PROXY_TYPE_HTTP = "HTTP";
    public static final String PROXY_TYPE_SOCKS = "SOCKS";
    private static final Properties props = new Properties();

    // 私有构造函数，防止实例化
    private SettingManager() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    static {
        load();
        initializeNotificationPosition();
    }

    public static void load() {
        synchronized (SETTINGS_IO_LOCK) {
            props.clear();
            props.putAll(loadProperties(new File(CONFIG_FILE)));
        }
    }

    /**
     * 初始化通知位置设置
     */
    private static void initializeNotificationPosition() {
        try {
            NotificationPosition position = getNotificationPosition();
            NotificationUtil.setDefaultPosition(position);
        } catch (Exception e) {
            // 如果解析失败，使用默认值
            log.error("Error initializing notification position", e);
        }
    }

    public static void save() {
        synchronized (SETTINGS_IO_LOCK) {
            Properties merged = loadProperties(new File(CONFIG_FILE));
            merged.putAll(props);
            storeProperties(merged, new File(CONFIG_FILE));
            refreshInMemorySettings(merged);
        }
    }

    static Properties saveProperty(File configFile, String key, String value) {
        synchronized (SETTINGS_IO_LOCK) {
            Properties merged = loadProperties(configFile);
            if (value == null) {
                merged.remove(key);
            } else {
                merged.setProperty(key, value);
            }
            storeProperties(merged, configFile);
            return merged;
        }
    }

    private static void setAndSaveProperty(String key, String value) {
        updateAndSaveProperties(settings -> applyProperty(settings, key, value));
    }

    private static void updateAndSaveProperties(Consumer<Properties> updater) {
        synchronized (SETTINGS_IO_LOCK) {
            Properties merged = loadProperties(new File(CONFIG_FILE));
            merged.putAll(props);
            updater.accept(merged);
            storeProperties(merged, new File(CONFIG_FILE));
            refreshInMemorySettings(merged);
        }
    }

    private static void applyProperty(Properties settings, String key, String value) {
        if (value == null) {
            settings.remove(key);
        } else {
            settings.setProperty(key, value);
        }
    }

    private static void refreshInMemorySettings(Properties settings) {
        props.clear();
        props.putAll(settings);
    }

    private static Properties loadProperties(File file) {
        Properties loaded = new Properties();
        if (file == null || !file.exists()) {
            return loaded;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            loaded.load(fis);
        } catch (IOException e) {
            log.warn("Failed to load settings from {}", file, e);
        }
        return loaded;
    }

    private static void storeProperties(Properties properties, File file) {
        if (file == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("Failed to create settings directory: {}", parent);
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, "EasyPostman Settings");
        } catch (IOException e) {
            log.warn("Failed to save settings to {}", file, e);
        }
    }

    public static int getMaxBodySize() {
        String val = props.getProperty("max_body_size");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 100 * 1024;
            }
        }
        return 100 * 1024; // 默认100KB
    }

    public static void setMaxBodySize(int size) {
        setAndSaveProperty("max_body_size", String.valueOf(size));
    }

    public static int getRequestTimeout() {
        String val = props.getProperty("request_timeout");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0; // 默认不超时
    }

    public static void setRequestTimeout(int timeout) {
        setAndSaveProperty("request_timeout", String.valueOf(timeout));
    }

    public static int getMaxDownloadSize() {
        String val = props.getProperty("max_download_size");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0; // 0表示不限制
    }

    public static void setMaxDownloadSize(int size) {
        setAndSaveProperty("max_download_size", String.valueOf(size));
    }

    public static int getJmeterMaxIdleConnections() {
        String val = props.getProperty("jmeter_max_idle_connections");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 200;
            }
        }
        return 200;
    }

    public static void setJmeterMaxIdleConnections(int maxIdle) {
        setAndSaveProperty("jmeter_max_idle_connections", String.valueOf(maxIdle));
    }

    public static long getJmeterKeepAliveSeconds() {
        String val = props.getProperty("jmeter_keep_alive_seconds");
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                return 60L;
            }
        }
        return 60L;
    }

    public static void setJmeterKeepAliveSeconds(long seconds) {
        setAndSaveProperty("jmeter_keep_alive_seconds", String.valueOf(seconds));
    }

    public static int getJmeterMaxRequests() {
        String val = props.getProperty("jmeter_max_requests");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 1000;
            }
        }
        return 1000; //（压测场景需要更大值）
    }

    public static void setJmeterMaxRequests(int maxRequests) {
        setAndSaveProperty("jmeter_max_requests", String.valueOf(maxRequests));
    }

    public static int getJmeterMaxRequestsPerHost() {
        String val = props.getProperty("jmeter_max_requests_per_host");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 1000;
            }
        }
        return 1000; // 默认1000（压测场景需要更大值）
    }

    public static void setJmeterMaxRequestsPerHost(int maxRequestsPerHost) {
        setAndSaveProperty("jmeter_max_requests_per_host", String.valueOf(maxRequestsPerHost));
    }

    public static int getJmeterSlowRequestThreshold() {
        String val = props.getProperty(JMETER_SLOW_REQUEST_THRESHOLD_KEY);
        if (val != null) {
            try {
                return Math.max(0, Integer.parseInt(val));
            } catch (NumberFormatException e) {
                return DEFAULT_JMETER_SLOW_REQUEST_THRESHOLD_MS;
            }
        }
        return DEFAULT_JMETER_SLOW_REQUEST_THRESHOLD_MS;
    }

    public static void setJmeterSlowRequestThreshold(int thresholdMs) {
        setAndSaveProperty(JMETER_SLOW_REQUEST_THRESHOLD_KEY, String.valueOf(Math.max(0, thresholdMs)));
    }

    public static int getTrendSamplingIntervalSeconds() {
        String val = props.getProperty("trend_sampling_interval_seconds");
        if (val != null) {
            try {
                int interval = Integer.parseInt(val);
                // 限制范围：1-60秒
                return Math.max(1, Math.min(60, interval));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1; // 默认1秒
    }

    public static void setTrendSamplingIntervalSeconds(int seconds) {
        // 限制范围：1-60秒
        int interval = Math.max(1, Math.min(60, seconds));
        setAndSaveProperty("trend_sampling_interval_seconds", String.valueOf(interval));
    }

    public static boolean isPerformanceEventLoggingEnabled() {
        String val = props.getProperty("performance_event_logging_enabled");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return false; // 默认关闭事件日志（提高性能）
    }

    public static void setPerformanceEventLoggingEnabled(boolean enabled) {
        setAndSaveProperty("performance_event_logging_enabled", String.valueOf(enabled));
    }

    public static boolean isShowDownloadProgressDialog() {
        String val = props.getProperty("show_download_progress_dialog");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认开启
    }

    public static void setShowDownloadProgressDialog(boolean show) {
        setAndSaveProperty("show_download_progress_dialog", String.valueOf(show));
    }

    public static int getDownloadProgressDialogThreshold() {
        String val = props.getProperty("download_progress_dialog_threshold");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 100 * 1024 * 1024;
            }
        }
        return 100 * 1024 * 1024; // 默认100MB
    }

    public static void setDownloadProgressDialogThreshold(int threshold) {
        setAndSaveProperty("download_progress_dialog_threshold", String.valueOf(threshold));
    }

    public static boolean isFollowRedirects() {
        String val = props.getProperty("follow_redirects");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认自动重定向
    }

    public static void setFollowRedirects(boolean follow) {
        setAndSaveProperty("follow_redirects", String.valueOf(follow));
    }

    /**
     * 是否禁用 SSL 证书验证（通用请求设置）
     * 此设置应用于所有 HTTPS 请求，用于开发测试环境
     */
    public static boolean isRequestSslVerificationDisabled() {
        String val = props.getProperty("ssl_verification_enabled");
        if (val != null) {
            return !Boolean.parseBoolean(val);
        }
        return true; // 默认禁用 SSL 验证，提升开发测试体验
    }

    public static void setRequestSslVerificationDisabled(boolean disabled) {
        setAndSaveProperty("ssl_verification_enabled", String.valueOf(!disabled));
        // 清除客户端缓存以应用新的 SSL 设置
        OkHttpClientManager.clearClientCache();
    }

    /**
     * 获取默认协议（当URL没有协议时自动补全）
     * 支持的值：http, https
     */
    public static String getDefaultProtocol() {
        String val = props.getProperty("default_protocol");
        if (val != null && (val.equals("http") || val.equals("https"))) {
            return val;
        }
        return "http"; // 默认使用 http
    }

    public static void setDefaultProtocol(String protocol) {
        if (protocol != null && (protocol.equals("http") || protocol.equals("https"))) {
            setAndSaveProperty("default_protocol", protocol);
        }
    }

    public static boolean isRemoteJsRequireEnabled() {
        String val = props.getProperty(SCRIPT_REMOTE_REQUIRE_ENABLED);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return false;
    }

    public static void setRemoteJsRequireEnabled(boolean enabled) {
        setAndSaveProperty(SCRIPT_REMOTE_REQUIRE_ENABLED, String.valueOf(enabled));
    }

    public static boolean isInsecureRemoteJsRequireEnabled() {
        String val = props.getProperty(SCRIPT_REMOTE_REQUIRE_ALLOW_HTTP);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return false;
    }

    public static void setInsecureRemoteJsRequireEnabled(boolean enabled) {
        setAndSaveProperty(SCRIPT_REMOTE_REQUIRE_ALLOW_HTTP, String.valueOf(enabled));
    }

    public static String getRemoteJsRequireAllowedHosts() {
        String val = props.getProperty(SCRIPT_REMOTE_REQUIRE_ALLOWED_HOSTS);
        return val != null ? val.trim() : "";
    }

    public static void setRemoteJsRequireAllowedHosts(String hosts) {
        setAndSaveProperty(SCRIPT_REMOTE_REQUIRE_ALLOWED_HOSTS, hosts != null ? hosts.trim() : "");
    }

    public static int getRemoteJsRequireConnectTimeoutMs() {
        return getPositiveIntSetting(SCRIPT_REMOTE_REQUIRE_CONNECT_TIMEOUT_MS, DEFAULT_SCRIPT_REMOTE_CONNECT_TIMEOUT_MS);
    }

    public static void setRemoteJsRequireConnectTimeoutMs(int timeoutMs) {
        setAndSaveProperty(SCRIPT_REMOTE_REQUIRE_CONNECT_TIMEOUT_MS, String.valueOf(Math.max(1, timeoutMs)));
    }

    public static int getRemoteJsRequireReadTimeoutMs() {
        return getPositiveIntSetting(SCRIPT_REMOTE_REQUIRE_READ_TIMEOUT_MS, DEFAULT_SCRIPT_REMOTE_READ_TIMEOUT_MS);
    }

    public static void setRemoteJsRequireReadTimeoutMs(int timeoutMs) {
        setAndSaveProperty(SCRIPT_REMOTE_REQUIRE_READ_TIMEOUT_MS, String.valueOf(Math.max(1, timeoutMs)));
    }

    public static int getRemoteJsRequireMaxBytes() {
        return getPositiveIntSetting(SCRIPT_REMOTE_REQUIRE_MAX_BYTES, DEFAULT_SCRIPT_REMOTE_MAX_BYTES);
    }

    public static void setRemoteJsRequireMaxBytes(int maxBytes) {
        setAndSaveProperty(SCRIPT_REMOTE_REQUIRE_MAX_BYTES, String.valueOf(Math.max(1, maxBytes)));
    }

    /**
     * 是否启用自定义受信任证书或 truststore。
     */
    public static boolean isCustomTrustMaterialEnabled() {
        String val = props.getProperty("custom_trust_material_enabled");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return false;
    }

    public static void setCustomTrustMaterialEnabled(boolean enabled) {
        setAndSaveProperty("custom_trust_material_enabled", String.valueOf(enabled));
        OkHttpClientManager.clearClientCache();
    }

    public static List<TrustedCertificateEntry> getCustomTrustMaterialEntries() {
        String json = props.getProperty("custom_trust_material_entries");
        if (json != null && !json.trim().isEmpty()) {
            try {
                List<TrustedCertificateEntry> entries = JSONUtil.toList(
                        JSONUtil.parseArray(json),
                        TrustedCertificateEntry.class
                );
                if (entries != null) {
                    return sanitizeTrustedCertificateEntries(entries);
                }
            } catch (Exception e) {
                log.warn("Failed to parse custom trust material entries, falling back to legacy settings", e);
            }
        }

        String legacyPath = props.getProperty("custom_trust_material_path", "").trim();
        if (legacyPath.isEmpty()) {
            return new ArrayList<>();
        }

        TrustedCertificateEntry entry = new TrustedCertificateEntry();
        entry.setEnabled(true);
        entry.setPath(legacyPath);
        entry.setPassword(props.getProperty("custom_trust_material_password", ""));
        List<TrustedCertificateEntry> entries = new ArrayList<>();
        entries.add(entry);
        return entries;
    }

    public static void setCustomTrustMaterialEntries(List<TrustedCertificateEntry> entries) {
        List<TrustedCertificateEntry> sanitizedEntries = sanitizeTrustedCertificateEntries(entries);
        updateAndSaveProperties(settings -> {
            if (sanitizedEntries.isEmpty()) {
                settings.remove("custom_trust_material_entries");
                settings.setProperty("custom_trust_material_path", "");
                settings.setProperty("custom_trust_material_password", "");
            } else {
                settings.setProperty("custom_trust_material_entries", JSONUtil.toJsonStr(sanitizedEntries));
                TrustedCertificateEntry firstEntry = sanitizedEntries.get(0);
                settings.setProperty("custom_trust_material_path", firstEntry.getPath());
                settings.setProperty("custom_trust_material_password", firstEntry.getPassword() != null ? firstEntry.getPassword() : "");
            }
        });
        OkHttpClientManager.clearClientCache();
    }

    /**
     * 自定义受信任证书或 truststore 文件路径。
     */
    public static String getCustomTrustMaterialPath() {
        List<TrustedCertificateEntry> entries = getCustomTrustMaterialEntries();
        if (!entries.isEmpty()) {
            return entries.get(0).getPath();
        }
        String val = props.getProperty("custom_trust_material_path");
        return val != null ? val : "";
    }

    public static void setCustomTrustMaterialPath(String path) {
        List<TrustedCertificateEntry> entries = getCustomTrustMaterialEntries();
        if (entries.isEmpty()) {
            TrustedCertificateEntry entry = new TrustedCertificateEntry();
            entry.setPath(path != null ? path : "");
            entries.add(entry);
        } else {
            entries.get(0).setPath(path != null ? path : "");
        }
        setCustomTrustMaterialEntries(entries);
    }

    /**
     * 自定义 truststore 密码。仅对 JKS/PKCS12 文件有效。
     */
    public static String getCustomTrustMaterialPassword() {
        List<TrustedCertificateEntry> entries = getCustomTrustMaterialEntries();
        if (!entries.isEmpty()) {
            return entries.get(0).getPassword();
        }
        String val = props.getProperty("custom_trust_material_password");
        return val != null ? val : "";
    }

    public static void setCustomTrustMaterialPassword(String password) {
        List<TrustedCertificateEntry> entries = getCustomTrustMaterialEntries();
        if (entries.isEmpty()) {
            TrustedCertificateEntry entry = new TrustedCertificateEntry();
            entry.setPassword(password != null ? password : "");
            entries.add(entry);
        } else {
            entries.get(0).setPassword(password != null ? password : "");
        }
        setCustomTrustMaterialEntries(entries);
    }

    private static List<TrustedCertificateEntry> sanitizeTrustedCertificateEntries(List<TrustedCertificateEntry> entries) {
        List<TrustedCertificateEntry> sanitizedEntries = new ArrayList<>();
        if (entries == null) {
            return sanitizedEntries;
        }

        for (TrustedCertificateEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            TrustedCertificateEntry sanitized = new TrustedCertificateEntry();
            sanitized.setEnabled(entry.isEnabled());
            sanitized.setPath(entry.getPath() != null ? entry.getPath().trim() : "");
            sanitized.setPassword(entry.getPassword() != null ? entry.getPassword() : "");
            if (sanitized.hasUsablePath()) {
                sanitizedEntries.add(sanitized);
            }
        }
        return sanitizedEntries;
    }

    public static int getMaxHistoryCount() {
        String val = props.getProperty("max_history_count");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 100;
            }
        }
        return 100; // 默认保存100条历史记录
    }

    public static void setMaxHistoryCount(int count) {
        setAndSaveProperty("max_history_count", String.valueOf(count));
    }

    public static int getMaxOpenedRequestsCount() {
        String val = props.getProperty("max_opened_requests_count");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 10;
            }
        }
        return 10;
    }

    public static void setMaxOpenedRequestsCount(int count) {
        setAndSaveProperty("max_opened_requests_count", String.valueOf(count));
    }

    /**
     * 是否根据响应类型自动格式化响应体
     */
    public static boolean isAutoFormatResponse() {
        String val = props.getProperty("auto_format_response");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认自动格式化，提升用户体验
    }

    public static void setAutoFormatResponse(boolean autoFormat) {
        setAndSaveProperty("auto_format_response", String.valueOf(autoFormat));
    }

    /**
     * 是否在启动时显示欢迎画面
     */
    public static boolean isStartupSplashEnabled() {
        String val = props.getProperty("startup_splash_enabled");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认开启
    }

    public static void setStartupSplashEnabled(boolean enabled) {
        setAndSaveProperty("startup_splash_enabled", String.valueOf(enabled));
    }

    /**
     * 是否默认展开侧边栏
     */
    public static boolean isSidebarExpanded() {
        String val = props.getProperty("sidebar_expanded");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return false; // 默认不展开
    }

    public static void setSidebarExpanded(boolean expanded) {
        setAndSaveProperty("sidebar_expanded", String.valueOf(expanded));
    }

    public static List<String> getSidebarTabOrder() {
        String val = props.getProperty("sidebar_tab_order");
        List<String> order = new ArrayList<>();
        if (val == null || val.isBlank()) {
            return order;
        }

        for (String token : val.split(",")) {
            String normalized = token == null ? null : token.trim();
            if (normalized != null && !normalized.isEmpty()) {
                order.add(normalized);
            }
        }
        return order;
    }

    public static void setSidebarTabOrder(Collection<String> tabOrder) {
        setAndSaveProperty("sidebar_tab_order", tabOrder == null || tabOrder.isEmpty() ? null : String.join(",", tabOrder));
    }

    public static Set<String> getHiddenSidebarTabs() {
        String val = props.getProperty("sidebar_hidden_tabs");
        Set<String> hiddenTabs = new LinkedHashSet<>();
        if (val == null || val.isBlank()) {
            return hiddenTabs;
        }

        for (String token : val.split(",")) {
            String normalized = token == null ? null : token.trim();
            if (normalized != null && !normalized.isEmpty()) {
                hiddenTabs.add(normalized.toUpperCase());
            }
        }
        return hiddenTabs;
    }

    public static void setHiddenSidebarTabs(Collection<String> hiddenTabs) {
        setAndSaveProperty("sidebar_hidden_tabs", hiddenTabs == null || hiddenTabs.isEmpty() ? null : String.join(",", hiddenTabs));
    }

    public static List<SidebarTab> getOrderedSidebarTabs() {
        return SidebarTab.resolveOrderedTabs(getSidebarTabOrder());
    }

    public static List<SidebarTab> getVisibleSidebarTabs() {
        return SidebarTab.resolveVisibleTabs(getSidebarTabOrder(), getHiddenSidebarTabs());
    }

    /**
     * 获取布局方向（true=垂直，false=水平）
     */
    public static boolean isLayoutVertical() {
        String val = props.getProperty("layout_vertical");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认垂直布局
    }

    public static void setLayoutVertical(boolean vertical) {
        setAndSaveProperty("layout_vertical", String.valueOf(vertical));
    }

    /**
     * 获取通知位置
     */
    public static NotificationPosition getNotificationPosition() {
        String val = props.getProperty("notification_position");
        if (val != null) {
            return NotificationPosition.fromName(val);
        }
        return NotificationPosition.BOTTOM_RIGHT; // 默认右下角
    }

    public static void setNotificationPosition(NotificationPosition position) {
        setAndSaveProperty("notification_position", position.name());
    }

    // ===== 自动更新设置 =====

    /**
     * 是否启用自动检查更新
     */
    public static boolean isAutoUpdateCheckEnabled() {
        String val = props.getProperty(AUTO_UPDATE_CHECK_ENABLED_KEY);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认开启
    }

    public static void setAutoUpdateCheckEnabled(boolean enabled) {
        setAndSaveProperty(AUTO_UPDATE_CHECK_ENABLED_KEY, String.valueOf(enabled));
    }

    /**
     * 获取更新检查频率
     * 支持的值：startup（每次启动）、daily（每日）、weekly（每周）、monthly（每月）
     */
    public static String getAutoUpdateCheckFrequency() {
        String val = props.getProperty(AUTO_UPDATE_CHECK_FREQUENCY_KEY);
        if (val != null && (val.equals("startup") || val.equals("daily") || val.equals("weekly") || val.equals("monthly"))) {
            return val;
        }
        return "daily"; // 默认每日
    }

    public static void setAutoUpdateCheckFrequency(String frequency) {
        String normalized = frequency == null ? "" : frequency.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("startup") || normalized.equals("daily") || normalized.equals("weekly") || normalized.equals("monthly")) {
            setAndSaveProperty(AUTO_UPDATE_CHECK_FREQUENCY_KEY, normalized);
        }
    }

    /**
     * 获取上次检查更新的时间戳（毫秒）
     */
    public static long getLastUpdateCheckTime() {
        String val = props.getProperty(LAST_UPDATE_CHECK_TIME_KEY);
        if (val != null) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L; // 0表示从未检查过
    }

    /**
     * 设置上次检查更新的时间戳（毫秒）
     */
    public static void setLastUpdateCheckTime(long timestamp) {
        setAndSaveProperty(LAST_UPDATE_CHECK_TIME_KEY, String.valueOf(timestamp));
    }

    /**
     * 更新源偏好设置
     * 支持的值：
     * - "auto": 自动选择最快的源（默认）
     * - "github": 始终使用 GitHub
     * - "gitee": 始终使用 Gitee
     */
    public static String getUpdateSourcePreference() {
        String val = props.getProperty(UPDATE_SOURCE_PREFERENCE_KEY);
        if (val != null && (val.equals("github") || val.equals("gitee") || val.equals("auto"))) {
            return val;
        }
        // 默认自动选择
        return "auto";
    }

    public static void setUpdateSourcePreference(String preference) {
        String normalized = preference == null ? "" : preference.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("auto") || normalized.equals("github") || normalized.equals("gitee")) {
            setAndSaveProperty(UPDATE_SOURCE_PREFERENCE_KEY, normalized);
            log.info("Update source preference saved: {}", normalized);
        }
    }

    // ===== 网络代理设置 =====

    /**
     * 是否启用网络代理
     */
    public static boolean isProxyEnabled() {
        String val = props.getProperty("proxy_enabled");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return false; // 默认关闭代理
    }

    public static void setProxyEnabled(boolean enabled) {
        setAndSaveProperty("proxy_enabled", String.valueOf(enabled));
    }

    /**
     * 代理模式：MANUAL 或 SYSTEM
     */
    public static String getProxyMode() {
        String val = props.getProperty("proxy_mode");
        if (PROXY_MODE_SYSTEM.equalsIgnoreCase(val)) {
            return PROXY_MODE_SYSTEM;
        }
        return PROXY_MODE_MANUAL;
    }

    public static void setProxyMode(String mode) {
        setAndSaveProperty("proxy_mode", PROXY_MODE_SYSTEM.equalsIgnoreCase(mode) ? PROXY_MODE_SYSTEM : PROXY_MODE_MANUAL);
    }

    public static boolean isSystemProxyMode() {
        return PROXY_MODE_SYSTEM.equalsIgnoreCase(getProxyMode());
    }

    public static boolean isManualProxyMode() {
        return PROXY_MODE_MANUAL.equalsIgnoreCase(getProxyMode());
    }

    public static boolean isManualProxyModeValue(String mode) {
        return PROXY_MODE_MANUAL.equalsIgnoreCase(mode);
    }

    /**
     * 代理类型：HTTP 或 SOCKS
     */
    public static String getProxyType() {
        String val = props.getProperty("proxy_type");
        if (PROXY_TYPE_SOCKS.equalsIgnoreCase(val)) {
            return PROXY_TYPE_SOCKS;
        }
        return PROXY_TYPE_HTTP; // 默认HTTP代理
    }

    public static void setProxyType(String type) {
        setAndSaveProperty("proxy_type", normalizeProxyType(type));
    }

    public static String normalizeProxyType(String type) {
        return PROXY_TYPE_SOCKS.equalsIgnoreCase(type) ? PROXY_TYPE_SOCKS : PROXY_TYPE_HTTP;
    }

    /**
     * 代理服务器地址
     */
    public static String getProxyHost() {
        String val = props.getProperty("proxy_host");
        if (val != null) {
            return val;
        }
        return ""; // 默认为空
    }

    public static void setProxyHost(String host) {
        setAndSaveProperty("proxy_host", host);
    }

    /**
     * 代理服务器端口
     */
    public static int getProxyPort() {
        String val = props.getProperty("proxy_port");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 8080;
            }
        }
        return 8080; // 默认8080端口
    }

    public static String getProxyPortText() {
        String val = props.getProperty("proxy_port");
        return val != null ? val : "";
    }

    public static void setProxyPort(int port) {
        setAndSaveProperty("proxy_port", String.valueOf(port));
    }

    /**
     * 代理用户名
     */
    public static String getProxyUsername() {
        String val = props.getProperty("proxy_username");
        if (val != null) {
            return val;
        }
        return ""; // 默认为空
    }

    public static void setProxyUsername(String username) {
        setAndSaveProperty("proxy_username", username);
    }

    /**
     * 代理密码
     */
    public static String getProxyPassword() {
        String val = props.getProperty("proxy_password");
        if (val != null) {
            return val;
        }
        return ""; // 默认为空
    }

    public static void setProxyPassword(String password) {
        setAndSaveProperty("proxy_password", password);
    }


    /**
     * 是否禁用 SSL 证书验证（代理环境专用设置）
     * 此设置专门用于解决代理环境下的 SSL 证书验证问题
     */
    public static boolean isProxySslVerificationDisabled() {
        String val = props.getProperty("proxy_ssl_verification_disabled");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认禁用代理 SSL 验证，避免代理环境阻断请求
    }

    public static void setProxySslVerificationDisabled(boolean disabled) {
        setAndSaveProperty("proxy_ssl_verification_disabled", String.valueOf(disabled));
        // 清除客户端缓存以应用新的 SSL 设置
        OkHttpClientManager.clearClientCache();
    }

    // ===== UI 字体设置 =====

    /**
     * 获取UI字体名称
     */
    public static String getUiFontName() {
        String val = props.getProperty("ui_font_name");
        if (val != null && !val.isEmpty()) {
            return val;
        }
        // 默认使用系统字体
        return ""; // 空字符串表示使用系统默认字体
    }

    public static void setUiFontName(String fontName) {
        setAndSaveProperty("ui_font_name", fontName != null ? fontName : "");
    }

    /**
     * 获取UI字体大小
     */
    public static int getUiFontSize() {
        String val = props.getProperty("ui_font_size");
        if (val != null) {
            try {
                int size = Integer.parseInt(val);
                // 限制范围：10-24
                return Math.max(10, Math.min(24, size));
            } catch (NumberFormatException e) {
                return getDefaultFontSize();
            }
        }
        return getDefaultFontSize(); // 根据操作系统返回默认字体大小
    }

    /**
     * 获取默认字体大小
     * 所有平台统一使用 13 号字体
     */
    private static int getDefaultFontSize() {
        return 13; // 所有平台统一默认 13号
    }

    public static void setUiFontSize(int size) {
        // 限制范围：10-24
        int fontSize = Math.max(10, Math.min(24, size));
        setAndSaveProperty("ui_font_size", String.valueOf(fontSize));
    }

    private static int getPositiveIntSetting(String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val != null) {
            try {
                int parsed = Integer.parseInt(val);
                return parsed > 0 ? parsed : defaultValue;
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
