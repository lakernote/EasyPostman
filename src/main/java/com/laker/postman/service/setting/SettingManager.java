package com.laker.postman.service.setting;

import com.laker.postman.model.NotificationPosition;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class SettingManager {
    private static final String CONFIG_FILE = ConfigPathConstants.EASY_POSTMAN_SETTINGS;
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
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                // ignore
            }
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
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "EasyPostman Settings");
        } catch (IOException e) {
            // ignore
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
        return 100 * 1024;
    }

    public static void setMaxBodySize(int size) {
        props.setProperty("max_body_size", String.valueOf(size));
        save();
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
        props.setProperty("request_timeout", String.valueOf(timeout));
        save();
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
        props.setProperty("max_download_size", String.valueOf(size));
        save();
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
        props.setProperty("jmeter_max_idle_connections", String.valueOf(maxIdle));
        save();
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
        props.setProperty("jmeter_keep_alive_seconds", String.valueOf(seconds));
        save();
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
        props.setProperty("jmeter_max_requests", String.valueOf(maxRequests));
        save();
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
        props.setProperty("jmeter_max_requests_per_host", String.valueOf(maxRequestsPerHost));
        save();
    }

    public static int getJmeterSlowRequestThreshold() {
        String val = props.getProperty("jmeter_slow_request_threshold");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 3000;
            }
        }
        return 3000;
    }

    public static void setJmeterSlowRequestThreshold(int thresholdMs) {
        props.setProperty("jmeter_slow_request_threshold", String.valueOf(thresholdMs));
        save();
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
        props.setProperty("trend_sampling_interval_seconds", String.valueOf(interval));
        save();
    }

    public static boolean isPerformanceEventLoggingEnabled() {
        String val = props.getProperty("performance_event_logging_enabled");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return false; // 默认关闭事件日志（提高性能）
    }

    public static void setPerformanceEventLoggingEnabled(boolean enabled) {
        props.setProperty("performance_event_logging_enabled", String.valueOf(enabled));
        save();
    }

    public static boolean isShowDownloadProgressDialog() {
        String val = props.getProperty("show_download_progress_dialog");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认开启
    }

    public static void setShowDownloadProgressDialog(boolean show) {
        props.setProperty("show_download_progress_dialog", String.valueOf(show));
        save();
    }

    public static int getDownloadProgressDialogThreshold() {
        String val = props.getProperty("download_progress_dialog_threshold");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 5 * 1024 * 1024;
            }
        }
        return 5 * 1024 * 1024; // 默认5MB
    }

    public static void setDownloadProgressDialogThreshold(int threshold) {
        props.setProperty("download_progress_dialog_threshold", String.valueOf(threshold));
        save();
    }

    public static boolean isFollowRedirects() {
        String val = props.getProperty("follow_redirects");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认自动重定向
    }

    public static void setFollowRedirects(boolean follow) {
        props.setProperty("follow_redirects", String.valueOf(follow));
        save();
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
        return true;
    }

    public static void setRequestSslVerificationDisabled(boolean disabled) {
        props.setProperty("ssl_verification_enabled", String.valueOf(!disabled));
        save();
        // 清除客户端缓存以应用新的 SSL 设置
        OkHttpClientManager.clearClientCache();
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
        props.setProperty("max_history_count", String.valueOf(count));
        save();
    }

    public static int getMaxOpenedRequestsCount() {
        String val = props.getProperty("max_opened_requests_count");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 20;
            }
        }
        return 20;
    }

    public static void setMaxOpenedRequestsCount(int count) {
        props.setProperty("max_opened_requests_count", String.valueOf(count));
        save();
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
        props.setProperty("auto_format_response", String.valueOf(autoFormat));
        save();
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
        props.setProperty("sidebar_expanded", String.valueOf(expanded));
        save();
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
        props.setProperty("layout_vertical", String.valueOf(vertical));
        save();
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
        props.setProperty("notification_position", position.name());
        save();
    }

    // ===== 自动更新设置 =====

    /**
     * 是否启用自动检查更新
     */
    public static boolean isAutoUpdateCheckEnabled() {
        String val = props.getProperty("auto_update_check_enabled");
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return true; // 默认开启
    }

    public static void setAutoUpdateCheckEnabled(boolean enabled) {
        props.setProperty("auto_update_check_enabled", String.valueOf(enabled));
        save();
    }

    /**
     * 获取更新检查频率
     * 支持的值：startup（每次启动）、daily（每日）、weekly（每周）、monthly（每月）
     */
    public static String getAutoUpdateCheckFrequency() {
        String val = props.getProperty("auto_update_check_frequency");
        if (val != null && (val.equals("startup") || val.equals("daily") || val.equals("weekly") || val.equals("monthly"))) {
            return val;
        }
        return "daily"; // 默认每日
    }

    public static void setAutoUpdateCheckFrequency(String frequency) {
        if (frequency != null && (frequency.equals("startup") || frequency.equals("daily") || frequency.equals("weekly") || frequency.equals("monthly"))) {
            props.setProperty("auto_update_check_frequency", frequency);
            save();
        }
    }

    /**
     * 获取上次检查更新的时间戳（毫秒）
     */
    public static long getLastUpdateCheckTime() {
        String val = props.getProperty("last_update_check_time");
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
        props.setProperty("last_update_check_time", String.valueOf(timestamp));
        save();
    }

    /**
     * 更新源偏好设置
     * 支持的值：
     * - "auto": 自动选择最快的源（默认）
     * - "github": 始终使用 GitHub
     * - "gitee": 始终使用 Gitee
     */
    public static String getUpdateSourcePreference() {
        String val = props.getProperty("update_source_preference");
        if (val != null && (val.equals("github") || val.equals("gitee") || val.equals("auto"))) {
            return val;
        }
        // 默认自动选择
        return "auto";
    }

    public static void setUpdateSourcePreference(String preference) {
        if (preference != null && (preference.equals("auto") || preference.equals("github") || preference.equals("gitee"))) {
            props.setProperty("update_source_preference", preference);
            save();
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
        return false; // 默认不启用
    }

    public static void setProxyEnabled(boolean enabled) {
        props.setProperty("proxy_enabled", String.valueOf(enabled));
        save();
    }

    /**
     * 代理类型：HTTP 或 SOCKS
     */
    public static String getProxyType() {
        String val = props.getProperty("proxy_type");
        if (val != null) {
            return val;
        }
        return "HTTP"; // 默认HTTP代理
    }

    public static void setProxyType(String type) {
        props.setProperty("proxy_type", type);
        save();
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
        props.setProperty("proxy_host", host);
        save();
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

    public static void setProxyPort(int port) {
        props.setProperty("proxy_port", String.valueOf(port));
        save();
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
        props.setProperty("proxy_username", username);
        save();
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
        props.setProperty("proxy_password", password);
        save();
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
        return false; // 默认启用 SSL 验证
    }

    public static void setProxySslVerificationDisabled(boolean disabled) {
        props.setProperty("proxy_ssl_verification_disabled", String.valueOf(disabled));
        save();
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
        props.setProperty("ui_font_name", fontName != null ? fontName : "");
        save();
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
        props.setProperty("ui_font_size", String.valueOf(fontSize));
        save();
    }
}