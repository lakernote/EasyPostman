package com.laker.postman.plugin.market;

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
        String sha256
) {

    public boolean isPlaceholder() {
        return "empty".equals(id);
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    public boolean hasHomepageUrl() {
        return homepageUrl != null && !homepageUrl.isBlank();
    }
}
