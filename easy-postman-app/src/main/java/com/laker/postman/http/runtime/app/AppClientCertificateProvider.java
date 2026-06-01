package com.laker.postman.http.runtime.app;

import com.laker.postman.http.runtime.ssl.ClientCertificateProvider;
import com.laker.postman.model.ClientCertificate;
import com.laker.postman.plugin.api.service.ClientCertificatePluginService;
import com.laker.postman.plugin.host.ClientCertificatePluginAccess;

import javax.net.ssl.KeyManager;
import java.io.File;

public class AppClientCertificateProvider implements ClientCertificateProvider {

    @Override
    public KeyManager[] loadKeyManagers(String host, int port) {
        ClientCertificatePluginService service = ClientCertificatePluginAccess.getService();
        if (service == null) {
            return new KeyManager[0];
        }
        return service.loadClientCertificateKeyManagers(host, port);
    }

    @Override
    public String cacheKey(String host, int port) {
        ClientCertificatePluginService service = ClientCertificatePluginAccess.getService();
        if (service == null) {
            return "clientCert:none";
        }
        for (ClientCertificate certificate : service.getAllCertificates()) {
            if (certificate != null && certificate.matches(host, port)) {
                return clientCertificateCacheKey(certificate);
            }
        }
        return "clientCert:none";
    }

    private static String clientCertificateCacheKey(ClientCertificate certificate) {
        return "clientCert:"
                + safe(certificate.getId()) + ':'
                + certificate.isEnabled() + ':'
                + safe(certificate.getHost()) + ':'
                + certificate.getPort() + ':'
                + safe(certificate.getCertType()) + ':'
                + fileCacheKey(certificate.getCertPath()) + ':'
                + safeHash(certificate.getCertPassword()) + ':'
                + fileCacheKey(certificate.getKeyPath()) + ':'
                + safeHash(certificate.getKeyPassword()) + ':'
                + certificate.getUpdatedAt();
    }

    private static String fileCacheKey(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        File file = new File(path);
        long lastModified = file.exists() ? file.lastModified() : -1L;
        long length = file.exists() ? file.length() : -1L;
        return path + ':' + lastModified + ':' + length;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static int safeHash(String value) {
        return value == null ? 0 : value.hashCode();
    }
}
