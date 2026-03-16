package com.laker.postman.plugin.clientcert;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.bridge.ClientCertificatePluginService;

public class ClientCertificatePlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        context.registerService(ClientCertificatePluginService.class, new ClientCertificatePluginServiceImpl());
    }
}
