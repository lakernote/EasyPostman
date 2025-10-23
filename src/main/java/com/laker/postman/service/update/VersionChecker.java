package com.laker.postman.service.update;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 版本检查器 - 负责检查远程版本信息
 */
@Slf4j
public class VersionChecker {

    private static final String GITEE_API_URL = "https://gitee.com/api/v5/repos/lakernote/easy-postman/releases/latest";
    private static final String OS_RELEASE_FILE = "/etc/os-release";

    /**
     * 检查更新信息
     */
    public UpdateInfo checkForUpdate() {
        try {
            String currentVersion = SystemUtil.getCurrentVersion();
            JSONObject releaseInfo = fetchLatestReleaseInfo();

            if (releaseInfo == null) {
                return UpdateInfo.noUpdateAvailable(I18nUtil.getMessage(MessageKeys.UPDATE_FETCH_RELEASE_FAILED));
            }

            String latestVersion = releaseInfo.getStr("tag_name");
            log.info("Current version: {}, Gitee latest version: {}", currentVersion, latestVersion);
            if (latestVersion == null) {
                return UpdateInfo.noUpdateAvailable(I18nUtil.getMessage(MessageKeys.UPDATE_NO_VERSION_INFO));
            }

            if (compareVersion(latestVersion, currentVersion) > 0 && CharSequenceUtil.isNotBlank(getDownloadUrl(releaseInfo))) {
                return UpdateInfo.updateAvailable(currentVersion, latestVersion, releaseInfo);
            } else {
                return UpdateInfo.noUpdateAvailable(I18nUtil.getMessage(MessageKeys.UPDATE_ALREADY_LATEST, currentVersion));
            }

        } catch (Exception e) {
            log.debug("Failed to check for updates", e);
            return UpdateInfo.checkFailed(e.getMessage());
        }
    }

    /**
     * 获取最新版本发布信息
     */
    private JSONObject fetchLatestReleaseInfo() {
        try {
            URL url = new URL(GITEE_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            // 使用更通用的 User-Agent
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; EasyPostman/" + SystemUtil.getCurrentVersion() + ")");
            // 添加更多请求头来避免被拒绝
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Connection", "keep-alive");

            int code = conn.getResponseCode();
            if (code == 200) {
                try (InputStream is = conn.getInputStream();
                     Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
                    String json = scanner.useDelimiter("\\A").next();
                    return new JSONObject(json);
                }
            } else {
                // 尝试读取错误响应
                String errorResponse = "";
                try (InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        try (Scanner scanner = new Scanner(errorStream, StandardCharsets.UTF_8)) {
                            errorResponse = scanner.useDelimiter("\\A").next();
                        }
                    }
                } catch (Exception ignored) {
                    // 忽略读取错误响应的异常
                }

                log.warn("Failed to fetch release info, HTTP code: {}, response: {}", code, errorResponse);

                return null;
            }
        } catch (Exception e) {
            log.debug("Error fetching release info: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取下载链接
     */
    public String getDownloadUrl(JSONObject releaseInfo) {
        if (releaseInfo == null) {
            return null;
        }

        JSONArray assets = releaseInfo.getJSONArray("assets");
        if (assets == null || assets.isEmpty()) {
            log.info("Release assets is empty");
            return null;
        }

        String osName = System.getProperty("os.name").toLowerCase();
        String expectedSuffix;

        if (osName.contains("win")) {
            expectedSuffix = ".msi";
        } else if (osName.contains("mac")) {
            // macOS 根据芯片架构选择对应的 DMG
            expectedSuffix = getMacPackageSuffix();
        } else if (osName.contains("linux")) {
            // Linux 系统根据发行版选择包格式
            expectedSuffix = getLinuxPackageExtension();
        } else {
            log.warn("Unsupported OS: {}", osName);
            return null;
        }

        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.endsWith(expectedSuffix)) {
                return asset.getStr("browser_download_url");
            }
        }

        return null;
    }

    /**
     * 获取 macOS 包的后缀（根据芯片架构判断）
     */
    private String getMacPackageSuffix() {
        try {
            String arch = System.getProperty("os.arch").toLowerCase();
            log.debug("Detected macOS architecture: {}", arch);

            // 检测 Apple Silicon (ARM64)
            if (arch.contains("aarch64") || arch.equals("arm64")) {
                log.info("Detected Apple Silicon (ARM64), using -arm64.dmg");
                return "-arm64.dmg";
            }

            // 检测 Intel (x86_64)
            if (arch.contains("x86_64") || arch.contains("amd64")) {
                log.info("Detected Intel chip (x86_64), using -intel.dmg");
                return "-intel.dmg";
            }

            // 备用方案：使用 uname -m 命令检测
            String unameResult = RuntimeUtil.execForStr("uname -m").trim();
            log.debug("uname -m result: {}", unameResult);

            if ("arm64".equals(unameResult) || "aarch64".equals(unameResult)) {
                log.info("Detected Apple Silicon via uname, using -arm64.dmg");
                return "-arm64.dmg";
            } else if ("x86_64".equals(unameResult)) {
                log.info("Detected Intel via uname, using -intel.dmg");
                return "-intel.dmg";
            }

        } catch (Exception e) {
            log.warn("Failed to detect macOS architecture: {}", e.getMessage());
        }

        // 默认返回 ARM64 版本（因为新 Mac 都是 Apple Silicon）
        log.info("Unable to detect architecture, defaulting to -arm64.dmg");
        return "-arm64.dmg";
    }

    /**
     * 获取 Linux 包的扩展名（根据发行版判断）
     */
    private String getLinuxPackageExtension() {
        try {
            // 优先读取 /etc/os-release 文件判断发行版
            if (FileUtil.exist(OS_RELEASE_FILE)) {
                String content = FileUtil.readUtf8String(OS_RELEASE_FILE).toLowerCase();

                // 检测 Red Hat 系列发行版
                if (CharSequenceUtil.containsAny(content, "centos", "rhel", "fedora", "red hat")) {
                    log.debug("Detected RPM-based Linux distribution");
                    return ".rpm";
                }

                // 检测 Debian 系列发行版
                if (CharSequenceUtil.containsAny(content, "ubuntu", "debian")) {
                    log.debug("Detected DEB-based Linux distribution");
                    return ".deb";
                }
            }

            // 备用方案：检查包管理器命令是否存在
            if (commandExists("dpkg")) {
                log.debug("Detected dpkg command, using .deb");
                return ".deb";
            } else if (commandExists("rpm")) {
                log.debug("Detected rpm command, using .rpm");
                return ".rpm";
            }

        } catch (Exception e) {
            log.debug("Failed to detect Linux distribution: {}", e.getMessage());
        }

        // 默认返回 .deb（Ubuntu/Debian 使用更广泛）
        log.debug("Using default package format: .deb");
        return ".deb";
    }

    /**
     * 检查命令是否存在
     */
    private boolean commandExists(String command) {
        try {
            // 使用 Hutool 的 RuntimeUtil 执行命令
            String result = RuntimeUtil.execForStr("which " + command);
            return CharSequenceUtil.isNotBlank(result);
        } catch (Exception e) {
            log.debug("Command '{}' not found: {}", command, e.getMessage());
            return false;
        }
    }

    /**
     * 比较版本号
     */
    private int compareVersion(String v1, String v2) {
        if (v1 == null || v2 == null) return 0;

        String s1 = v1.startsWith("v") ? v1.substring(1) : v1;
        String s2 = v2.startsWith("v") ? v2.substring(1) : v2;

        String[] arr1 = s1.split("\\.");
        String[] arr2 = s2.split("\\.");
        int len = Math.max(arr1.length, arr2.length);

        for (int i = 0; i < len; i++) {
            int n1 = i < arr1.length ? parseIntSafe(arr1[i]) : 0;
            int n2 = i < arr2.length ? parseIntSafe(arr2[i]) : 0;
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        return 0;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
