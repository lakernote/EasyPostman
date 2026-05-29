package com.example;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;

public class LinkageErrorRuntimePlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        throw new NoClassDefFoundError("com/laker/postman/plugin/bridge/ClientCertificatePluginService");
    }
}
