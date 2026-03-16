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

    @Test
    public void shouldExposeOfficialCatalogUrls() {
        assertEquals(
                PluginCatalogService.getOfficialCatalogUrl("github"),
                "https://raw.githubusercontent.com/lakernote/easy-postman/master/plugin-catalog/catalog-github.json"
        );
        assertEquals(
                PluginCatalogService.getOfficialCatalogUrl("gitee"),
                "https://gitee.com/lakernote/easy-postman/raw/master/plugin-catalog/catalog-gitee.json"
        );
        assertEquals(
                PluginCatalogService.getOfficialCatalogUrl("auto"),
                "https://gitee.com/lakernote/easy-postman/raw/master/plugin-catalog/catalog-gitee.json"
        );
        assertEquals(
                PluginCatalogService.detectOfficialCatalogSource(PluginCatalogService.getOfficialCatalogUrl("github")),
                "github"
        );
        assertEquals(
                PluginCatalogService.detectOfficialCatalogSource(PluginCatalogService.getOfficialCatalogUrl("gitee")),
                "gitee"
        );
    }

    @Test
    public void shouldLoadBundledOfficialCatalog() throws Exception {
        List<PluginCatalogEntry> githubEntries = PluginCatalogService.loadBundledOfficialCatalog("github");
        List<PluginCatalogEntry> giteeEntries = PluginCatalogService.loadBundledOfficialCatalog("gitee");

        assertEquals(githubEntries.size(), 4);
        assertEquals(giteeEntries.size(), 4);
        assertEquals(githubEntries.get(0).id(), "plugin-redis");
        assertTrue(githubEntries.get(0).installUrl().startsWith("https://github.com/lakernote/easy-postman/"));
        assertTrue(giteeEntries.get(0).installUrl().startsWith("https://gitee.com/lakernote/easy-postman/"));
    }
}
