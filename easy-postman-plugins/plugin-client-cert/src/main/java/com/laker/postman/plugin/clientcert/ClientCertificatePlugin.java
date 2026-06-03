package com.laker.postman.plugin.clientcert;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContributionSupport;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginSettingsContribution;
import com.laker.postman.plugin.api.service.ClientCertificatePluginService;

public class ClientCertificatePlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        context.registerI18nBundle(ClientCertI18n.BUNDLE_NAME);
        ClientCertificatePluginServiceImpl certificateService = new ClientCertificatePluginServiceImpl();
        context.registerService(ClientCertificatePluginService.class, certificateService);
        PluginContributionSupport.registerSettingsContribution(
                context,
                "client-certificates",
                MessageKeys.CERT_TITLE,
                700,
                PluginSettingsContribution.CATEGORY_EXTENSIONS,
                ClientCertI18n.BUNDLE_NAME,
                settingsContext -> new ClientCertificateSettingsPanel(settingsContext.parentWindow(), certificateService),
                ClientCertificatePlugin.class
        );
    }
}
