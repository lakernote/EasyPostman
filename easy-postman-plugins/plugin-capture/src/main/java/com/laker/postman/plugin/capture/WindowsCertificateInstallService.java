package com.laker.postman.plugin.capture;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

final class WindowsCertificateInstallService {
    private static final String CERTUTIL = "certutil";
    private static final String CMD = "cmd";
    private static final String POWERSHELL = "powershell";
    private static final int POWERSHELL_CERT_NOT_FOUND_EXIT_CODE = 2;
    private static final String CURRENT_USER_ROOT_STORE = "Cert:\\CurrentUser\\Root";
    private static final String LOCAL_MACHINE_ROOT_STORE = "Cert:\\LocalMachine\\Root";
    private final CommandRunner commandRunner;
    private final String osName;

    WindowsCertificateInstallService() {
        this(new ProcessCommandRunner(), System.getProperty("os.name", ""));
    }

    WindowsCertificateInstallService(CommandRunner commandRunner) {
        this(commandRunner, System.getProperty("os.name", ""));
    }

    WindowsCertificateInstallService(CommandRunner commandRunner, String osName) {
        this.commandRunner = commandRunner == null ? new ProcessCommandRunner() : commandRunner;
        this.osName = osName == null ? "" : osName;
    }

    boolean isSupported() {
        return osName.toLowerCase(Locale.ROOT).contains("win");
    }

    void installToCurrentUserRoot(String certificatePath) throws Exception {
        ensureSupported();
        Exception certutilFailure = null;
        try {
            runCommand(List.of(CERTUTIL, "-user", "-f", "-addstore", "Root", certificatePath));
            return;
        } catch (Exception ex) {
            certutilFailure = ex;
        }
        try {
            runCommand(List.of(
                    POWERSHELL,
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-Command",
                    buildImportCertificateScript(certificatePath)
            ));
        } catch (Exception ex) {
            if (certutilFailure != null) {
                ex.addSuppressed(certutilFailure);
            }
            throw ex;
        }
    }

    WindowsTrustStatus trustStatus(String certificatePath) throws Exception {
        ensureSupported();
        X509Certificate target = readCertificate(certificatePath);
        WindowsTrustStatus keyStoreStatus = null;
        Exception keyStoreFailure = null;
        try {
            keyStoreStatus = trustStatusFromWindowsRootKeyStore(target);
            if (keyStoreStatus.trusted()) {
                return keyStoreStatus;
            }
        } catch (Exception ex) {
            keyStoreFailure = ex;
        }
        try {
            WindowsTrustStatus powershellStatus = trustStatusFromPowerShellRootStores(target);
            if (powershellStatus.installed() || keyStoreStatus == null) {
                return powershellStatus;
            }
            return keyStoreStatus;
        } catch (Exception ex) {
            if (keyStoreStatus != null) {
                return keyStoreStatus;
            }
            if (keyStoreFailure != null) {
                ex.addSuppressed(keyStoreFailure);
            }
            throw ex;
        }
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

    private WindowsTrustStatus trustStatusFromWindowsRootKeyStore(X509Certificate target) throws Exception {
        KeyStore rootStore = KeyStore.getInstance("Windows-ROOT");
        rootStore.load(null, null);
        Enumeration<String> aliases = rootStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate certificate = rootStore.getCertificate(alias);
            if (certificate instanceof X509Certificate existing
                    && java.util.Arrays.equals(existing.getEncoded(), target.getEncoded())) {
                return new WindowsTrustStatus(true, true,
                        "Root CA installed in the Windows Root certificate store");
            }
        }
        return new WindowsTrustStatus(false, false,
                "Root CA not found in the Windows Root certificate store");
    }

    private WindowsTrustStatus trustStatusFromPowerShellRootStores(X509Certificate target) throws Exception {
        String thumbprint = sha1Fingerprint(target);
        CommandResult result = runCommandAllowFailure(List.of(
                POWERSHELL,
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                buildTrustCheckScript(thumbprint)
        ));
        if (result.exitCode() == 0) {
            String store = result.lines().stream().findFirst().orElse(CURRENT_USER_ROOT_STORE);
            return new WindowsTrustStatus(true, true, trustDetailForStore(store));
        }
        if (result.exitCode() == POWERSHELL_CERT_NOT_FOUND_EXIT_CODE) {
            return new WindowsTrustStatus(false, false,
                    "Root CA not found in the Windows Root certificate stores");
        }
        throw new IllegalStateException(String.join(System.lineSeparator(), result.lines()));
    }

    private String buildImportCertificateScript(String certificatePath) {
        return "$ErrorActionPreference='Stop';"
                + "Import-Certificate -FilePath " + powerShellSingleQuoted(certificatePath)
                + " -CertStoreLocation " + powerShellSingleQuoted(CURRENT_USER_ROOT_STORE)
                + " | Out-Null";
    }

    private String buildTrustCheckScript(String thumbprint) {
        return "$ErrorActionPreference='Stop';"
                + "$thumb=" + powerShellSingleQuoted(thumbprint) + ";"
                + "$stores=@("
                + powerShellSingleQuoted(CURRENT_USER_ROOT_STORE) + ","
                + powerShellSingleQuoted(LOCAL_MACHINE_ROOT_STORE)
                + ");"
                + "foreach($store in $stores){"
                + "$cert=Get-ChildItem -Path $store -ErrorAction SilentlyContinue "
                + "| Where-Object { (($_.Thumbprint -replace '\\s','').ToUpperInvariant()) -eq $thumb } "
                + "| Select-Object -First 1;"
                + "if($cert){ Write-Output $store; exit 0 }"
                + "};"
                + "exit " + POWERSHELL_CERT_NOT_FOUND_EXIT_CODE;
    }

    private String trustDetailForStore(String store) {
        if (LOCAL_MACHINE_ROOT_STORE.equalsIgnoreCase(store)) {
            return "Root CA installed in the Windows Local Machine Root store";
        }
        return "Root CA installed in the Windows Current User Root store";
    }

    private String sha1Fingerprint(X509Certificate certificate) throws Exception {
        return HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-1").digest(certificate.getEncoded()));
    }

    private String powerShellSingleQuoted(String value) {
        return "'" + (value == null ? "" : value.replace("'", "''")) + "'";
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
        return runCommand(List.of(command));
    }

    private List<String> runCommand(List<String> command) throws Exception {
        CommandResult result = runCommandAllowFailure(command);
        if (result.exitCode() != 0) {
            throw new IllegalStateException(String.join(System.lineSeparator(), result.lines()));
        }
        return result.lines();
    }

    private CommandResult runCommandAllowFailure(List<String> command) throws Exception {
        return commandRunner.run(command);
    }

    @FunctionalInterface
    interface CommandRunner {
        CommandResult run(List<String> command) throws Exception;
    }

    record CommandResult(int exitCode, List<String> lines) {
    }

    private static final class ProcessCommandRunner implements CommandRunner {
        @Override
        public CommandResult run(List<String> command) throws Exception {
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
            return new CommandResult(exitCode, lines);
        }
    }

    record WindowsTrustStatus(boolean installed, boolean trusted, String detail) {
    }
}
