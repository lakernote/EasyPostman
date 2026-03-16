package com.laker.postman.plugin.manager.market;

/**
 * 插件市场条目。
 */
public record PluginCatalogEntry(
        String id,
        String name,
        String version,
        String description,
        String downloadUrl,
        String homepageUrl,
        String sha256,
        String resolvedDownloadUrl
) {

    public PluginCatalogEntry(String id,
                              String name,
                              String version,
                              String description,
                              String downloadUrl,
                              String homepageUrl,
                              String sha256) {
        this(id, name, version, description, downloadUrl, homepageUrl, sha256, downloadUrl);
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

    public String installUrl() {
        return resolvedDownloadUrl != null && !resolvedDownloadUrl.isBlank() ? resolvedDownloadUrl : downloadUrl;
    }
}
