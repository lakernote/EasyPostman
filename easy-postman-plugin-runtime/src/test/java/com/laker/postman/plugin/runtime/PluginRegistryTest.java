package com.laker.postman.plugin.runtime;

import org.testng.annotations.Test;

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
}
