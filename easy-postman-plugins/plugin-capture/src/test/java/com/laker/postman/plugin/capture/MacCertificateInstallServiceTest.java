package com.laker.postman.plugin.capture;

import com.laker.postman.util.SystemUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MacCertificateInstallServiceTest {
    private String previousDataDirectory;
    private Path tempDataDirectory;

    @BeforeMethod
    public void setUp() throws Exception {
        previousDataDirectory = System.getProperty("easyPostman.data.dir");
        tempDataDirectory = Files.createTempDirectory("easy-postman-mac-cert-");
        System.setProperty("easyPostman.data.dir", tempDataDirectory.toString());
        SystemUtil.resetForTests();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (previousDataDirectory == null) {
            System.clearProperty("easyPostman.data.dir");
        } else {
            System.setProperty("easyPostman.data.dir", previousDataDirectory);
        }
        SystemUtil.resetForTests();
        deleteRecursively(tempDataDirectory);
    }

    @Test
    public void shouldTrustCurrentCaWhenVerifyCertSucceedsEvenIfFindCertificateReturnsNoMatch() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        List<List<String>> commands = new ArrayList<>();
        MacCertificateInstallService service = new MacCertificateInstallService(command -> {
            commands.add(command);
            if (command.contains("find-certificate")) {
                return new MacCertificateInstallService.CommandResult(0, List.of());
            }
            if (command.contains("verify-cert")) {
                return new MacCertificateInstallService.CommandResult(0, List.of("...certificate verification successful."));
            }
            return new MacCertificateInstallService.CommandResult(0, List.of());
        }, "Mac OS X");

        MacCertificateInstallService.CertificateTrustStatus status = service.trustStatus(certificatePath);

        assertTrue(status.installed());
        assertTrue(status.trusted());
        assertTrue(status.detail().contains("find-certificate"));
        assertTrue(commands.stream().anyMatch(command -> command.contains("find-certificate")));
        assertTrue(commands.stream().anyMatch(command -> command.contains("verify-cert")));
    }

    @Test
    public void shouldReportNotInstalledWhenFindCertificateAndVerifyCertFail() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        MacCertificateInstallService service = new MacCertificateInstallService(command -> {
            if (command.contains("find-certificate")) {
                return new MacCertificateInstallService.CommandResult(0, List.of());
            }
            if (command.contains("verify-cert")) {
                return new MacCertificateInstallService.CommandResult(1, List.of("Cert Verify Result: CSSMERR_TP_NOT_TRUSTED"));
            }
            return new MacCertificateInstallService.CommandResult(0, List.of());
        }, "Mac OS X");

        MacCertificateInstallService.CertificateTrustStatus status = service.trustStatus(certificatePath);

        assertFalse(status.installed());
        assertFalse(status.trusted());
    }

    @Test
    public void shouldDeleteCurrentCaByFingerprintWhenFindCertificateReturnsNoMatch() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        List<List<String>> commands = new ArrayList<>();
        MacCertificateInstallService service = new MacCertificateInstallService(command -> {
            commands.add(command);
            if (command.contains("find-certificate")) {
                return new MacCertificateInstallService.CommandResult(0, List.of());
            }
            if (command.contains("delete-certificate")) {
                return new MacCertificateInstallService.CommandResult(0, List.of());
            }
            return new MacCertificateInstallService.CommandResult(0, List.of());
        }, "Mac OS X");

        int removed = service.removeMatchingLoginKeychainCertificates(certificatePath);

        assertTrue(removed > 0);
        assertTrue(commands.stream().anyMatch(command -> command.contains("delete-certificate")
                        && command.contains("-Z")
                        && command.stream().anyMatch(part -> part.matches("[0-9A-F]{64}"))),
                "Uninstall should try deleting the current root CA by SHA-256 fingerprint");
    }

    @Test
    public void shouldTreatUnableToDeleteCertificateMatchingAsNotFound() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        MacCertificateInstallService service = new MacCertificateInstallService(command -> {
            if (command.contains("find-certificate")) {
                return new MacCertificateInstallService.CommandResult(0, List.of());
            }
            if (command.contains("delete-certificate")) {
                return new MacCertificateInstallService.CommandResult(
                        1,
                        List.of("Unable to delete certificate matching \"B13273530F683E6CD54B29D1413A573CAC01274294D3BC41A38E14EF2B14F7BC\"")
                );
            }
            return new MacCertificateInstallService.CommandResult(0, List.of());
        }, "Mac OS X");

        int removed = service.removeMatchingLoginKeychainCertificates(certificatePath);

        assertEquals(removed, 0);
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path current : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(current);
            }
        }
    }
}
