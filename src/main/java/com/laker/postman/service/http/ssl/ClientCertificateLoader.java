package com.laker.postman.service.http.ssl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.ClientCertificate;
import com.laker.postman.util.PemUtil;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

/**
 * 客户端证书加载器
 * 支持加载 PFX/P12 和 PEM 格式的客户端证书
 */
@Slf4j
public class ClientCertificateLoader {

    private ClientCertificateLoader() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 从客户端证书配置创建 KeyManager
     */
    public static KeyManager[] createKeyManagers(ClientCertificate cert) throws Exception {
        if (cert == null) {
            return new KeyManager[0];
        }

        if (ClientCertificate.CERT_TYPE_PFX.equals(cert.getCertType())) {
            return createKeyManagersFromPFX(cert);
        } else if (ClientCertificate.CERT_TYPE_PEM.equals(cert.getCertType())) {
            return createKeyManagersFromPEM(cert);
        } else {
            throw new IllegalArgumentException("Unsupported certificate type: " + cert.getCertType());
        }
    }

    /**
     * 从 PFX/P12 文件创建 KeyManager
     */
    private static KeyManager[] createKeyManagersFromPFX(ClientCertificate cert) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] password = cert.getCertPassword() != null ?
                cert.getCertPassword().toCharArray() : new char[0];

        try (FileInputStream fis = new FileInputStream(cert.getCertPath())) {
            keyStore.load(fis, password);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        log.debug("Loaded PFX/P12 certificate from: {}", cert.getCertPath());
        return kmf.getKeyManagers();
    }

    /**
     * 从 PEM 文件创建 KeyManager
     */
    private static KeyManager[] createKeyManagersFromPEM(ClientCertificate cert) throws Exception {
        String certPem = FileUtil.readString(cert.getCertPath(), StandardCharsets.UTF_8);
        String keyPem = FileUtil.readString(cert.getKeyPath(), StandardCharsets.UTF_8);

        KeyManager[] keyManagers = PemUtil.createKeyManagers(certPem, keyPem, CharSequenceUtil.trimToNull(cert.getKeyPassword()));

        log.debug("Loaded PEM certificate from: {} and key from: {}", cert.getCertPath(), cert.getKeyPath());
        return keyManagers;
    }
}