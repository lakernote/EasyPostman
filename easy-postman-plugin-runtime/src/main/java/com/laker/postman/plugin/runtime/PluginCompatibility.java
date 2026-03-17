package com.laker.postman.plugin.runtime;

/**
 * 插件兼容性判定结果。
 */
public record PluginCompatibility(
        boolean compatible,
        boolean appVersionCompatible,
        boolean platformVersionCompatible,
        String currentAppVersion,
        String currentPlatformVersion,
        String minAppVersion,
        String maxAppVersion,
        String minPlatformVersion,
        String maxPlatformVersion
) {

    public boolean hasAppVersionConstraint() {
        return notBlank(minAppVersion) || notBlank(maxAppVersion);
    }

    public boolean hasPlatformVersionConstraint() {
        return notBlank(minPlatformVersion) || notBlank(maxPlatformVersion);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
