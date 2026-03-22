package com.laker.postman.service.http.ssl;

import org.testng.annotations.Test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class SSLConfigurationUtilTest {

    @Test
    public void mergedTrustManagerShouldContainSystemAndCustomIssuers() throws Exception {
        Path certificateFile = TestTrustMaterialFixtures.writePemCertificateFile();
        try {
            X509TrustManager defaultTrustManager = getDefaultTrustManager();
            X509TrustManager customTrustManager = CustomTrustMaterialLoader.loadTrustManager(
                    certificateFile.toString(),
                    ""
            );

            X509TrustManager mergedTrustManager = SSLConfigurationUtil.createMergedTrustManager(
                    defaultTrustManager,
                    customTrustManager
            );

            assertNotNull(mergedTrustManager);
            X509Certificate customCertificate = TestTrustMaterialFixtures.loadCertificate();
            X509Certificate systemIssuer = defaultTrustManager.getAcceptedIssuers()[0];
            X509Certificate[] mergedIssuers = mergedTrustManager.getAcceptedIssuers();

            assertTrue(Arrays.stream(mergedIssuers).anyMatch(customCertificate::equals));
            assertTrue(Arrays.stream(mergedIssuers).anyMatch(systemIssuer::equals));
            TestTrustMaterialFixtures.assertTrusts(mergedTrustManager);
        } finally {
            Files.deleteIfExists(certificateFile);
        }
    }

    private static X509TrustManager getDefaultTrustManager() throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);

        for (TrustManager trustManager : factory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new IllegalStateException("No default X509TrustManager found");
    }
}
