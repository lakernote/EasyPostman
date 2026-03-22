package com.laker.postman.service.http.ssl;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

final class TestTrustMaterialFixtures {
    private static final String TEST_CERT_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDejCCAmKgAwIBAgIJAN/b0Bi2uG/6MA0GCSqGSIb3DQEBDAUAMGsxCzAJBgNV
            BAYTAkNOMREwDwYDVQQIEwhTaGFuZ2hhaTERMA8GA1UEBxMIU2hhbmdoYWkxFDAS
            BgNVBAoTC0Vhc3lQb3N0bWFuMQwwCgYDVQQLEwNEZXYxEjAQBgNVBAMTCWxvY2Fs
            aG9zdDAeFw0yNjAzMjIwNDE3NTlaFw0yNzAzMjIwNDE3NTlaMGsxCzAJBgNVBAYT
            AkNOMREwDwYDVQQIEwhTaGFuZ2hhaTERMA8GA1UEBxMIU2hhbmdoYWkxFDASBgNV
            BAoTC0Vhc3lQb3N0bWFuMQwwCgYDVQQLEwNEZXYxEjAQBgNVBAMTCWxvY2FsaG9z
            dDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL57NhhFqYqoNy2AGM6O
            Nat6X1blikV7ikm1X0Kzul/NamYzGr+O33JZFiGV1eYvMmh3GsP1MXNyl48iqfIE
            twhO8jNsP6uvpUgXfTy0rS7fPxdNbc4PJhoQBiDlF/84Lm6F23B21ObnT9VFb8K6
            DzYCUsaY1BKme7lVAjvcBnqaU7HLm6nCOtMJQzhdiCEUrVTFSmp86ZoOSWN/t/1y
            cospzX2nafWv6YHw4DX93EVKNBUc9NlN1sWJjdhjwz7hgPEwxP0RpJPcWVJxjV8o
            rOiFAQvIqfjP/CAyR3DmGDrFD/UpkSqL0yoGOLQ5NE4wlJrR43Uln8dplHpI7n3a
            7WMCAwEAAaMhMB8wHQYDVR0OBBYEFH7Pdn8s8QIbk5zvhSq0ewjbExEOMA0GCSqG
            SIb3DQEBDAUAA4IBAQBdGIMscMtu1SnsEQWzdzYno79OHgEOg3YuhQTUH9Tm8Tbw
            dQ4H8zAuXe0OYj7TsjJcaKBzkonE5pptMZ5YNp05l4+ny1bRL+INXdw7lm/1Hhea
            i7Ca/HxP5xuM6eNuqy33Dak3tCo6CJUlXKQK2nDczO7BDnQDgnsC3O7dKoySURmE
            1lI87H8oODIC/BprkJfSMSRtF9PSI85bwFRF2XoUNKhkVzIK/X0iO4PztEG8FNkJ
            TkpHc9WacKKRjZAQA1R67AuFimSeprPP7hg8J0CL/9aEQKyABp3Hgk67ZJ7kM9PL
            gi9vrxs1aEWc5kbXCPVqw46nt9B6U2GVLGQPl+NQ
            -----END CERTIFICATE-----
            """;

    private TestTrustMaterialFixtures() {
    }

    static X509Certificate loadCertificate() throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (InputStream in = new java.io.ByteArrayInputStream(TEST_CERT_PEM.getBytes(StandardCharsets.US_ASCII))) {
            return (X509Certificate) certificateFactory.generateCertificate(in);
        }
    }

    static Path writePemCertificateFile() throws IOException {
        Path file = Files.createTempFile("easy-postman-custom-ca", ".pem");
        Files.writeString(file, TEST_CERT_PEM, StandardCharsets.US_ASCII);
        return file;
    }

    static Path writeJksTrustStore(String password) throws Exception {
        X509Certificate certificate = loadCertificate();
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setCertificateEntry("test-cert", certificate);

        Path file = Files.createTempFile("easy-postman-custom-ca", ".jks");
        try (var out = Files.newOutputStream(file)) {
            keyStore.store(out, password.toCharArray());
        }
        return file;
    }

    static void assertTrusts(X509TrustManager trustManager) throws Exception {
        X509Certificate certificate = loadCertificate();
        trustManager.checkServerTrusted(new X509Certificate[]{certificate}, "RSA");
    }
}
