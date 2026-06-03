package com.laker.postman.plugin.api;

import java.awt.Window;

/**
 * Context available when a plugin creates its settings panel.
 */
public record PluginSettingsContributionContext(
        Window parentWindow,
        PluginHostActions hostActions
) {

    public PluginSettingsContributionContext(Window parentWindow) {
        this(parentWindow, PluginHostActions.NOOP);
    }

    public PluginSettingsContributionContext {
        if (hostActions == null) {
            hostActions = PluginHostActions.NOOP;
        }
    }
}
