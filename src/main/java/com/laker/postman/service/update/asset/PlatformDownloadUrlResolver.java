package com.laker.postman.service.update.asset;

import cn.hutool.json.JSONArray;
import lombok.extern.slf4j.Slf4j;

/**
 * 平台特定的下载 URL 解析器
 */
@Slf4j
public class PlatformDownloadUrlResolver {

    private final AssetFinder assetFinder;

    public PlatformDownloadUrlResolver() {
        this.assetFinder = new AssetFinder();
    }

    /**
     * 根据当前平台获取下载 URL
     *
     * @param assets 资源列表
     * @return 下载 URL，未找到返回 null
     */
    public String resolveDownloadUrl(JSONArray assets) {
        // 首先尝试查找 Fat JAR
        String jarUrl = assetFinder.findFatJar(assets);
        if (jarUrl != null) {
            log.info("Found fat JAR download URL: {}", jarUrl);
            return jarUrl;
        }

        log.warn("Fat JAR not found, falling back to platform-specific installer");

        // 根据操作系统查找对应的安装包
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return resolveWindowsUrl(assets);
        } else if (osName.contains("mac")) {
            return resolveMacUrl(assets);
        } else if (osName.contains("linux")) {
            return assetFinder.findByExtension(assets, ".deb");
        } else {
            log.warn("Unsupported OS: {}", osName);
            return null;
        }
    }

    /**
     * 解析 Windows 下载 URL
     */
    private String resolveWindowsUrl(JSONArray assets) {
        boolean isPortable = WindowsVersionDetector.isPortableVersion();

        if (isPortable) {
            log.info("Detected portable version, looking for portable.zip");
            String portableUrl = assetFinder.findByPattern(assets, "-portable.zip");

            if (portableUrl != null) {
                log.info("Found portable ZIP for update");
                return portableUrl;
            }

            log.warn("Portable ZIP not found, update not available for portable version");
            return null;
        } else {
            log.info("Detected installed version, looking for .msi");
            return assetFinder.findByExtension(assets, ".msi");
        }
    }

    /**
     * 解析 macOS 下载 URL
     */
    private String resolveMacUrl(JSONArray assets) {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        boolean isAppleSilicon = arch.contains("aarch64") || arch.equals("arm64");

        // 1. 优先查找新格式的架构特定 DMG
        String archSpecificSuffix = getMacPackageSuffix(isAppleSilicon);
        String downloadUrl = assetFinder.findByExtension(assets, archSpecificSuffix);

        if (downloadUrl != null) {
            log.info("Found architecture-specific DMG (new format): {}", archSpecificSuffix);
            return downloadUrl;
        }

        // 2. 回退到旧格式
        if (isAppleSilicon) {
            log.info("New format not found, trying legacy format: -arm64.dmg");
            downloadUrl = assetFinder.findByExtension(assets, "-arm64.dmg");

            if (downloadUrl != null) {
                log.info("Found legacy ARM64 DMG");
                return downloadUrl;
            }

            // 3. 尝试通用 DMG（仅支持 M 芯片）
            log.info("Legacy ARM64 DMG not found, trying generic .dmg");
            downloadUrl = assetFinder.findGenericDmg(assets);

            if (downloadUrl != null) {
                log.info("Found generic DMG (legacy version, M chip compatible)");
                return downloadUrl;
            }
        } else {
            // Intel Mac
            log.info("New format not found, trying legacy format: -intel.dmg");
            downloadUrl = assetFinder.findByExtension(assets, "-intel.dmg");

            if (downloadUrl != null) {
                log.info("Found legacy Intel DMG");
                return downloadUrl;
            }

            log.warn("Intel Mac requires -intel.dmg or -macos-x86_64.dmg file");
        }

        log.warn("No suitable DMG file found for current architecture");
        return null;
    }

    /**
     * 获取 macOS 包后缀
     */
    private String getMacPackageSuffix(boolean isAppleSilicon) {
        if (isAppleSilicon) {
            log.info("Detected Apple Silicon (ARM64), using -macos-arm64.dmg");
            return "-macos-arm64.dmg";
        } else {
            log.info("Detected Intel chip (x86_64), using -macos-x86_64.dmg");
            return "-macos-x86_64.dmg";
        }
    }
}

