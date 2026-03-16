package com.laker.postman.plugin.bridge;

import com.laker.postman.plugin.runtime.PluginRuntime;

public final class GitPluginServices {

    private static final String MISSING_MESSAGE = "Git plugin is not installed. Please install easy-postman-plugin-git first.";

    private GitPluginServices() {
    }

    public static boolean isGitPluginInstalled() {
        return PluginRuntime.getRegistry().getService(GitPluginService.class) != null;
    }

    public static GitPluginService requireGitService() {
        GitPluginService service = PluginRuntime.getRegistry().getService(GitPluginService.class);
        if (service == null) {
            throw new IllegalStateException(MISSING_MESSAGE);
        }
        return service;
    }
}
