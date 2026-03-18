package com.laker.postman.plugin.manager.market;

import com.laker.postman.plugin.runtime.PluginRuntime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

public class PluginCatalogServiceTest {

    private Path dataDir;

    @BeforeMethod
    public void setUp() throws IOException {
        dataDir = Files.createTempDirectory("plugin-catalog-test");
        System.setProperty("easyPostman.data.dir", dataDir.toString());
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty("easyPostman.data.dir");
        PluginRuntime.resetForTests();
    }

    @Test
    public void shouldRejectLocalCatalogLocations() {
        assertThrows(IllegalArgumentException.class,
                () -> PluginCatalogService.normalizeCatalogLocation("/tmp/catalog.json"));
        assertThrows(IllegalArgumentException.class,
                () -> PluginCatalogService.normalizeCatalogLocation("file:///tmp/catalog.json"));
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
        assertEquals(
                githubEntries.stream().map(PluginCatalogEntry::id).collect(java.util.stream.Collectors.toSet()),
                Set.of("plugin-redis", "plugin-kafka", "plugin-decompiler", "plugin-client-cert")
        );
        assertEquals(
                giteeEntries.stream().map(PluginCatalogEntry::id).collect(java.util.stream.Collectors.toSet()),
                Set.of("plugin-redis", "plugin-kafka", "plugin-decompiler", "plugin-client-cert")
        );
        assertTrue(githubEntries.stream().noneMatch(entry -> "plugin-git".equals(entry.id())));
        assertTrue(giteeEntries.stream().noneMatch(entry -> "plugin-git".equals(entry.id())));

        PluginCatalogEntry githubKafka = githubEntries.stream()
                .filter(entry -> "plugin-kafka".equals(entry.id()))
                .findFirst()
                .orElse(null);
        PluginCatalogEntry giteeKafka = giteeEntries.stream()
                .filter(entry -> "plugin-kafka".equals(entry.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(githubKafka);
        assertNotNull(giteeKafka);
        assertTrue(githubKafka.installUrl().startsWith("https://github.com/"));
        assertTrue(githubKafka.installUrl().contains("/releases/download/"));
        assertTrue(giteeKafka.installUrl().startsWith("https://gitee.com/lakernote/easy-postman/"));
        assertNotNull(githubKafka.version());
        assertFalse(githubKafka.version().isBlank());
        assertNotNull(giteeKafka.version());
        assertFalse(giteeKafka.version().isBlank());
        assertEquals(githubKafka.minPlatformVersion(), "1.0.0");
        assertEquals(githubKafka.maxPlatformVersion(), "1.0.0");
        assertEquals(giteeKafka.minPlatformVersion(), "1.0.0");
        assertEquals(giteeKafka.maxPlatformVersion(), "1.0.0");
    }

    @Test
    public void shouldSelectHighestCompatibleReleaseFromNestedCatalog() throws Exception {
        String json = """
                {
                  "plugins": [
                    {
                      "id": "plugin-kafka",
                      "name": "Kafka Plugin",
                      "description": "Kafka toolbox panel",
                      "homepage": "https://example.com",
                      "releases": [
                        {
                          "version": "5.3.18",
                          "downloadUrl": "https://example.com/plugin-kafka-5.3.18.jar",
                          "sha256": "sha-518",
                          "minAppVersion": "5.3.18",
                          "minPlatformVersion": "1.0.0",
                          "maxPlatformVersion": "1.0.0"
                        },
                        {
                          "version": "5.3.17",
                          "downloadUrl": "https://example.com/plugin-kafka-5.3.17.jar",
                          "sha256": "sha-517",
                          "minPlatformVersion": "1.0.0",
                          "maxPlatformVersion": "1.0.0"
                        }
                      ]
                    }
                  ]
                }
                """;

        List<PluginCatalogEntry> entries = PluginCatalogService.parseCatalogForTests(
                json,
                "https://example.com/catalog-github.json",
                "5.3.18",
                "1.0.0"
        );

        assertEquals(entries.size(), 1);
        assertEquals(entries.get(0).id(), "plugin-kafka");
        assertEquals(entries.get(0).version(), "5.3.18");
        assertEquals(entries.get(0).installUrl(), "https://example.com/plugin-kafka-5.3.18.jar");
    }
}
