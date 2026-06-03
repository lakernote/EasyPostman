package com.laker.postman.plugin.api;

import java.awt.Window;

/**
 * Context available when a plugin creates its settings panel.
 */
public record PluginSettingsContributionContext(Window parentWindow) {
}
