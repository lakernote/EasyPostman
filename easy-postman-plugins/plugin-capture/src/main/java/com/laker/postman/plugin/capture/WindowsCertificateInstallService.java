package com.laker.postman.plugin.capture;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

final class WindowsCertificateInstallService {
    private static final String CERTUTIL = "certutil";
    private static final String CMD = "cmd";

    boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    void installToCurrentUserRoot(String certificatePath) throws Exception {
        ensureSupported();
        runCommand(CERTUTIL, "-user", "-f", "-addstore", "Root", certificatePath);
    }

    WindowsTrustStatus trustStatus(String certificatePath) throws Exception {
        ensureSupported();
        X509Certificate target = readCertificate(certificatePath);
        KeyStore rootStore = KeyStore.getInstance("Windows-ROOT");
        rootStore.load(null, null);
        Enumeration<String> aliases = rootStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate certificate = rootStore.getCertificate(alias);
            if (certificate instanceof X509Certificate existing
                    && java.util.Arrays.equals(existing.getEncoded(), target.getEncoded())) {
                return new WindowsTrustStatus(true, true,
                        "Root CA installed in the Windows Current User Root store");
            }
        }
        return new WindowsTrustStatus(false, false,
                "Root CA not found in the Windows Current User Root store");
    }

    void openCertificate(String certificatePath) throws Exception {
        ensureSupported();
        File certificate = new File(certificatePath);
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(certificate);
            return;
        }
        runCommand(CMD, "/c", "start", "", certificate.getAbsolutePath());
    }

    void openCertificateManager() throws Exception {
        ensureSupported();
        runCommand(CMD, "/c", "start", "", "certmgr.msc");
    }

    private void ensureSupported() {
        if (!isSupported()) {
            throw new IllegalStateException("Certificate install helper is only supported on Windows");
        }
    }

    private X509Certificate readCertificate(String certificatePath) throws Exception {
        try (FileInputStream input = new FileInputStream(certificatePath)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
        }
    }

    private List<String> runCommand(String... command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(String.join(System.lineSeparator(), lines));
        }
        return lines;
    }

    record WindowsTrustStatus(boolean installed, boolean trusted, String detail) {
    }
}
