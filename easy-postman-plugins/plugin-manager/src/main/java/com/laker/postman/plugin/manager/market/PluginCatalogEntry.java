package com.laker.postman.plugin.manager.market;

/**
 * 插件市场条目。
 *
 * <p>这里是 catalog.json 对外暴露的元数据模型。
 * <p>- resolvedDownloadUrl：解析相对地址后的最终下载地址，优先用于实际安装
 * <p>- min/maxAppVersion：给用户表达“当前 EasyPostman 发行版能不能装”
 * <p>- min/maxPlatformVersion：给运行时表达“当前插件 SPI 能不能装配”
 */
public record PluginCatalogEntry(
        String id,
        String name,
        String version,
        String description,
        String downloadUrl,
        String homepageUrl,
        String sha256,
        String resolvedDownloadUrl,
        String minAppVersion,
        String maxAppVersion,
        String minPlatformVersion,
        String maxPlatformVersion
) {

    public PluginCatalogEntry(String id,
                              String name,
                              String version,
                              String description,
                              String downloadUrl,
                              String homepageUrl,
                              String sha256) {
        this(id, name, version, description, downloadUrl, homepageUrl, sha256, downloadUrl, "", "", "", "");
    }

    public PluginCatalogEntry(String id,
                              String name,
                              String version,
                              String description,
                              String downloadUrl,
                              String homepageUrl,
                              String sha256,
                              String resolvedDownloadUrl) {
        this(id, name, version, description, downloadUrl, homepageUrl, sha256, resolvedDownloadUrl, "", "", "", "");
    }

    public boolean isPlaceholder() {
        return "empty".equals(id);
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    public boolean hasHomepageUrl() {
        return homepageUrl != null && !homepageUrl.isBlank();
    }

    // 优先使用 catalog 解析后的绝对下载地址，避免相对路径在安装阶段重复处理。
    public String installUrl() {
        return resolvedDownloadUrl != null && !resolvedDownloadUrl.isBlank() ? resolvedDownloadUrl : downloadUrl;
    }

    public boolean hasMinAppVersion() {
        return minAppVersion != null && !minAppVersion.isBlank();
    }

    public boolean hasMaxAppVersion() {
        return maxAppVersion != null && !maxAppVersion.isBlank();
    }

    public boolean hasMinPlatformVersion() {
        return minPlatformVersion != null && !minPlatformVersion.isBlank();
    }

    public boolean hasMaxPlatformVersion() {
        return maxPlatformVersion != null && !maxPlatformVersion.isBlank();
    }
}
