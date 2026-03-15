package com.laker.postman.plugin.api;

/**
 * 插件元数据。
 */
public record PluginDescriptor(
        String id,
        String name,
        String version,
        String entryClass,
        String description,
        String homepageUrl,
        String minAppVersion,
        String maxAppVersion
) {

    public PluginDescriptor(String id, String name, String version, String entryClass) {
        this(id, name, version, entryClass, "", "", "", "");
    }

    public PluginDescriptor(String id, String name, String version, String entryClass,
                            String description, String homepageUrl) {
        this(id, name, version, entryClass, description, homepageUrl, "", "");
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    public boolean hasHomepageUrl() {
        return homepageUrl != null && !homepageUrl.isBlank();
    }

    public boolean hasMinAppVersion() {
        return minAppVersion != null && !minAppVersion.isBlank();
    }

    public boolean hasMaxAppVersion() {
        return maxAppVersion != null && !maxAppVersion.isBlank();
    }
}
