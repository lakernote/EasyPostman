package com.laker.postman.plugin.runtime;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PluginRuntimePathsTest {

    private Path dataDir;
    private Path appDir;

    @BeforeMethod
    public void setUp() throws IOException {
        dataDir = Files.createTempDirectory("plugin-runtime-data");
        appDir = Files.createTempDirectory("plugin-runtime-app");
        System.setProperty("easyPostman.data.dir", dataDir.toString());
        System.setProperty("easyPostman.app.dir", appDir.toString());
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty("easyPostman.data.dir");
        System.clearProperty("easyPostman.app.dir");
        System.clearProperty("easyPostman.portable");
        PluginRuntime.resetForTests();
    }

    @Test
    public void shouldUseApplicationPluginsDirectoryInPortableMode() {
        System.setProperty("easyPostman.portable", "true");
        PluginRuntime.resetForTests();

        assertEquals(PluginRuntime.getManagedPluginDir(), appDir.resolve("plugins"));
        assertEquals(PluginRuntime.getPluginPackageDir(), appDir.resolve("plugins").resolve("packages"));
        assertTrue(Files.isDirectory(appDir.resolve("plugins")));
        assertTrue(Files.isDirectory(appDir.resolve("plugins").resolve("packages")));
    }

    @Test
    public void shouldDiscoverDroppedPluginJarFromApplicationPluginsDirectory() throws Exception {
        Path pluginJar = appDir.resolve("plugins").resolve("plugin-redis-5.3.16.jar");
        writeStubPluginJar(pluginJar, "plugin-redis", "5.3.16");

        List<PluginFileInfo> installedPlugins = PluginRuntime.getInstalledPlugins();

        assertEquals(installedPlugins.size(), 1);
        assertEquals(installedPlugins.get(0).descriptor().id(), "plugin-redis");
        assertEquals(installedPlugins.get(0).jarPath(), pluginJar);
    }

    @Test
    public void shouldKeepInstalledEditionManagedPluginsUnderDataDirectory() {
        assertEquals(PluginRuntime.getManagedPluginDir(), dataDir.resolve("plugins").resolve("installed"));
        assertEquals(PluginRuntime.getPluginPackageDir(), dataDir.resolve("plugins").resolve("packages"));
    }

    private static void writeStubPluginJar(Path jarPath, String pluginId, String version) throws IOException {
        Files.createDirectories(jarPath.getParent());
        try (OutputStream outputStream = Files.newOutputStream(jarPath);
             JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            jarOutputStream.putNextEntry(new JarEntry("META-INF/easy-postman/" + pluginId + ".properties"));
            jarOutputStream.write(("""
                    plugin.id=%s
                    plugin.name=Stub Plugin
                    plugin.version=%s
                    plugin.entryClass=com.example.StubPlugin
                    plugin.minPlatformVersion=
                    plugin.maxPlatformVersion=
                    """.formatted(pluginId, version)).getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }
    }
}
