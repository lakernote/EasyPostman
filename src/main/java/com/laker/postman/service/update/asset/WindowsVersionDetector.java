package com.laker.postman.service.update.asset;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * Windows 版本检测器 - 判断是便携版还是安装版
 */
@Slf4j
public class WindowsVersionDetector {

    private static final String PORTABLE_MARKER = ".portable";

    /**
     * 检测是否为便携版
     *
     * <p>判断依据：</p>
     * <ol>
     *   <li>检查应用根目录是否存在 .portable 标识文件（优先级最高）</li>
     *   <li>检查是否从 Program Files 或 AppData 目录运行（安装版特征）</li>
     *   <li>默认认为是安装版</li>
     * </ol>
     *
     * @return true 表示便携版，false 表示安装版
     */
    public static boolean isPortableVersion() {
        try {
            String jarPath = WindowsVersionDetector.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();
            String decodedPath = java.net.URLDecoder.decode(jarPath, java.nio.charset.StandardCharsets.UTF_8);

            log.info("Current JAR path: {}", decodedPath);

            File jarFile = new File(decodedPath);
            File appDir = jarFile.getParentFile();

            if (appDir == null) {
                log.warn("Cannot determine application directory");
            } else {
                // 1. 检查 .portable 标识文件
                if (hasPortableMarker(appDir)) {
                    log.info("Found .portable marker file, confirmed portable version");
                    return true;
                }

                // 检查上级目录
                File parentDir = appDir.getParentFile();
                if (parentDir != null && hasPortableMarker(parentDir)) {
                    log.info("Found .portable marker in parent directory, confirmed portable version");
                    return true;
                }
            }

            // 2. 检查安装路径特征
            if (isInstalledPath(decodedPath)) {
                log.info("Running from installation directory, detected as installed version");
                return false;
            }

            // 3. 默认为安装版
            log.info("No .portable marker found, assuming installed version");
            return false;

        } catch (Exception e) {
            log.warn("Failed to detect Windows version type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查目录是否包含便携版标识文件
     */
    private static boolean hasPortableMarker(File dir) {
        File marker = new File(dir, PORTABLE_MARKER);
        return marker.exists() && marker.isFile();
    }

    /**
     * 检查路径是否为典型的安装路径
     */
    private static boolean isInstalledPath(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.contains("program files") ||
               lowerPath.contains("appdata\\local\\programs") ||
               lowerPath.contains("appdata/local/programs");
    }
}

