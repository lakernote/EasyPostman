package com.laker.postman.plugin.bridge;

public final class ClientCertificatePluginServices {

    private static final String MISSING_MESSAGE =
            "Client Certificate plugin is not installed. Please install easy-postman-plugin-client-cert first.";

    private ClientCertificatePluginServices() {
    }

    public static boolean isClientCertificatePluginInstalled() {
        return PluginAccess.getService(ClientCertificatePluginService.class) != null;
    }

    public static ClientCertificatePluginService getClientCertificateService() {
        return PluginAccess.getService(ClientCertificatePluginService.class);
    }

    public static ClientCertificatePluginService requireClientCertificateService() {
        ClientCertificatePluginService service = getClientCertificateService();
        if (service == null) {
            throw new IllegalStateException(MISSING_MESSAGE);
        }
        return service;
    }
}
