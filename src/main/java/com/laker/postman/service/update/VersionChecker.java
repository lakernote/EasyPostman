package com.laker.postman.service.update;

import cn.hutool.core.text.CharSequenceUtil;
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

        if (osName.contains("win")) {
            // Windows 特殊处理：区分便携版和安装版
            return getWindowsDownloadUrl(assets);
        } else if (osName.contains("mac")) {
            // macOS 特殊处理：支持新版本的架构特定 DMG 和旧版本的通用 DMG
            return getMacDownloadUrl(assets);
        } else if (osName.contains("linux")) {
            return findAssetByExtension(assets, ".deb");
        } else {
            log.warn("Unsupported OS: {}", osName);
            return null;
        }
    }

    /**
     * 获取 Windows 下载链接（区分便携版和安装版）
     */
    private String getWindowsDownloadUrl(JSONArray assets) {
        // 检测当前运行的是便携版还是安装版
        boolean isPortable = isWindowsPortableVersion();

        if (isPortable) {
            // 便携版：优先下载便携版 ZIP
            log.info("Detected portable version, looking for -portable.zip");
            String portableUrl = findAssetByPattern(assets, "-portable.zip");

            if (portableUrl != null) {
                log.info("Found portable ZIP for update");
                return portableUrl;
            }

            // 如果没有便携版，提示用户手动下载（不自动降级到 MSI）
            log.warn("Portable ZIP not found in release, update not available for portable version");
            return null;
        } else {
            // 安装版：下载 MSI
            log.info("Detected installed version, looking for .msi");
            return findAssetByExtension(assets, ".msi");
        }
    }

    /**
     * 检测是否为 Windows 便携版
     * 判断依据：
     * 1. 检查应用根目录是否存在 .portable 标识文件（绿色版特征，优先级最高）
     * 2. 检查是否从 Program Files 或 AppData 目录运行（安装版特征）
     * 3. 默认认为是安装版
     */
    private boolean isWindowsPortableVersion() {
        try {
            // 获取当前 JAR 文件路径
            String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = java.net.URLDecoder.decode(jarPath, java.nio.charset.StandardCharsets.UTF_8);

            log.info("Current JAR path: {}", decodedPath);

            // 获取应用根目录（JAR 文件的父目录或上级目录）
            java.io.File jarFile = new java.io.File(decodedPath);
            java.io.File appDir = jarFile.getParentFile();

            if (appDir == null) {
                log.warn("Cannot determine application directory");
            } else {
                // 1. 优先检查 .portable 标识文件（release.yml 中创建的标识文件）
                java.io.File portableMarker = new java.io.File(appDir, ".portable");
                if (portableMarker.exists() && portableMarker.isFile()) {
                    log.info("Found .portable marker file, confirmed portable version");
                    return true;
                }

                // 检查上级目录（可能 JAR 在 app 子目录中）
                java.io.File parentDir = appDir.getParentFile();
                if (parentDir != null) {
                    portableMarker = new java.io.File(parentDir, ".portable");
                    if (portableMarker.exists() && portableMarker.isFile()) {
                        log.info("Found .portable marker file in parent directory, confirmed portable version");
                        return true;
                    }
                }
            }

            // 2. 检查安装路径特征
            String lowerPath = decodedPath.toLowerCase();
            if (lowerPath.contains("program files") ||
                    lowerPath.contains("appdata\\local\\programs") ||
                    lowerPath.contains("appdata/local/programs")) {
                log.info("Running from Program Files or AppData, detected as installed version");
                return false;
            }

            // 3. 默认认为是安装版
            log.info("No .portable marker found and not in typical installation directory, assuming installed version");
            return false;

        } catch (Exception e) {
            log.warn("Failed to detect Windows version type: {}", e.getMessage());
            // 检测失败时默认为安装版
            return false;
        }
    }

    /**
     * 在 assets 中查找匹配模式的文件
     */
    private String findAssetByPattern(JSONArray assets, String pattern) {
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.contains(pattern)) {
                String url = asset.getStr("browser_download_url");
                log.debug("Found asset by pattern '{}': {} -> {}", pattern, name, url);
                return url;
            }
        }
        return null;
    }

    /**
     * 获取 macOS 下载链接（支持向后兼容）
     */
    private String getMacDownloadUrl(JSONArray assets) {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        boolean isAppleSilicon = arch.contains("aarch64") || arch.equals("arm64");

        // 1. 优先查找架构特定的 DMG（新版本）
        String archSpecificSuffix = getMacPackageSuffix();
        String downloadUrl = findAssetByExtension(assets, archSpecificSuffix);

        if (downloadUrl != null) {
            log.info("Found architecture-specific DMG: {}", archSpecificSuffix);
            return downloadUrl;
        }

        // 2. 回退到通用 .dmg（仅限 Apple Silicon，因为旧版本 .dmg 只支持 M 芯片）
        if (isAppleSilicon) {
            log.info("Architecture-specific DMG not found, trying generic .dmg for backward compatibility (Apple Silicon only)");
            downloadUrl = findGenericDmg(assets);

            if (downloadUrl != null) {
                log.info("Found generic DMG (legacy version, M chip compatible)");
                return downloadUrl;
            }
        } else {
            // Intel Mac 必须有 -intel.dmg 才能升级
            log.warn("Intel Mac requires -intel.dmg file, but not found in release assets");
            log.info("Note: Legacy .dmg files only support Apple Silicon (M chip)");
        }

        log.warn("No suitable DMG file found for current architecture");
        return null;
    }

    /**
     * 在 assets 中查找指定扩展名的文件
     */
    private String findAssetByExtension(JSONArray assets, String extension) {
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.endsWith(extension)) {
                String url = asset.getStr("browser_download_url");
                log.debug("Found asset: {} -> {}", name, url);
                return url;
            }
        }

        return null;
    }

    /**
     * 查找通用 DMG 文件（格式：EasyPostman-版本号.dmg，不包含架构后缀）
     * 用于向后兼容旧版本（旧版本 DMG 只支持 Apple Silicon）
     */
    private String findGenericDmg(JSONArray assets) {
        for (int i = 0; i < assets.size(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getStr("name");
            if (name != null && name.endsWith(".dmg")) {
                // 排除带有架构后缀的 DMG（-intel.dmg 和 -arm64.dmg）
                if (!name.endsWith("-intel.dmg") && !name.endsWith("-arm64.dmg")) {
                    String url = asset.getStr("browser_download_url");
                    log.debug("Found generic DMG (without architecture suffix): {} -> {}", name, url);
                    return url;
                }
            }
        }
        return null;
    }

    /**
     * 获取 macOS 包的后缀（根据芯片架构判断）
     */
    private String getMacPackageSuffix() {
        try {
            // 使用 Java 系统属性检测架构，避免执行系统命令（防止被杀毒软件误报）
            String arch = System.getProperty("os.arch", "").toLowerCase();
            log.debug("Detected macOS architecture via os.arch: {}", arch);

            // 检测 Apple Silicon (ARM64)
            if (arch.contains("aarch64") || arch.equals("arm64")) {
                log.info("Detected Apple Silicon (ARM64), using -arm64.dmg");
                return "-arm64.dmg";
            }

            // 检测 Intel (x86_64)
            if (arch.contains("x86_64") || arch.contains("amd64") || arch.equals("x64")) {
                log.info("Detected Intel chip (x86_64), using -intel.dmg");
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
