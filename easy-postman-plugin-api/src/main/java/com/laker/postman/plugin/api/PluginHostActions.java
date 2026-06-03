package com.laker.postman.plugin.api;

/**
 * Host actions exposed to plugin UI contributions without depending on app internals.
 */
public interface PluginHostActions {

    PluginHostActions NOOP = new PluginHostActions() {
    };

    default void openPluginCenter() {
        openPluginCenter(null);
    }

    default void openPluginCenter(String pluginId) {
        // no-op by default
    }
}
