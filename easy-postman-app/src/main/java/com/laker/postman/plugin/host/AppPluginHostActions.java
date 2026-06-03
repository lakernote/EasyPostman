package com.laker.postman.plugin.host;

import com.laker.postman.panel.topmenu.plugin.PluginManagerDialog;
import com.laker.postman.plugin.api.PluginHostActions;
import lombok.RequiredArgsConstructor;

import java.awt.Window;

/**
 * App implementation of host actions exposed to plugin UI contributions.
 */
@RequiredArgsConstructor
public final class AppPluginHostActions implements PluginHostActions {

    private final Window parentWindow;

    @Override
    public void openPluginCenter(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            PluginManagerDialog.showDialog(parentWindow);
            return;
        }
        PluginManagerDialog.showMarketDialog(parentWindow, pluginId.trim());
    }
}
