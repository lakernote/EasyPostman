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
import static org.testng.Assert.assertTrue;

public class WindowsCertificateInstallServiceTest {
    private String previousDataDirectory;
    private String previousOsName;
    private Path tempDataDirectory;

    @BeforeMethod
    public void setUp() throws Exception {
        previousDataDirectory = System.getProperty("easyPostman.data.dir");
        previousOsName = System.getProperty("os.name");
        tempDataDirectory = Files.createTempDirectory("easy-postman-windows-cert-");
        System.setProperty("easyPostman.data.dir", tempDataDirectory.toString());
        System.setProperty("os.name", "Windows 11");
        SystemUtil.resetForTests();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (previousDataDirectory == null) {
            System.clearProperty("easyPostman.data.dir");
        } else {
            System.setProperty("easyPostman.data.dir", previousDataDirectory);
        }
        if (previousOsName == null) {
            System.clearProperty("os.name");
        } else {
            System.setProperty("os.name", previousOsName);
        }
        SystemUtil.resetForTests();
        deleteRecursively(tempDataDirectory);
    }

    @Test
    public void shouldResolveTrustWithPowerShellWhenWindowsRootKeyStoreIsUnavailable() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        List<List<String>> commands = new ArrayList<>();
        WindowsCertificateInstallService service = new WindowsCertificateInstallService(command -> {
            commands.add(command);
            return new WindowsCertificateInstallService.CommandResult(0, List.of("CurrentUser"));
        });

        WindowsCertificateInstallService.WindowsTrustStatus status = service.trustStatus(certificatePath);

        assertTrue(status.installed());
        assertTrue(status.trusted());
        assertTrue(commands.stream().anyMatch(command -> command.stream()
                        .anyMatch(part -> part.contains("Cert:\\CurrentUser\\Root") && part.contains("Cert:\\LocalMachine\\Root"))),
                "PowerShell trust check should inspect current-user and local-machine root stores");
    }

    @Test
    public void shouldInstallRootCaIntoCurrentUserRootStoreWithCertutil() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        List<List<String>> commands = new ArrayList<>();
        WindowsCertificateInstallService service = new WindowsCertificateInstallService(command -> {
            commands.add(command);
            return new WindowsCertificateInstallService.CommandResult(0, List.of());
        });

        service.installToCurrentUserRoot(certificatePath);

        assertTrue(commands.stream().anyMatch(command -> command.equals(List.of(
                "certutil", "-user", "-f", "-addstore", "Root", certificatePath
        ))), "Install should use non-interactive current-user Root store import first");
    }

    @Test
    public void shouldFallbackToPowerShellImportWhenCertutilFails() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        List<List<String>> commands = new ArrayList<>();
        WindowsCertificateInstallService service = new WindowsCertificateInstallService(command -> {
            commands.add(command);
            if ("certutil".equals(command.get(0))) {
                return new WindowsCertificateInstallService.CommandResult(1, List.of("certutil failed"));
            }
            return new WindowsCertificateInstallService.CommandResult(0, List.of());
        });

        service.installToCurrentUserRoot(certificatePath);

        assertTrue(commands.stream().anyMatch(command -> command.stream()
                        .anyMatch(part -> part.contains("Import-Certificate") && part.contains("Cert:\\CurrentUser\\Root"))),
                "Install should fall back to PowerShell CurrentUser Root import when certutil fails");
    }

    @Test
    public void shouldRemoveRootCaFromCurrentUserRootStoreWithCertutil() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        List<List<String>> commands = new ArrayList<>();
        WindowsCertificateInstallService service = new WindowsCertificateInstallService(command -> {
            commands.add(command);
            return new WindowsCertificateInstallService.CommandResult(0, List.of());
        });

        int removed = service.removeFromCurrentUserRoot(certificatePath);

        assertEquals(removed, 1);
        assertTrue(commands.stream().anyMatch(command -> command.size() == 5
                        && command.subList(0, 4).equals(List.of("certutil", "-user", "-delstore", "Root"))
                        && command.get(4).matches("[0-9A-F]{40}")),
                "Uninstall should use non-interactive current-user Root store removal first");
    }

    @Test
    public void shouldFallbackToPowerShellRemoveWhenCertutilFails() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        List<List<String>> commands = new ArrayList<>();
        WindowsCertificateInstallService service = new WindowsCertificateInstallService(command -> {
            commands.add(command);
            if ("certutil".equals(command.get(0))) {
                return new WindowsCertificateInstallService.CommandResult(1, List.of("certutil failed"));
            }
            return new WindowsCertificateInstallService.CommandResult(0, List.of("removed"));
        });

        int removed = service.removeFromCurrentUserRoot(certificatePath);

        assertEquals(removed, 1);
        assertTrue(commands.stream().anyMatch(command -> command.stream()
                        .anyMatch(part -> part.contains("Remove-Item") && part.contains("Cert:\\CurrentUser\\Root"))),
                "Uninstall should fall back to PowerShell CurrentUser Root removal when certutil fails");
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
