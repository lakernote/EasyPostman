package com.laker.postman.plugin.bridge;

import com.laker.postman.plugin.runtime.PluginRuntime;

public final class ClientCertificatePluginServices {

    private static final String MISSING_MESSAGE =
            "Client Certificate plugin is not installed. Please install easy-postman-plugin-client-cert first.";

    private ClientCertificatePluginServices() {
    }

    public static boolean isClientCertificatePluginInstalled() {
        return PluginRuntime.getRegistry().getService(ClientCertificatePluginService.class) != null;
    }

    public static ClientCertificatePluginService getClientCertificateService() {
        return PluginRuntime.getRegistry().getService(ClientCertificatePluginService.class);
    }

    public static ClientCertificatePluginService requireClientCertificateService() {
        ClientCertificatePluginService service = getClientCertificateService();
        if (service == null) {
            throw new IllegalStateException(MISSING_MESSAGE);
        }
        return service;
    }
}
