package com.laker.postman.http.runtime.ssl;

import javax.net.ssl.KeyManager;

public interface ClientCertificateProvider {

    KeyManager[] loadKeyManagers(String host, int port);

    String cacheKey(String host, int port);

    static ClientCertificateProvider noop() {
        return new ClientCertificateProvider() {
            @Override
            public KeyManager[] loadKeyManagers(String host, int port) {
                return new KeyManager[0];
            }

            @Override
            public String cacheKey(String host, int port) {
                return "clientCert:none";
            }
        };
    }
}
