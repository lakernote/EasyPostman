package com.laker.postman.plugin.bridge;

import com.laker.postman.plugin.git.GitWorkspacePluginService;

public final class GitPluginServices {

    private static final GitPluginService BUILT_IN_SERVICE = new GitWorkspacePluginService();

    private GitPluginServices() {
    }

    public static boolean isGitPluginInstalled() {
        return true;
    }

    public static GitPluginService requireGitService() {
        return BUILT_IN_SERVICE;
    }
}
