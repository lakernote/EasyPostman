package com.laker.postman.service.http.ssl;

import com.laker.postman.model.TrustedCertificateEntry;
import org.testng.annotations.Test;

import javax.net.ssl.X509TrustManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class CustomTrustMaterialLoaderTest {

    @Test
    public void shouldLoadTrustManagerFromPemCertificateFile() throws Exception {
        Path certificateFile = TestTrustMaterialFixtures.writePemCertificateFile();
        try {
            X509TrustManager trustManager = CustomTrustMaterialLoader.loadTrustManager(
                    certificateFile.toString(),
                    ""
            );

            assertNotNull(trustManager);
            assertTrue(trustManager.getAcceptedIssuers().length > 0);
            TestTrustMaterialFixtures.assertTrusts(trustManager);
        } finally {
            Files.deleteIfExists(certificateFile);
        }
    }

    @Test
    public void shouldLoadTrustManagerFromJksTrustStore() throws Exception {
        String password = "changeit";
        Path trustStoreFile = TestTrustMaterialFixtures.writeJksTrustStore(password);
        try {
            X509TrustManager trustManager = CustomTrustMaterialLoader.loadTrustManager(
                    trustStoreFile.toString(),
                    password
            );

            assertNotNull(trustManager);
            assertTrue(trustManager.getAcceptedIssuers().length > 0);
            TestTrustMaterialFixtures.assertTrusts(trustManager);
        } finally {
            Files.deleteIfExists(trustStoreFile);
        }
    }

    @Test
    public void shouldLoadTrustManagerFromMultipleTrustEntries() throws Exception {
        String password = "changeit";
        Path certificateFile = TestTrustMaterialFixtures.writePemCertificateFile();
        Path trustStoreFile = TestTrustMaterialFixtures.writeJksTrustStore(password);
        try {
            TrustedCertificateEntry pemEntry = new TrustedCertificateEntry();
            pemEntry.setPath(certificateFile.toString());

            TrustedCertificateEntry jksEntry = new TrustedCertificateEntry();
            jksEntry.setPath(trustStoreFile.toString());
            jksEntry.setPassword(password);

            X509TrustManager trustManager = CustomTrustMaterialLoader.loadTrustManager(
                    List.of(pemEntry, jksEntry)
            );

            assertNotNull(trustManager);
            assertTrue(trustManager.getAcceptedIssuers().length > 0);
            TestTrustMaterialFixtures.assertTrusts(trustManager);
        } finally {
            Files.deleteIfExists(certificateFile);
            Files.deleteIfExists(trustStoreFile);
        }
    }
}
