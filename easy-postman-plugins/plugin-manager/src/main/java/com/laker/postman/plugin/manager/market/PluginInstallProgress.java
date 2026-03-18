package com.laker.postman.plugin.manager.market;

/**
 * 插件安装进度快照。
 */
public record PluginInstallProgress(
        Stage stage,
        String pluginId,
        String installUrl,
        long downloadedBytes,
        long totalBytes,
        double bytesPerSecond
) {

    public enum Stage {
        CONNECTING,
        DOWNLOADING,
        VERIFYING,
        INSTALLING,
        COMPLETED
    }

    public boolean hasKnownTotalBytes() {
        return totalBytes > 0;
    }
}
