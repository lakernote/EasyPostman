package com.laker.postman.plugin.runtime;

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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PluginRuntimeTest {

    private Path dataDir;

    @BeforeMethod
    public void setUp() throws IOException {
        dataDir = Files.createTempDirectory("plugin-runtime-test");
        System.setProperty("easyPostman.data.dir", dataDir.toString());
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty("easyPostman.data.dir");
        PluginRuntime.resetForTests();
    }

    @Test
    public void shouldClearPendingUninstallWhenPluginIsEnabledAgain() {
        PluginRuntime.markPluginPendingUninstall("plugin-redis");

        assertTrue(PluginRuntime.isPluginPendingUninstall("plugin-redis"));
        assertFalse(PluginRuntime.isPluginEnabled("plugin-redis"));

        PluginRuntime.setPluginEnabled("plugin-redis", true);

        assertFalse(PluginRuntime.isPluginPendingUninstall("plugin-redis"));
        assertTrue(PluginRuntime.isPluginEnabled("plugin-redis"));
    }

    @Test
    public void shouldDeleteManagedPluginFilesDuringPendingUninstallCleanup() throws Exception {
        Path pluginJar = PluginRuntime.getManagedPluginDir().resolve("plugin-redis-4.3.55.jar");
        writeStubPluginJar(pluginJar, "plugin-redis");
        PluginRuntime.markPluginPendingUninstall("plugin-redis");

        PluginRuntime.cleanupPendingUninstallPlugins();

        assertFalse(Files.exists(pluginJar));
        assertFalse(PluginRuntime.isPluginPendingUninstall("plugin-redis"));
    }

    private static void writeStubPluginJar(Path jarPath, String pluginId) throws IOException {
        Files.createDirectories(jarPath.getParent());
        try (OutputStream outputStream = Files.newOutputStream(jarPath);
             JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            jarOutputStream.putNextEntry(new JarEntry("META-INF/easy-postman/" + pluginId + ".properties"));
            jarOutputStream.write(("""
                    plugin.id=%s
                    plugin.name=Stub Plugin
                    plugin.version=4.3.55
                    plugin.entryClass=com.example.StubPlugin
                    """.formatted(pluginId)).getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }
    }
}
