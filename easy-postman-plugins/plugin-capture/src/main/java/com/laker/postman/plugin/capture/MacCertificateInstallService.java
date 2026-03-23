package com.laker.postman.plugin.capture;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

final class MacCertificateInstallService {
    private static final String OPEN = "/usr/bin/open";
    private static final String OSASCRIPT = "/usr/bin/osascript";
    private static final String SECURITY = "/usr/bin/security";
    private static final Pattern SHA256_PATTERN = Pattern.compile("^SHA-256 hash:\\s*(\\S+)$");
    private static final Pattern COMMON_NAME_PATTERN = Pattern.compile("(?:^|,)CN=([^,]+)");

    boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    void installToLoginKeychain(String certificatePath) throws Exception {
        ensureSupported();
        runCommand(addTrustedCertCommand(resolveLoginKeychain(), certificatePath, false));
    }

    void installToSystemKeychainWithPrompt(String certificatePath) throws Exception {
        ensureSupported();
        String shellCommand = shellJoin(addTrustedCertCommand(resolveSystemKeychain(), certificatePath, true));
        runCommand(
                OSASCRIPT,
                "-e",
                "do shell script " + appleScriptString(shellCommand) + " with administrator privileges"
        );
    }

    int removeMatchingLoginKeychainCertificates(String certificatePath) throws Exception {
        ensureSupported();
        return removeMatchingCertificates(certificatePath, resolveLoginKeychain());
    }

    CertificateTrustStatus trustStatus(String certificatePath) throws Exception {
        ensureSupported();
        X509Certificate certificate = readCertificate(certificatePath);
        String fingerprint = sha256(certificate);
        String subjectName = extractCommonName(certificate);

        KeychainTrust loginTrust = inspectKeychain(subjectName, fingerprint, certificatePath, resolveLoginKeychain(), "login keychain");
        if (loginTrust.installed() && loginTrust.trusted()) {
            return new CertificateTrustStatus(true, true,
                    "Root CA installed and accepted by macOS trust evaluation (" + loginTrust.location() + ")");
        }

        KeychainTrust systemTrust = inspectKeychain(subjectName, fingerprint, certificatePath, resolveSystemKeychain(), "System keychain");
        if (systemTrust.installed() && systemTrust.trusted()) {
            return new CertificateTrustStatus(true, true,
                    "Root CA installed and accepted by macOS trust evaluation (" + systemTrust.location() + ")");
        }

        if (loginTrust.installed() || systemTrust.installed()) {
            String location = loginTrust.installed() ? loginTrust.location() : systemTrust.location();
            return new CertificateTrustStatus(true, false,
                    "Root CA is present in the " + location + ", but macOS trust evaluation is not effective yet");
        }
        return new CertificateTrustStatus(false, false,
                "Root CA not found in macOS login or System keychain");
    }

    void openCertificate(String certificatePath) throws Exception {
        ensureSupported();
        File certificate = new File(certificatePath);
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(certificate);
            return;
        }
        runCommand(OPEN, certificate.getAbsolutePath());
    }

    void openKeychainAccess() throws Exception {
        ensureSupported();
        runCommand(OPEN, "-a", "Keychain Access");
    }

    private void ensureSupported() {
        if (!isSupported()) {
            throw new IllegalStateException("Certificate install helper is only supported on macOS");
        }
    }

    private Path resolveLoginKeychain() {
        Path modern = Paths.get(System.getProperty("user.home"), "Library", "Keychains", "login.keychain-db");
        if (Files.exists(modern)) {
            return modern;
        }
        return Paths.get(System.getProperty("user.home"), "Library", "Keychains", "login.keychain");
    }

    private Path resolveSystemKeychain() {
        return Paths.get("/Library", "Keychains", "System.keychain");
    }

    private X509Certificate readCertificate(String certificatePath) throws Exception {
        try (FileInputStream input = new FileInputStream(certificatePath)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(input);
        }
    }

    private String sha256(X509Certificate certificate) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        return HexFormat.of().withUpperCase().formatHex(digest);
    }

    private String extractCommonName(X509Certificate certificate) {
        String subject = certificate.getSubjectX500Principal().getName(X500Principal.RFC2253);
        Matcher matcher = COMMON_NAME_PATTERN.matcher(subject);
        return matcher.find() ? matcher.group(1) : subject;
    }

    private List<String> findCertificateFingerprints(String subjectName, Path keychain) throws Exception {
        List<String> lines = runCommand(
                SECURITY,
                "find-certificate",
                "-a",
                "-Z",
                "-c",
                subjectName,
                keychain.toString()
        );
        List<String> fingerprints = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = SHA256_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                fingerprints.add(matcher.group(1));
            }
        }
        return fingerprints;
    }

    private int removeMatchingCertificates(String certificatePath, Path keychain) throws Exception {
        X509Certificate certificate = readCertificate(certificatePath);
        List<String> fingerprints = findCertificateFingerprints(extractCommonName(certificate), keychain);
        int removed = 0;
        for (String fingerprint : fingerprints) {
            runCommand(
                    SECURITY,
                    "delete-certificate",
                    "-Z",
                    fingerprint,
                    keychain.toString()
            );
            removed++;
        }
        return removed;
    }

    private boolean verifyCertificateTrust(String certificatePath, Path keychain) throws Exception {
        CommandResult result = runCommandAllowFailure(
                SECURITY,
                "verify-cert",
                "-L",
                "-l",
                "-p",
                "basic",
                "-c",
                certificatePath,
                "-k",
                keychain.toString()
        );
        for (String line : result.lines()) {
            if (line.contains("Cert Verify Result: No error.")) {
                return true;
            }
        }
        return result.exitCode() == 0;
    }

    private KeychainTrust inspectKeychain(String subjectName, String fingerprint, String certificatePath, Path keychain, String location) throws Exception {
        boolean installed = false;
        try {
            installed = findCertificateFingerprints(subjectName, keychain)
                    .stream()
                    .anyMatch(existing -> existing.equalsIgnoreCase(fingerprint));
        } catch (Exception ignored) {
            // If the certificate cannot be found in the keychain, do not infer trust from verify-cert alone.
            // For a self-signed root CA, verify-cert may still report success even when the CA is not installed.
        }
        boolean trusted = false;
        if (installed) {
            try {
                trusted = verifyCertificateTrust(certificatePath, keychain);
            } catch (Exception ignored) {
                // Ignore keychain-specific trust evaluation failures and fall back to the remaining checks.
            }
        }
        return new KeychainTrust(location, installed, trusted);
    }

    private String[] addTrustedCertCommand(Path keychain, String certificatePath, boolean adminDomain) {
        List<String> command = new ArrayList<>();
        command.add(SECURITY);
        command.add("add-trusted-cert");
        if (adminDomain) {
            command.add("-d");
        }
        command.add("-r");
        command.add("trustRoot");
        command.add("-p");
        command.add("ssl");
        command.add("-p");
        command.add("basic");
        command.add("-k");
        command.add(keychain.toString());
        command.add(certificatePath);
        return command.toArray(String[]::new);
    }

    private String shellJoin(String[] command) {
        List<String> quoted = new ArrayList<>(command.length);
        for (String part : command) {
            quoted.add(shellQuote(part));
        }
        return String.join(" ", quoted);
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private String appleScriptString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private List<String> runCommand(String... command) throws Exception {
        CommandResult result = runCommandAllowFailure(command);
        if (result.exitCode() != 0) {
            throw new IllegalStateException(String.join(System.lineSeparator(), result.lines()));
        }
        return result.lines();
    }

    private CommandResult runCommandAllowFailure(String... command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return new CommandResult(process.waitFor(), lines);
    }

    record CertificateTrustStatus(boolean installed, boolean trusted, String detail) {
    }

    private record KeychainTrust(String location, boolean installed, boolean trusted) {
    }

    private record CommandResult(int exitCode, List<String> lines) {
    }
}
