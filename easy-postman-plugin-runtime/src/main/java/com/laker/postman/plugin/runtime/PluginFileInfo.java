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
        boolean compatible
) {

    public PluginFileInfo(PluginDescriptor descriptor, Path jarPath, boolean loaded) {
        this(descriptor, jarPath, loaded, true, true);
    }
}
