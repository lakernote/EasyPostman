package com.laker.postman.plugin.runtime;

import com.laker.postman.plugin.api.PluginSettingsContribution;
import org.testng.annotations.Test;

import javax.swing.JPanel;
import java.util.Map;

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
}
