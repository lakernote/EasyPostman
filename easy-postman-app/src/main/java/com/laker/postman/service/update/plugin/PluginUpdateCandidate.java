package com.laker.postman.service.update.plugin;

/**
 * 插件更新候选项。
 */
public record PluginUpdateCandidate(
        String pluginId,
        String pluginName,
        String installedVersion,
        String latestVersion
) {
}
