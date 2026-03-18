package com.laker.postman.plugin.manager.market;

@FunctionalInterface
public interface PluginInstallProgressListener {

    PluginInstallProgressListener NO_OP = progress -> {
    };

    void onProgress(PluginInstallProgress progress);
}
