package com.laker.postman.plugin.api;

import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class PluginSettingsContributionContextTest {

    @Test
    public void shouldKeepLegacyParentOnlyConstructorWithNoopHostActions() {
        PluginSettingsContributionContext context = new PluginSettingsContributionContext(null);

        assertNotNull(context.hostActions());
        context.hostActions().openPluginCenter();
        context.hostActions().openPluginCenter("plugin-client-cert");
    }

    @Test
    public void shouldExposeHostActionsToPluginSettingsPanels() {
        AtomicReference<String> openedPluginId = new AtomicReference<>();
        PluginHostActions hostActions = new PluginHostActions() {
            @Override
            public void openPluginCenter(String pluginId) {
                openedPluginId.set(pluginId);
            }
        };

        PluginSettingsContributionContext context = new PluginSettingsContributionContext(null, hostActions);
        context.hostActions().openPluginCenter("plugin-redis");

        assertEquals(openedPluginId.get(), "plugin-redis");
    }
}
