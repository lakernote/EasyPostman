package com.laker.postman.service.http.ssl;

import com.laker.postman.model.TrustedCertificateEntry;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * Loads user-supplied CA bundles or truststores into an {@link X509TrustManager}.
 */
public final class CustomTrustMaterialLoader {

    private CustomTrustMaterialLoader() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static X509TrustManager loadTrustManager(String path, String password) throws Exception {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        TrustedCertificateEntry entry = new TrustedCertificateEntry();
        entry.setPath(path);
        entry.setPassword(password != null ? password : "");
        return loadTrustManager(List.of(entry));
    }

    public static X509TrustManager loadTrustManager(List<TrustedCertificateEntry> entries) throws Exception {
        List<TrustedCertificateEntry> activeEntries = new ArrayList<>();
        if (entries != null) {
            for (TrustedCertificateEntry entry : entries) {
                if (entry != null && entry.isEnabled() && entry.hasUsablePath()) {
                    activeEntries.add(entry);
                }
            }
        }

        if (activeEntries.isEmpty()) {
            return null;
        }

        KeyStore keyStore = mergeTrustStores(activeEntries);

        TrustManagerFactory factory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);

        for (TrustManager tm : factory.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }

        throw new IllegalStateException("No X509TrustManager found for custom trust material");
    }

    private static KeyStore mergeTrustStores(List<TrustedCertificateEntry> entries) throws Exception {
        KeyStore mergedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        mergedKeyStore.load(null, null);

        int aliasIndex = 0;
        for (TrustedCertificateEntry entry : entries) {
            File file = new File(entry.getPath().trim());
            if (!file.isFile()) {
                throw new IllegalArgumentException("Trust material file not found: " + file.getAbsolutePath());
            }
            KeyStore source = loadKeyStore(file, entry.getPassword());
            Enumeration<String> aliases = source.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate certificate = source.getCertificate(alias);
                if (certificate != null) {
                    mergedKeyStore.setCertificateEntry("custom-ca-" + aliasIndex++, certificate);
                }
            }
        }
        return mergedKeyStore;
    }

    private static KeyStore loadKeyStore(File file, String password) throws Exception {
        return isKeyStoreFile(file.getName())
                ? loadTrustStore(file, password)
                : loadCertificateBundle(file);
    }

    private static KeyStore loadTrustStore(File file, String password) throws Exception {
        String lower = file.getName().toLowerCase();
        String type = lower.endsWith(".jks") ? "JKS" : "PKCS12";
        KeyStore keyStore = KeyStore.getInstance(type);
        char[] pwd = password != null ? password.toCharArray() : new char[0];
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            keyStore.load(in, pwd);
        }
        return keyStore;
    }

    private static KeyStore loadCertificateBundle(File file) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            certificates = certificateFactory.generateCertificates(in);
        }

        if (certificates == null || certificates.isEmpty()) {
            throw new IllegalArgumentException("No certificates found in file: " + file.getAbsolutePath());
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        int index = 0;
        for (Certificate certificate : certificates) {
            keyStore.setCertificateEntry("custom-ca-" + index++, certificate);
        }
        return keyStore;
    }

    private static boolean isKeyStoreFile(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        return lower.endsWith(".jks") || lower.endsWith(".p12") || lower.endsWith(".pfx");
    }
}
