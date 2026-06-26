package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.PluginMenuContribution;
import com.laker.postman.plugin.api.PluginSettingsContribution;
import com.laker.postman.plugin.api.PluginUpdateMetadata;
import com.laker.postman.plugin.api.PluginUpdateMetadataContribution;
import com.laker.postman.plugin.api.StatusBarActionContribution;
import org.testng.annotations.Test;

import javax.swing.JPanel;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class PluginRegistryTest {

    @Test
    public void shouldUseLatestRegisteredScriptApiFactoryForSameAlias() {
        PluginRegistry registry = new PluginRegistry();

        registry.registerScriptApi("plugin-a", "dup", () -> "first");
        registry.registerScriptApi("plugin-b", "dup", () -> "second");

        Map<String, Object> apis = registry.createScriptApis();

        assertEquals(apis.size(), 1);
        assertEquals(apis.get("dup"), "second");
        assertEquals(registry.getScriptApiOwner("dup"), "plugin-b");
    }

    @Test
    public void shouldUseLatestRegisteredServiceForSameType() {
        PluginRegistry registry = new PluginRegistry();
        Runnable first = () -> {
        };
        Runnable second = () -> {
        };

        registry.registerService("plugin-a", Runnable.class, first);
        registry.registerService("plugin-b", Runnable.class, second);

        assertSame(registry.getService(Runnable.class), second);
        assertEquals(registry.getServiceOwner(Runnable.class), "plugin-b");
    }

    @Test
    public void shouldRegisterNeutralScriptCompletionContributor() {
        PluginRegistry registry = new PluginRegistry();

        registry.registerScriptCompletionContributor(sink -> sink.basic("pm.redis", "Redis plugin API"));

        assertEquals(registry.getScriptCompletionContributors().size(), 1);
        registry.getScriptCompletionContributors().get(0).contribute(item ->
                assertTrue("pm.redis".equals(item.inputText())));
    }

    @Test
    public void shouldRegisterSettingsContributions() {
        PluginRegistry registry = new PluginRegistry();
        PluginSettingsContribution contribution = new PluginSettingsContribution(
                "plugin-settings",
                "plugin.settings.title",
                900,
                PluginSettingsContribution.CATEGORY_EXTENSIONS,
                context -> new JPanel()
        );

        registry.registerSettingsContribution("plugin-a", contribution);

        assertEquals(registry.getSettingsContributions().size(), 1);
        assertSame(registry.getSettingsContributions().get(0), contribution);
    }

    @Test
    public void shouldRegisterMenuContributions() {
        PluginRegistry registry = new PluginRegistry();
        AtomicBoolean invoked = new AtomicBoolean(false);
        PluginMenuContribution contribution = new PluginMenuContribution(
                "plugin-action",
                "plugin.action.title",
                900,
                context -> invoked.set(true)
        );

        registry.registerMenuContribution("plugin-a", contribution);
        registry.getMenuContributions().get(0).perform(null);

        assertEquals(registry.getMenuContributions().size(), 1);
        assertSame(registry.getMenuContributions().get(0), contribution);
        assertTrue(invoked.get());
    }

    @Test
    public void shouldRegisterStatusBarActionContributions() {
        PluginRegistry registry = new PluginRegistry();
        StatusBarActionContribution contribution = new StatusBarActionContribution(
                "capture-shortcut",
                "Capture requests",
                "icons/capture.svg",
                StatusBarActionContribution.TARGET_TOOLBOX,
                "capture",
                200
        );

        registry.registerStatusBarActionContribution("plugin-capture", contribution);

        assertEquals(registry.getStatusBarActionContributions().size(), 1);
        assertSame(registry.getStatusBarActionContributions().get(0), contribution);
    }

    @Test
    public void shouldRegisterUpdateMetadataContributions() {
        PluginRegistry registry = new PluginRegistry();
        PluginUpdateMetadataContribution contribution = new PluginUpdateMetadataContribution(
                "plugin-update-metadata",
                900,
                () -> java.util.List.of(new PluginUpdateMetadata(
                        "plugin-kafka",
                        "Kafka Plugin",
                        "5.3.24",
                        "",
                        "https://example.com/plugin-kafka-5.3.24.jar",
                        "https://example.com/plugin-kafka",
                        "sha256-5.3.24"
                ))
        );

        registry.registerUpdateMetadataContribution("plugin-a", contribution);

        assertEquals(registry.getUpdateMetadataContributions().size(), 1);
        assertSame(registry.getUpdateMetadataContributions().get(0), contribution);
        assertEquals(registry.getUpdateMetadataContributions().get(0).loadMetadata().get(0).pluginId(), "plugin-kafka");
    }
}
