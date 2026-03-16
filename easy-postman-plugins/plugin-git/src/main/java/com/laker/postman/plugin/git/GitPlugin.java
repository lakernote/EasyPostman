package com.laker.postman.plugin.git;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.bridge.GitPluginService;

public class GitPlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        context.registerService(GitPluginService.class, new GitWorkspacePluginService());
    }
}
