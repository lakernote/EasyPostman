package com.laker.postman.plugin.manager.market;

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PluginCatalogServiceTest {

    @Test
    public void shouldResolveRelativePluginJarAgainstLocalCatalogFile() throws Exception {
        Path tempDir = Files.createTempDirectory("plugin-catalog-test");
        Path pluginJar = tempDir.resolve("easy-postman-plugin-redis.jar");
        Path catalogFile = tempDir.resolve("catalog.json");

        Files.writeString(pluginJar, "stub", StandardCharsets.UTF_8);
        Files.writeString(catalogFile, """
                {
                  "plugins": [
                    {
                      "id": "plugin-redis",
                      "name": "Redis Plugin",
                      "version": "4.3.55",
                      "downloadUrl": "easy-postman-plugin-redis.jar"
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);

        List<PluginCatalogEntry> entries = PluginCatalogService.loadCatalog(catalogFile.toString());

        assertEquals(entries.size(), 1);
        PluginCatalogEntry entry = entries.get(0);
        assertEquals(entry.downloadUrl(), "easy-postman-plugin-redis.jar");
        assertEquals(Paths.get(java.net.URI.create(entry.installUrl())), pluginJar);
    }

    @Test
    public void shouldNormalizePlainCatalogPathToFileUri() throws Exception {
        Path tempDir = Files.createTempDirectory("plugin-catalog-location-test");
        Path catalogFile = tempDir.resolve("catalog.json");
        Files.writeString(catalogFile, "{\"plugins\":[]}", StandardCharsets.UTF_8);

        String normalized = PluginCatalogService.normalizeCatalogLocation(catalogFile.toString());

        assertTrue(normalized.startsWith("file:"));
        assertEquals(normalized, catalogFile.toUri().toString());
    }

    @Test
    public void shouldNormalizeCatalogDirectoryToCatalogJson() throws Exception {
        Path tempDir = Files.createTempDirectory("plugin-catalog-dir-test");
        Path catalogFile = tempDir.resolve("catalog.json");
        Files.writeString(catalogFile, "{\"plugins\":[]}", StandardCharsets.UTF_8);

        String normalized = PluginCatalogService.normalizeCatalogLocation(tempDir.toString());

        assertEquals(normalized, catalogFile.toUri().toString());
    }
}
