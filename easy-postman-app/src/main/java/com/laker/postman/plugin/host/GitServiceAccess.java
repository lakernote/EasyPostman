package com.laker.postman.plugin.host;

import com.laker.postman.plugin.api.service.GitPluginService;
import com.laker.postman.plugin.git.GitWorkspacePluginService;

public final class GitServiceAccess {

    private static final GitPluginService BUILT_IN_SERVICE = new GitWorkspacePluginService();

    private GitServiceAccess() {
    }

    public static boolean isServiceAvailable() {
        return true;
    }

    public static GitPluginService requireService() {
        return BUILT_IN_SERVICE;
    }
}
