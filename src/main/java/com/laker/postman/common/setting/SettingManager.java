package com.laker.postman.common.setting;

import com.laker.postman.util.SystemUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SettingManager {
    private static final String CONFIG_FILE = SystemUtil.getUserHomeEasyPostmanPath() + "easy_postman_settings.properties";
    private static final Properties props = new Properties();

    static {
        load();
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
        return 0;
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
}