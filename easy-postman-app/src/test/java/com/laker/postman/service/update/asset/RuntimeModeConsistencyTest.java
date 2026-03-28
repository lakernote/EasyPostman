package com.laker.postman.service.update.asset;

import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.util.AppRuntimeLayout;
import com.laker.postman.util.SystemUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RuntimeModeConsistencyTest {

    private Path appDir;
    private Path dataDir;

    @BeforeMethod
    public void setUp() throws IOException {
        appDir = Files.createTempDirectory("runtime-mode-app");
        dataDir = Files.createTempDirectory("runtime-mode-data");
        clearOverrides();
        SystemUtil.resetForTests();
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void tearDown() {
        clearOverrides();
        SystemUtil.resetForTests();
        PluginRuntime.resetForTests();
    }

    @Test
    public void shouldResolvePortableModeConsistentlyAcrossModules() throws Exception {
        System.setProperty("easyPostman.app.dir", appDir.toString());
        Files.writeString(appDir.resolve(".portable"), "portable");

        assertTrue(WindowsVersionDetector.isPortableVersion());
        assertEquals(Paths.get(SystemUtil.getEasyPostmanPath()).normalize(), appDir.resolve("data"));
        assertEquals(AppRuntimeLayout.logRootDirectory(RuntimeModeConsistencyTest.class), appDir.resolve("logs"));
        assertEquals(PluginRuntime.getManagedPluginDir(), appDir.resolve("plugins"));
        assertEquals(PluginRuntime.getPluginPackageDir(), appDir.resolve("plugins").resolve("packages"));
    }

    @Test
    public void shouldTreatJarStyleLaunchAsInstalledModeByDefault() {
        System.setProperty("easyPostman.app.dir", appDir.toString());

        Path expectedDataRoot = Paths.get(System.getProperty("user.home"), "EasyPostman").toAbsolutePath().normalize();
        Path expectedLogRoot = expectedDataRoot.resolve("logs");

        assertFalse(WindowsVersionDetector.isPortableVersion());
        assertEquals(Paths.get(SystemUtil.getEasyPostmanPath()).normalize(), expectedDataRoot);
        assertEquals(AppRuntimeLayout.logRootDirectory(RuntimeModeConsistencyTest.class), expectedLogRoot);
        assertEquals(PluginRuntime.getManagedPluginDir(), expectedDataRoot.resolve("plugins").resolve("installed"));
        assertEquals(PluginRuntime.getPluginPackageDir(), expectedDataRoot.resolve("plugins").resolve("packages"));
    }

    @Test
    public void shouldRespectExplicitDataDirectoryOverrideInInstalledMode() {
        System.setProperty("easyPostman.app.dir", appDir.toString());
        System.setProperty("easyPostman.data.dir", dataDir.toString());

        assertFalse(WindowsVersionDetector.isPortableVersion());
        assertEquals(Paths.get(SystemUtil.getEasyPostmanPath()).normalize(), dataDir);
        assertEquals(PluginRuntime.getManagedPluginDir(), dataDir.resolve("plugins").resolve("installed"));
    }

    private static void clearOverrides() {
        System.clearProperty("easyPostman.app.dir");
        System.clearProperty("easyPostman.data.dir");
        System.clearProperty("easyPostman.portable");
    }
}
