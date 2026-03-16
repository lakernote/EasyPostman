package com.laker.postman.plugin.clientcert.internal;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.ClientCertificate;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.ssl.pem.util.PemUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.KeyStore;

@Slf4j
public final class ClientCertificateLoader {

    private ClientCertificateLoader() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static KeyManager[] createKeyManagers(ClientCertificate certificate) throws Exception {
        if (certificate == null) {
            return new KeyManager[0];
        }

        if (ClientCertificate.CERT_TYPE_PFX.equals(certificate.getCertType())) {
            return createKeyManagersFromPfx(certificate);
        }
        if (ClientCertificate.CERT_TYPE_PEM.equals(certificate.getCertType())) {
            return createKeyManagersFromPem(certificate);
        }
        throw new IllegalArgumentException("Unsupported certificate type: " + certificate.getCertType());
    }

    private static KeyManager[] createKeyManagersFromPfx(ClientCertificate certificate) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] password = certificate.getCertPassword() != null
                ? certificate.getCertPassword().toCharArray()
                : new char[0];

        try (FileInputStream fis = new FileInputStream(certificate.getCertPath())) {
            keyStore.load(fis, password);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        log.debug("Loaded PFX/P12 certificate from: {}", certificate.getCertPath());
        return kmf.getKeyManagers();
    }

    private static KeyManager[] createKeyManagersFromPem(ClientCertificate certificate) throws Exception {
        log.debug("Loaded PEM certificate from: {} and key from: {}",
                certificate.getCertPath(), certificate.getKeyPath());
        try (BufferedInputStream certInputStream = FileUtil.getInputStream(certificate.getCertPath());
             BufferedInputStream keyInputStream = FileUtil.getInputStream(certificate.getKeyPath())) {

            X509ExtendedKeyManager keyManager = CharSequenceUtil.isNotBlank(certificate.getKeyPassword())
                    ? PemUtils.loadIdentityMaterial(certInputStream, keyInputStream,
                    certificate.getKeyPassword().toCharArray())
                    : PemUtils.loadIdentityMaterial(certInputStream, keyInputStream);

            return new KeyManager[]{keyManager};
        }
    }
}
