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
import java.util.concurrent.CompletableFuture;

/**
 * 版本检查器 - 负责检查远程版本信息
 */
@Slf4j
public class VersionChecker {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/lakernote/easy-postman/releases/latest";
    private static final String GITEE_API_URL = "https://gitee.com/api/v5/repos/lakernote/easy-postman/releases/latest";

    private static final String SOURCE_GITHUB = "GitHub";
    private static final String SOURCE_GITEE = "Gitee";
    private static final String SOURCE_KEY = "_source";
    private static final String BROWSER_DOWNLOAD_URL = "browser_download_url";
    private static final String MACOS_ARM64_DMG = "-macos-arm64.dmg";
    private static final String PORTABLE_ZIP = "-portable.zip";

    // 缓存最佳源，避免重复检测
    private static volatile String cachedBestSource = null;

    /**
     * 检查更新信息
     */
    public UpdateInfo checkForUpdate() {
        try {
            String currentVersion = SystemUtil.getCurrentVersion();

            // 并行检测最快的源
            String bestSource = detectBestSource();
            JSONObject releaseInfo = fetchLatestReleaseInfo(bestSource);

            if (releaseInfo == null) {
                // 如果最佳源失败，尝试备用源
                String fallbackSource = bestSource.equals(GITHUB_API_URL) ? GITEE_API_URL : GITHUB_API_URL;
                log.info("Primary source failed, trying fallback source");
                releaseInfo = fetchLatestReleaseInfo(fallbackSource);

                if (releaseInfo != null) {
                    cachedBestSource = fallbackSource; // 更新缓存
                }
            }

            if (releaseInfo == null) {
                return UpdateInfo.noUpdateAvailable(I18nUtil.getMessage(MessageKeys.UPDATE_FETCH_RELEASE_FAILED));
            }

            String latestVersion = releaseInfo.getStr("tag_name");
            String sourceName = releaseInfo.getStr(SOURCE_KEY, "unknown");
            log.info("Current version: {}, Latest version: {} (from {})", currentVersion, latestVersion, sourceName);

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
     * 检测最佳源（GitHub 或 Gitee）
     * 策略：根据用户偏好设置或并行测试连接速度，选择最合适的源
     */
    private static String detectBestSource() {
        // 检查用户的更新源偏好设置
        String preference = com.laker.postman.panel.topmenu.setting.SettingManager.getUpdateSourcePreference();

        // 如果用户指定了固定源，直接使用
        if ("github".equals(preference)) {
            log.info("Using user-preferred source: GitHub");
            cachedBestSource = GITHUB_API_URL;
            return GITHUB_API_URL;
        } else if ("gitee".equals(preference)) {
            log.info("Using user-preferred source: Gitee");
            cachedBestSource = GITEE_API_URL;
            return GITEE_API_URL;
        }

        // auto 模式：自动选择最快的源
        // 如果已有缓存，直接使用
        if (cachedBestSource != null) {
            log.info("Using cached best source: {}", cachedBestSource.contains("github") ? SOURCE_GITHUB : SOURCE_GITEE);
            return cachedBestSource;
        }

        log.info("Auto-detecting best update source...");

        // 并行测试两个源的连接速度
        CompletableFuture<Long> githubTest = CompletableFuture.supplyAsync(() -> testSourceSpeed(GITHUB_API_URL, SOURCE_GITHUB));
        CompletableFuture<Long> giteeTest = CompletableFuture.supplyAsync(() -> testSourceSpeed(GITEE_API_URL, SOURCE_GITEE));

        try {
            // 等待两个测试完成，最多等待 6 秒（给予足够的时间完成 2.5 秒超时测试）
            CompletableFuture.allOf(githubTest, giteeTest).get(6, java.util.concurrent.TimeUnit.SECONDS);

            long githubTime = githubTest.getNow(Long.MAX_VALUE);
            long giteeTime = giteeTest.getNow(Long.MAX_VALUE);

            // 选择响应时间更短的源（优先选择 3 秒内响应的源）
            if (githubTime < giteeTime && githubTime < 3000) {
                log.info("GitHub is faster ({} ms vs {} ms), using GitHub", githubTime, giteeTime);
                cachedBestSource = GITHUB_API_URL;
                return GITHUB_API_URL;
            } else if (giteeTime < Long.MAX_VALUE) {
                log.info("Gitee is faster or preferred ({} ms vs {} ms), using Gitee", giteeTime, githubTime);
                cachedBestSource = GITEE_API_URL;
                return GITEE_API_URL;
            }

        } catch (java.util.concurrent.TimeoutException e) {
            log.debug("Source detection timeout: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (java.util.concurrent.ExecutionException e) {
            log.debug("Source detection execution error: {}", e.getMessage());
        } catch (InterruptedException e) {
            log.debug("Source detection interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }

        // 默认使用 Gitee（国内用户居多）
        log.info("Using default source: Gitee");
        cachedBestSource = GITEE_API_URL;
        return GITEE_API_URL;
    }

    /**
     * 测试源的连接速度（返回响应时间，单位：毫秒）
     * 注意：只测试连接和响应头，不下载完整内容，以节省带宽和提高测试准确性
     */
    private static long testSourceSpeed(String apiUrl, String sourceName) {
        long startTime = System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2500);  // 缩短超时时间到 2.5 秒
            conn.setReadTimeout(2500);
            conn.setRequestMethod("GET"); // 使用 GET 请求（Gitee API 不支持 HEAD）
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; EasyPostman/" + SystemUtil.getCurrentVersion() + ")");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Connection", "close"); // 明确告知服务器关闭连接

            // 获取响应码（此时会建立连接并接收响应头）
            int code = conn.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;

            // 不读取响应体，立即断开连接以节省带宽

            if (code == 200 || code == 301 || code == 302) {
                log.debug("{} connection test successful: {} ms (HTTP {})", sourceName, responseTime, code);
                return responseTime;
            } else {
                log.debug("{} connection test failed with code: {}", sourceName, code);
                return Long.MAX_VALUE;
            }
        } catch (java.net.SocketTimeoutException e) {
            long failedTime = System.currentTimeMillis() - startTime;
            log.debug("{} connection timeout after {} ms", sourceName, failedTime);
            return Long.MAX_VALUE;
        } catch (Exception e) {
            long failedTime = System.currentTimeMillis() - startTime;
            log.debug("{} connection test failed after {} ms: {}", sourceName, failedTime, e.getMessage());
            return Long.MAX_VALUE;
        } finally {
            if (conn != null) {
                try {
                    // 关闭连接，避免资源泄漏
                    conn.disconnect();
                } catch (Exception ignored) {
                    // 忽略断开连接时的异常
                }
            }
        }
    }

    /**
     * 获取最新版本发布信息
     */
    private JSONObject fetchLatestReleaseInfo(String apiUrl) {
        String sourceName = apiUrl.contains("github") ? SOURCE_GITHUB : SOURCE_GITEE;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            // 使用更通用的 User-Agent
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; EasyPostman/" + SystemUtil.getCurrentVersion() + ")");
            // 添加更多请求头来避免被拒绝
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Cache-Control", "no-cache");

            int code = conn.getResponseCode();
            if (code == 200) {
                try (InputStream is = conn.getInputStream();
                     Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
                    String json = scanner.useDelimiter("\\A").next();
                    JSONObject releaseInfo = new JSONObject(json);
                    // 标记数据来源
                    releaseInfo.set(SOURCE_KEY, sourceName);
                    log.info("Successfully fetched release info from {}", sourceName);
                    return releaseInfo;
                }
            } else {
                // 尝试读取错误响应
                String errorResponse = readErrorResponse(conn);
                log.warn("Failed to fetch release info from {}, HTTP code: {}, response: {}", sourceName, code, errorResponse);
                return null;
            }
        } catch (Exception e) {
            log.debug("Error fetching release info from {}: {}", sourceName, e.getMessage());
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                    // 忽略断开连接时的异常
                }
            }
        }
    }

    /**
     * 读取 HTTP 错误响应
     */
    private String readErrorResponse(HttpURLConnection conn) {
        try (InputStream errorStream = conn.getErrorStream()) {
            if (errorStream != null) {
                try (Scanner scanner = new Scanner(errorStream, StandardCharsets.UTF_8)) {
                    if (scanner.hasNext()) {
                        return scanner.useDelimiter("\\A").next();
                    }
                }
            }
        } catch (Exception ignored) {
            // 忽略读取错误响应的异常
        }
        return "";
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
        String source = releaseInfo.getStr(SOURCE_KEY, "unknown");

        String downloadUrl;
        if (osName.contains("win")) {
            // Windows 特殊处理：区分便携版和安装版
            downloadUrl = getWindowsDownloadUrl(assets);
        } else if (osName.contains("mac")) {
            // macOS 特殊处理：支持新版本的架构特定 DMG 和旧版本的通用 DMG
            downloadUrl = getMacDownloadUrl(assets);
        } else if (osName.contains("linux")) {
            downloadUrl = findAssetByExtension(assets, ".deb");
        } else {
            log.warn("Unsupported OS: {}", osName);
            return null;
        }

        if (downloadUrl != null) {
            log.info("Found download URL from {}: {}", source, downloadUrl);
        }

        return downloadUrl;
    }

    /**
     * 获取 Windows 下载链接（区分便携版和安装版）
     */
    private String getWindowsDownloadUrl(JSONArray assets) {
        // 检测当前运行的是便携版还是安装版
        boolean isPortable = isWindowsPortableVersion();

        if (isPortable) {
            // 便携版：优先下载便携版 ZIP
            log.info("Detected portable version, looking for portable.zip");
            String portableUrl = findAssetByPattern(assets, PORTABLE_ZIP);

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
                String url = asset.getStr(BROWSER_DOWNLOAD_URL);
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

        // 1. 优先查找新格式的架构特定 DMG
        String archSpecificSuffix = getMacPackageSuffix();
        String downloadUrl = findAssetByExtension(assets, archSpecificSuffix);

        if (downloadUrl != null) {
            log.info("Found architecture-specific DMG (new format): {}", archSpecificSuffix);
            return downloadUrl;
        }

        // 2. 回退到旧格式的架构特定 DMG
        if (isAppleSilicon) {
            log.info("New format not found, trying legacy format: -arm64.dmg");
            downloadUrl = findAssetByExtension(assets, "-arm64.dmg");

            if (downloadUrl != null) {
                log.info("Found legacy ARM64 DMG: -arm64.dmg");
                return downloadUrl;
            }

            // 3. 最后尝试通用 .dmg（旧版本，只支持 M 芯片）
            log.info("Legacy ARM64 DMG not found, trying generic .dmg for backward compatibility");
            downloadUrl = findGenericDmg(assets);

            if (downloadUrl != null) {
                log.info("Found generic DMG (legacy version, M chip compatible)");
                return downloadUrl;
            }
        } else {
            // Intel Mac: 尝试旧格式 -intel.dmg
            log.info("New format not found, trying legacy format: -intel.dmg");
            downloadUrl = findAssetByExtension(assets, "-intel.dmg");

            if (downloadUrl != null) {
                log.info("Found legacy Intel DMG: -intel.dmg");
                return downloadUrl;
            }

            log.warn("Intel Mac requires -intel.dmg or -macos-x86_64.dmg file, but not found in release assets");
            log.info("Note: Legacy generic .dmg files only support Apple Silicon (M chip)");
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
                String url = asset.getStr(BROWSER_DOWNLOAD_URL);
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
            if (name != null && name.endsWith(".dmg") &&
                    !name.endsWith("-intel.dmg") &&
                    !name.endsWith("-arm64.dmg") &&
                    !name.endsWith("-macos-x86_64.dmg") &&
                    !name.endsWith(MACOS_ARM64_DMG)) {
                String url = asset.getStr(BROWSER_DOWNLOAD_URL);
                log.debug("Found generic DMG (without architecture suffix): {} -> {}", name, url);
                return url;
            }
        }
        return null;
    }

    /**
     * 获取 macOS 包的后缀（根据芯片架构判断）
     * 支持新命名规范（-macos-x86_64.dmg / -macos-arm64.dmg）
     * 同时兼容旧命名规范（-intel.dmg / -arm64.dmg）用于向后兼容
     */
    private String getMacPackageSuffix() {
        try {
            // 使用 Java 系统属性检测架构，避免执行系统命令（防止被杀毒软件误报）
            String arch = System.getProperty("os.arch", "").toLowerCase();
            log.debug("Detected macOS architecture via os.arch: {}", arch);

            // 检测 Apple Silicon (ARM64)
            if (arch.contains("aarch64") || arch.equals("arm64")) {
                log.info("Detected Apple Silicon (ARM64), using -macos-arm64.dmg");
                return MACOS_ARM64_DMG;
            }

            // 检测 Intel (x86_64)
            if (arch.contains("x86_64") || arch.contains("amd64") || arch.equals("x64")) {
                log.info("Detected Intel chip (x86_64), using -macos-x86_64.dmg");
                return "-macos-x86_64.dmg";
            }

        } catch (Exception e) {
            log.warn("Failed to detect macOS architecture: {}", e.getMessage());
        }

        // 默认返回 ARM64 版本（因为新 Mac 都是 Apple Silicon）
        log.info("Unable to detect architecture, defaulting to -macos-arm64.dmg");
        return MACOS_ARM64_DMG;
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
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
