package com.laker.postman.plugin.manager;

public record PluginUninstallResult(
        boolean removed,
        boolean restartRequired
) {
}
