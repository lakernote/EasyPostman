package com.laker.postman.plugin.manager;

import com.laker.postman.plugin.runtime.PluginRuntime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PluginManagementServiceTest {

    private Path dataDir;

    @BeforeMethod
    public void setUp() throws IOException {
        dataDir = Files.createTempDirectory("plugin-manager-test");
        System.setProperty("easyPostman.data.dir", dataDir.toString());
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty("easyPostman.data.dir");
        PluginRuntime.resetForTests();
    }

    @Test
    public void shouldRemoveUnloadedManagedPluginImmediately() throws Exception {
        Path pluginJar = PluginManagementService.getManagedPluginDir().resolve("plugin-redis-5.3.16.jar");
        writeStubPluginJar(pluginJar, "plugin-redis");

        PluginUninstallResult result = PluginManagementService.uninstallPlugin("plugin-redis");

        assertTrue(result.removed());
        assertFalse(result.restartRequired());
        assertFalse(Files.exists(pluginJar));
        assertFalse(PluginManagementService.isPluginPendingUninstall("plugin-redis"));
    }

    @Test
    public void shouldPersistInstallSourceMetadata() {
        Path localJar = dataDir.resolve("downloads").resolve("plugin-kafka-5.3.17.jar");

        PluginInstallSourceStore.recordLocalInstall("plugin-kafka", localJar);
        PluginInstallSource localSource = PluginManagementService.getInstallSource("plugin-kafka");

        assertNotNull(localSource);
        assertTrue(localSource.isLocal());
        assertEquals(localSource.location(), localJar.toAbsolutePath().normalize().toString());

        PluginInstallSourceStore.recordMarketInstall("plugin-kafka", "https://example.com/plugin-kafka.jar");
        PluginInstallSource marketSource = PluginManagementService.getInstallSource("plugin-kafka");

        assertNotNull(marketSource);
        assertTrue(marketSource.isMarket());
        assertEquals(marketSource.location(), "https://example.com/plugin-kafka.jar");

        PluginInstallSourceStore.clear("plugin-kafka");
        assertNull(PluginManagementService.getInstallSource("plugin-kafka"));
    }

    @Test
    public void shouldRejectIncompatiblePluginBeforeInstall() throws Exception {
        Path sourceJar = dataDir.resolve("downloads").resolve("plugin-redis-9.0.0.jar");
        writeStubPluginJar(sourceJar, "plugin-redis", "9.0.0", "9.0.0", "9.0.0");

        IllegalStateException error;
        try {
            PluginManagementService.installPluginJar(sourceJar);
            throw new AssertionError("Expected incompatible plugin install to fail");
        } catch (IllegalStateException e) {
            error = e;
        }

        assertTrue(error.getMessage().contains("not compatible"));
        try (var files = Files.list(PluginManagementService.getManagedPluginDir())) {
            assertTrue(files.findAny().isEmpty());
        }
    }

    private static void writeStubPluginJar(Path jarPath, String pluginId) throws IOException {
        writeStubPluginJar(jarPath, pluginId, "5.3.16", "", "");
    }

    private static void writeStubPluginJar(Path jarPath,
                                           String pluginId,
                                           String pluginVersion,
                                           String minPlatformVersion,
                                           String maxPlatformVersion) throws IOException {
        Files.createDirectories(jarPath.getParent());
        try (OutputStream outputStream = Files.newOutputStream(jarPath);
             JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            jarOutputStream.putNextEntry(new JarEntry("META-INF/easy-postman/" + pluginId + ".properties"));
            jarOutputStream.write(("""
                    plugin.id=%s
                    plugin.name=Stub Plugin
                    plugin.version=%s
                    plugin.entryClass=com.example.StubPlugin
                    plugin.minPlatformVersion=%s
                    plugin.maxPlatformVersion=%s
                    """.formatted(pluginId, pluginVersion, minPlatformVersion, maxPlatformVersion))
                    .getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }
    }
}
