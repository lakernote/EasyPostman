package com.laker.postman.plugin.capture;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
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
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

final class MacCertificateInstallService {
    private static final String OPEN = "/usr/bin/open";
    private static final String OSASCRIPT = "/usr/bin/osascript";
    private static final String SECURITY = "/usr/bin/security";
    private static final Pattern SHA256_PATTERN = Pattern.compile("^SHA-256 hash:\\s*(\\S+)$");
    private static final Pattern COMMON_NAME_PATTERN = Pattern.compile("(?:^|,)CN=([^,]+)");
    private final CommandRunner commandRunner;
    private final String osName;

    MacCertificateInstallService() {
        this(new ProcessCommandRunner(), System.getProperty("os.name", ""));
    }

    MacCertificateInstallService(CommandRunner commandRunner, String osName) {
        this.commandRunner = commandRunner == null ? new ProcessCommandRunner() : commandRunner;
        this.osName = osName == null ? "" : osName;
    }

    boolean isSupported() {
        return osName.toLowerCase(Locale.ROOT).contains("mac");
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

    int removeMatchingSystemKeychainCertificatesWithPrompt(String certificatePath) throws Exception {
        ensureSupported();
        return removeMatchingCertificates(certificatePath, resolveSystemKeychain(), true);
    }

    CertificateTrustStatus trustStatus(String certificatePath) throws Exception {
        ensureSupported();
        X509Certificate certificate = readCertificate(certificatePath);
        String fingerprint = sha256(certificate);
        String subjectName = extractCommonName(certificate);

        KeychainTrust loginTrust = inspectKeychain(certificate, subjectName, fingerprint, certificatePath, resolveLoginKeychain(), "login keychain");
        if (loginTrust.installed() && loginTrust.trusted()) {
            return new CertificateTrustStatus(true, true,
                    "Root CA installed and accepted by macOS trust evaluation (" + loginTrust.location() + ")");
        }

        KeychainTrust systemTrust = inspectKeychain(certificate, subjectName, fingerprint, certificatePath, resolveSystemKeychain(), "System keychain");
        if (systemTrust.installed() && systemTrust.trusted()) {
            return new CertificateTrustStatus(true, true,
                    "Root CA installed and accepted by macOS trust evaluation (" + systemTrust.location() + ")");
        }

        if (loginTrust.trusted() || systemTrust.trusted()) {
            return new CertificateTrustStatus(true, true,
                    "Root CA accepted by macOS trust evaluation, but security find-certificate did not return the matching keychain item");
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
        return removeMatchingCertificates(certificatePath, keychain, false);
    }

    private int removeMatchingCertificates(String certificatePath, Path keychain, boolean adminPrompt) throws Exception {
        X509Certificate certificate = readCertificate(certificatePath);
        Set<String> fingerprints = new LinkedHashSet<>();
        fingerprints.add(sha256(certificate));
        fingerprints.addAll(findMatchingCertificateFingerprints(certificate, keychain));
        int removed = 0;
        for (String fingerprint : fingerprints) {
            CommandResult result = runDeleteCertificateCommand(fingerprint, keychain, adminPrompt);
            if (result.exitCode() == 0) {
                removed++;
                continue;
            }
            if (isCertificateNotFound(result.lines())) {
                continue;
            }
            throw new IllegalStateException(String.join(System.lineSeparator(), result.lines()));
        }
        return removed;
    }

    private Set<String> findMatchingCertificateFingerprints(X509Certificate certificate, Path keychain) throws Exception {
        return findMatchingCertificateFingerprints(certificate, extractCommonName(certificate), sha256(certificate), keychain);
    }

    private Set<String> findMatchingCertificateFingerprints(
            X509Certificate certificate,
            String subjectName,
            String fingerprint,
            Path keychain
    ) throws Exception {
        Set<String> fingerprints = new LinkedHashSet<>();
        try {
            fingerprints.addAll(findCertificateFingerprints(subjectName, keychain));
        } catch (Exception ignored) {
            // Fall back to exporting all certificates from the keychain and comparing locally.
        }
        for (X509Certificate existing : exportCertificates(keychain)) {
            if (sha256(existing).equalsIgnoreCase(fingerprint)
                    || extractCommonName(existing).equals(subjectName)) {
                fingerprints.add(sha256(existing));
            }
        }
        return fingerprints;
    }

    private List<X509Certificate> exportCertificates(Path keychain) throws Exception {
        CommandResult result = runCommandAllowFailure(SECURITY, "find-certificate", "-a", "-p", keychain.toString());
        if (result.exitCode() != 0 || result.lines().isEmpty()) {
            return List.of();
        }
        String pem = String.join(System.lineSeparator(), result.lines());
        Collection<? extends java.security.cert.Certificate> certificates =
                CertificateFactory.getInstance("X.509")
                        .generateCertificates(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
        List<X509Certificate> resultCertificates = new ArrayList<>();
        for (java.security.cert.Certificate exported : certificates) {
            if (exported instanceof X509Certificate x509Certificate) {
                resultCertificates.add(x509Certificate);
            }
        }
        return resultCertificates;
    }

    private String[] deleteCertificateCommand(String fingerprint, Path keychain) {
        return new String[]{
                SECURITY,
                "delete-certificate",
                "-Z",
                fingerprint,
                "-t",
                keychain.toString()
        };
    }

    private CommandResult runDeleteCertificateCommand(String fingerprint, Path keychain, boolean adminPrompt) throws Exception {
        String[] command = deleteCertificateCommand(fingerprint, keychain);
        if (!adminPrompt) {
            return runCommandAllowFailure(command);
        }
        return runCommandAllowFailure(
                OSASCRIPT,
                "-e",
                "do shell script " + appleScriptString(shellJoin(command)) + " with administrator privileges"
        );
    }

    private boolean isCertificateNotFound(List<String> lines) {
        String message = String.join("\n", lines).toLowerCase(Locale.ROOT);
        return message.contains("could not be found")
                || message.contains("not found")
                || message.contains("unable to find")
                || message.contains("unable to delete certificate matching")
                || message.contains("no certificate")
                || message.contains("secitemcopymatching")
                || message.contains("seckeychainsearchcopynext");
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

    private KeychainTrust inspectKeychain(
            X509Certificate certificate,
            String subjectName,
            String fingerprint,
            String certificatePath,
            Path keychain,
            String location
    ) throws Exception {
        boolean installed = false;
        try {
            installed = findMatchingCertificateFingerprints(certificate, subjectName, fingerprint, keychain)
                    .stream()
                    .anyMatch(existing -> existing.equalsIgnoreCase(fingerprint));
        } catch (Exception ignored) {
            // Continue with trust evaluation; some macOS keychain views expose certificates that find-certificate -c
            // does not return even though verify-cert accepts the current root.
        }
        boolean trusted = false;
        try {
            trusted = verifyCertificateTrust(certificatePath, keychain);
        } catch (Exception ignored) {
            // Ignore keychain-specific trust evaluation failures and fall back to the remaining checks.
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
        return commandRunner.run(List.of(command));
    }

    record CertificateTrustStatus(boolean installed, boolean trusted, String detail) {
    }

    private record KeychainTrust(String location, boolean installed, boolean trusted) {
    }

    record CommandResult(int exitCode, List<String> lines) {
    }

    interface CommandRunner {
        CommandResult run(List<String> command) throws Exception;
    }

    private static final class ProcessCommandRunner implements CommandRunner {
        @Override
        public CommandResult run(List<String> command) throws Exception {
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
    }
}
