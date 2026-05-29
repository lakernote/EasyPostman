package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.PluginDescriptor;

import java.nio.file.Path;

/**
 * 插件文件信息。
 */
public record PluginFileInfo(
        PluginDescriptor descriptor,
        Path jarPath,
        boolean loaded,
        boolean enabled,
        boolean compatible,
        String loadFailureMessage
) {

    public PluginFileInfo {
        loadFailureMessage = loadFailureMessage == null ? "" : loadFailureMessage;
    }

    public PluginFileInfo(PluginDescriptor descriptor,
                          Path jarPath,
                          boolean loaded,
                          boolean enabled,
                          boolean compatible) {
        this(descriptor, jarPath, loaded, enabled, compatible, "");
    }

    public PluginFileInfo(PluginDescriptor descriptor, Path jarPath, boolean loaded) {
        this(descriptor, jarPath, loaded, true, true);
    }

    public boolean hasLoadFailure() {
        return !loadFailureMessage.isBlank();
    }
}
