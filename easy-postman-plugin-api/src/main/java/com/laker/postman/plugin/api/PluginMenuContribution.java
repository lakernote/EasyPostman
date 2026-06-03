package com.laker.postman.plugin.api;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Menu action contributed by a plugin.
 */
public record PluginMenuContribution(
        String id,
        String parentMenuId,
        String titleKey,
        int order,
        Consumer<PluginMenuActionContext> action,
        String titleBundleName,
        ClassLoader titleClassLoader
) {

    public static final String PARENT_MENU_PLUGINS = "plugins";

    public PluginMenuContribution(String id,
                                  String titleKey,
                                  int order,
                                  Consumer<PluginMenuActionContext> action) {
        this(id, PARENT_MENU_PLUGINS, titleKey, order, action, null, null);
    }

    public PluginMenuContribution(String id,
                                  String parentMenuId,
                                  String titleKey,
                                  int order,
                                  Consumer<PluginMenuActionContext> action,
                                  String titleBundleName) {
        this(id, parentMenuId, titleKey, order, action, titleBundleName, null);
    }

    public PluginMenuContribution {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Plugin menu contribution id must not be blank");
        }
        parentMenuId = parentMenuId == null || parentMenuId.isBlank()
                ? PARENT_MENU_PLUGINS
                : parentMenuId.trim();
        if (titleKey == null || titleKey.isBlank()) {
            throw new IllegalArgumentException("Plugin menu contribution titleKey must not be blank");
        }
        action = Objects.requireNonNull(action, "action");
    }

    public void perform(PluginMenuActionContext context) {
        action.accept(context == null ? new PluginMenuActionContext(null) : context);
    }

    public PluginMenuContribution withTitleClassLoader(ClassLoader fallbackClassLoader) {
        if (titleClassLoader != null || fallbackClassLoader == null) {
            return this;
        }
        return new PluginMenuContribution(
                id,
                parentMenuId,
                titleKey,
                order,
                action,
                titleBundleName,
                fallbackClassLoader
        );
    }
}
