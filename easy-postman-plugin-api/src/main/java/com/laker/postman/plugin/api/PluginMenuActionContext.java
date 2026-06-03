package com.laker.postman.plugin.api;

import java.awt.Window;

/**
 * Context available when a plugin menu action runs.
 */
public record PluginMenuActionContext(Window parentWindow) {
}
