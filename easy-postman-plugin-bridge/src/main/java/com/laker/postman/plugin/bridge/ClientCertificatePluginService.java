package com.laker.postman.plugin.bridge;

import com.laker.postman.model.ClientCertificate;

import javax.net.ssl.KeyManager;
import java.util.List;

public interface ClientCertificatePluginService {

    List<ClientCertificate> getAllCertificates();

    void addCertificate(ClientCertificate certificate);

    void updateCertificate(ClientCertificate certificate);

    void deleteCertificate(String id);

    boolean validateCertificatePaths(ClientCertificate certificate);

    KeyManager[] loadClientCertificateKeyManagers(String host, int port);
}
