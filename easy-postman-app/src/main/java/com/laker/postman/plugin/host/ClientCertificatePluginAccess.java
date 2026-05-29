package com.laker.postman.plugin.host;

import com.laker.postman.plugin.api.service.ClientCertificatePluginService;

public final class ClientCertificatePluginAccess {

    private static final String MISSING_MESSAGE =
            "Client Certificate plugin is not installed. Please install easy-postman-plugin-client-cert first.";

    private ClientCertificatePluginAccess() {
    }

    public static boolean isServiceAvailable() {
        return PluginAccess.getService(ClientCertificatePluginService.class) != null;
    }

    public static ClientCertificatePluginService getService() {
        return PluginAccess.getService(ClientCertificatePluginService.class);
    }

    public static ClientCertificatePluginService requireService() {
        ClientCertificatePluginService service = getService();
        if (service == null) {
            throw new IllegalStateException(MISSING_MESSAGE);
        }
        return service;
    }
}
