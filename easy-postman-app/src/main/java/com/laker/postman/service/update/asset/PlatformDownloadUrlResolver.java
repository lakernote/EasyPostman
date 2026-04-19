package com.laker.postman.service.update.asset;

import cn.hutool.json.JSONArray;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

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
     * 根据当前平台获取安装包下载 URL
     *
     * @param assets 资源列表
     * @return 下载 URL，未找到返回 null
     */
    public String resolveDownloadUrl(JSONArray assets) {
        return resolvePlatformInstallerUrl(assets);
    }

    /**
     * 解析平台特定的安装包 URL
     */
    private String resolvePlatformInstallerUrl(JSONArray assets) {
        // 根据操作系统查找对应的安装包
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return resolveWindowsUrl(assets);
        } else if (osName.contains("mac")) {
            return resolveMacUrl(assets);
        } else if (osName.contains("linux")) {
            return resolveLinuxUrl(assets);
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
            log.info("Detected installed version, looking for .exe");
            return assetFinder.findByExtension(assets, ".exe");
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
     * 解析 Linux 下载 URL
     */
    private String resolveLinuxUrl(JSONArray assets) {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean isArm64 = arch.contains("aarch64") || arch.equals("arm64");
        LinuxReleaseInfo releaseInfo = loadLinuxReleaseInfo();

        if (releaseInfo.prefersRpm()) {
            return resolveLinuxRpmUrl(assets, isArm64);
        }
        return resolveLinuxDebUrl(assets, isArm64, releaseInfo);
    }

    private String resolveLinuxDebUrl(JSONArray assets, boolean isArm64, LinuxReleaseInfo releaseInfo) {
        if (isArm64) {
            if (releaseInfo.isUosLike()) {
                String compatUrl = assetFinder.findByPattern(assets, "-linux-arm64-compat.deb");
                if (compatUrl != null) {
                    log.info("Detected UOS-like ARM64 system, using compatibility DEB");
                    return compatUrl;
                }
            }

            String genericArmUrl = assetFinder.findFirstByPatterns(
                    assets,
                    "-linux-arm64.deb",
                    "_arm64.deb");
            if (genericArmUrl != null) {
                log.info("Detected Linux ARM64, using generic ARM64 DEB");
                return genericArmUrl;
            }
        } else {
            String amd64Url = assetFinder.findFirstByPatterns(
                    assets,
                    "-linux-amd64.deb",
                    "_amd64.deb");
            if (amd64Url != null) {
                log.info("Detected Linux AMD64, using AMD64 DEB");
                return amd64Url;
            }
        }

        log.warn("No suitable DEB file found for Linux arch={}, distro={}", isArm64 ? "arm64" : "amd64", releaseInfo.summary());
        return null;
    }

    private String resolveLinuxRpmUrl(JSONArray assets, boolean isArm64) {
        String rpmUrl = assetFinder.findFirstByPatterns(
                assets,
                isArm64 ? ".aarch64.rpm" : ".x86_64.rpm");
        if (rpmUrl != null) {
            log.info("Detected RPM-based Linux, using {} package", isArm64 ? "ARM64" : "AMD64");
        } else {
            log.warn("No suitable RPM file found for Linux arch={}", isArm64 ? "arm64" : "amd64");
        }
        return rpmUrl;
    }

    private LinuxReleaseInfo loadLinuxReleaseInfo() {
        String overridePath = System.getProperty("easyPostman.update.osReleasePath", "").trim();
        Path osReleasePath = overridePath.isEmpty()
                ? Paths.get("/etc/os-release")
                : Paths.get(overridePath);

        try {
            if (!Files.exists(osReleasePath)) {
                return LinuxReleaseInfo.empty();
            }
            String content = Files.readString(osReleasePath).toLowerCase(Locale.ROOT);
            return new LinuxReleaseInfo(content);
        } catch (Exception e) {
            log.debug("Failed to read os-release from {}", osReleasePath, e);
            return LinuxReleaseInfo.empty();
        }
    }

    private static final class LinuxReleaseInfo {
        private final String normalizedContent;

        private LinuxReleaseInfo(String normalizedContent) {
            this.normalizedContent = normalizedContent == null ? "" : normalizedContent;
        }

        static LinuxReleaseInfo empty() {
            return new LinuxReleaseInfo("");
        }

        boolean prefersRpm() {
            return containsAny("rhel", "fedora", "centos", "rocky", "almalinux", "opensuse", "suse");
        }

        boolean isUosLike() {
            return containsAny("uos", "uniontech", "deepin");
        }

        String summary() {
            if (normalizedContent.isBlank()) {
                return "unknown";
            }
            return normalizedContent.length() > 120 ? normalizedContent.substring(0, 120) : normalizedContent;
        }

        private boolean containsAny(String... values) {
            for (String value : values) {
                if (normalizedContent.contains(value)) {
                    return true;
                }
            }
            return false;
        }
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
