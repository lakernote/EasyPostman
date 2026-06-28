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
    public void shouldInstallRootCaIntoCurrentUserRootStoreWithPowerShell() throws Exception {
        String certificatePath = new CaptureCertificateService().rootCertificatePath();
        List<List<String>> commands = new ArrayList<>();
        WindowsCertificateInstallService service = new WindowsCertificateInstallService(command -> {
            commands.add(command);
            return new WindowsCertificateInstallService.CommandResult(0, List.of());
        });

        service.installToCurrentUserRoot(certificatePath);

        assertTrue(commands.stream().anyMatch(command -> command.stream()
                        .anyMatch(part -> part.contains("Import-Certificate") && part.contains("Cert:\\CurrentUser\\Root"))),
                "Install should use the current-user Windows root certificate store");
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
