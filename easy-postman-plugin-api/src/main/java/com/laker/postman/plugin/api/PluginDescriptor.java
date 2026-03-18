package com.laker.postman.plugin.api;

/**
 * 插件元数据。
 *
 * <p>字段语义约定：
 * <p>- min/maxAppVersion：约束宿主发行版范围，只有插件依赖某个宿主功能时才建议填写
 * <p>- min/maxPlatformVersion：约束插件平台 SPI 范围，属于插件机制内部兼容边界
 * <p>- 运行时会同时检查 app + platform，两者都满足才允许加载
 */
public record PluginDescriptor(
        String id,
        String name,
        String version,
        String entryClass,
        String description,
        String homepageUrl,
        String minAppVersion,
        String maxAppVersion,
        String minPlatformVersion,
        String maxPlatformVersion
) {

    public PluginDescriptor(String id, String name, String version, String entryClass) {
        this(id, name, version, entryClass, "", "", "", "", "", "");
    }

    public PluginDescriptor(String id, String name, String version, String entryClass,
                            String description, String homepageUrl) {
        this(id, name, version, entryClass, description, homepageUrl, "", "", "", "");
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

    public boolean hasMinPlatformVersion() {
        return minPlatformVersion != null && !minPlatformVersion.isBlank();
    }

    public boolean hasMaxPlatformVersion() {
        return maxPlatformVersion != null && !maxPlatformVersion.isBlank();
    }
}
