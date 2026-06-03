package com.laker.postman.plugin.api;

/**
 * Update metadata contributed by a plugin.
 *
 * <p>This is intentionally independent from the host plugin catalog model so
 * third-party plugins only depend on the stable plugin API module.</p>
 */
public record PluginUpdateMetadata(
        String pluginId,
        String pluginName,
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

    public PluginUpdateMetadata(String pluginId,
                                String pluginName,
                                String version,
                                String description,
                                String downloadUrl,
                                String homepageUrl,
                                String sha256) {
        this(pluginId, pluginName, version, description, downloadUrl, homepageUrl, sha256,
                downloadUrl, "", "", "", "");
    }

    public PluginUpdateMetadata {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("Plugin update metadata pluginId must not be blank");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Plugin update metadata version must not be blank");
        }
        pluginId = pluginId.trim();
        pluginName = pluginName == null || pluginName.isBlank() ? pluginId : pluginName.trim();
        version = version.trim();
        description = normalize(description);
        downloadUrl = normalize(downloadUrl);
        homepageUrl = normalize(homepageUrl);
        sha256 = normalize(sha256);
        resolvedDownloadUrl = normalize(resolvedDownloadUrl);
        minAppVersion = normalize(minAppVersion);
        maxAppVersion = normalize(maxAppVersion);
        minPlatformVersion = normalize(minPlatformVersion);
        maxPlatformVersion = normalize(maxPlatformVersion);
    }

    public String installUrl() {
        return resolvedDownloadUrl == null || resolvedDownloadUrl.isBlank() ? downloadUrl : resolvedDownloadUrl;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
