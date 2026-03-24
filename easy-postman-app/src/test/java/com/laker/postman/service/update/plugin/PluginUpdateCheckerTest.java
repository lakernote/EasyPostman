package com.laker.postman.service.update.plugin;

import com.laker.postman.plugin.api.PluginDescriptor;
import com.laker.postman.plugin.manager.market.PluginCatalogEntry;
import com.laker.postman.plugin.runtime.PluginFileInfo;
import com.laker.postman.plugin.runtime.PluginRuntime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PluginUpdateCheckerTest {

    private Path dataDir;
    private Path appDir;

    @BeforeMethod
    public void setUp() throws IOException {
        dataDir = Files.createTempDirectory("plugin-update-checker-test");
        appDir = Files.createTempDirectory("plugin-update-checker-app");
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
    public void shouldFindCompatiblePluginUpdates() {
        List<PluginUpdateCandidate> candidates = PluginUpdateChecker.findUpdateCandidates(
                List.of(installedPlugin("plugin-kafka", "Kafka Plugin", "5.3.18")),
                List.of(
                        catalogEntry("plugin-kafka", "Kafka Plugin", "5.3.23", "", ""),
                        catalogEntry("plugin-redis", "Redis Plugin", "5.3.18", "", "")
                )
        );

        assertEquals(candidates.size(), 1);
        assertEquals(candidates.get(0).pluginId(), "plugin-kafka");
        assertEquals(candidates.get(0).installedVersion(), "5.3.18");
        assertEquals(candidates.get(0).latestVersion(), "5.3.23");
    }

    @Test
    public void shouldIgnoreIncompatibleAndNonNewerCatalogEntries() {
        List<PluginUpdateCandidate> candidates = PluginUpdateChecker.findUpdateCandidates(
                List.of(installedPlugin("plugin-kafka", "Kafka Plugin", "5.3.18")),
                List.of(
                        catalogEntry("plugin-kafka", "Kafka Plugin", "99.0.0", "99.0.0", ""),
                        catalogEntry("plugin-kafka", "Kafka Plugin", "5.3.18", "", ""),
                        catalogEntry("plugin-kafka", "Kafka Plugin", "5.3.17", "", "")
                )
        );

        assertTrue(candidates.isEmpty());
    }

    @Test
    public void shouldUseHighestInstalledVersionPerPluginId() {
        List<PluginUpdateCandidate> candidates = PluginUpdateChecker.findUpdateCandidates(
                List.of(
                        installedPlugin("plugin-kafka", "Kafka Plugin", "5.3.18"),
                        installedPlugin("plugin-kafka", "Kafka Plugin", "5.3.20")
                ),
                List.of(catalogEntry("plugin-kafka", "Kafka Plugin", "5.3.21", "", ""))
        );

        assertEquals(candidates.size(), 1);
        assertEquals(candidates.get(0).installedVersion(), "5.3.20");
        assertEquals(candidates.get(0).latestVersion(), "5.3.21");
    }

    private static PluginFileInfo installedPlugin(String id, String name, String version) {
        return new PluginFileInfo(
                new PluginDescriptor(id, name, version, "com.example." + name.replace(" ", "")),
                Path.of("/tmp", id + "-" + version + ".jar"),
                true,
                true,
                true
        );
    }

    private static PluginCatalogEntry catalogEntry(String id,
                                                   String name,
                                                   String version,
                                                   String minAppVersion,
                                                   String maxAppVersion) {
        return new PluginCatalogEntry(
                id,
                name,
                version,
                "",
                "https://example.com/" + id + "-" + version + ".jar",
                "https://example.com/" + id,
                "sha256-" + version,
                "https://example.com/" + id + "-" + version + ".jar",
                minAppVersion,
                maxAppVersion,
                "1.0.0",
                "1.0.0"
        );
    }
}
