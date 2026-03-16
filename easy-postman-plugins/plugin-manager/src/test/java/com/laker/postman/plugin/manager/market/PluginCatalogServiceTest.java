package com.laker.postman.plugin.manager.market;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class PluginCatalogServiceTest {

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

        assertEquals(githubEntries.size(), 5);
        assertEquals(giteeEntries.size(), 5);
        assertEquals(githubEntries.get(0).id(), "plugin-redis");
        assertTrue(githubEntries.get(0).installUrl().startsWith("https://github.com/lakernote/easy-postman/"));
        assertTrue(giteeEntries.get(0).installUrl().startsWith("https://gitee.com/lakernote/easy-postman/"));
    }
}
