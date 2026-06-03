package com.laker.postman.plugin.api;

import javax.swing.JComponent;
import java.util.Objects;
import java.util.function.Function;

/**
 * Settings page contributed by a plugin.
 */
public record PluginSettingsContribution(
        String id,
        String titleKey,
        int order,
        String category,
        Function<PluginSettingsContributionContext, ? extends JComponent> panelFactory,
        String titleBundleName,
        ClassLoader titleClassLoader
) {

    public static final String CATEGORY_APPLICATION = "application";
    public static final String CATEGORY_NETWORK = "network";
    public static final String CATEGORY_RUNTIME = "runtime";
    public static final String CATEGORY_EXTENSIONS = "extensions";

    public PluginSettingsContribution(String id,
                                      String titleKey,
                                      int order,
                                      String category,
                                      Function<PluginSettingsContributionContext, ? extends JComponent> panelFactory) {
        this(id, titleKey, order, category, panelFactory, null, null);
    }

    public PluginSettingsContribution(String id,
                                      String titleKey,
                                      int order,
                                      String category,
                                      Function<PluginSettingsContributionContext, ? extends JComponent> panelFactory,
                                      String titleBundleName) {
        this(id, titleKey, order, category, panelFactory, titleBundleName, null);
    }

    public PluginSettingsContribution {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Plugin settings contribution id must not be blank");
        }
        if (titleKey == null || titleKey.isBlank()) {
            throw new IllegalArgumentException("Plugin settings contribution titleKey must not be blank");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Plugin settings contribution category must not be blank");
        }
        panelFactory = Objects.requireNonNull(panelFactory, "panelFactory");
    }

    public JComponent createPanel(PluginSettingsContributionContext context) {
        return Objects.requireNonNull(
                panelFactory.apply(context == null ? new PluginSettingsContributionContext(null) : context),
                "plugin settings contribution panel"
        );
    }

    public PluginSettingsContribution withTitleClassLoader(ClassLoader fallbackClassLoader) {
        if (titleClassLoader != null || fallbackClassLoader == null) {
            return this;
        }
        return new PluginSettingsContribution(
                id,
                titleKey,
                order,
                category,
                panelFactory,
                titleBundleName,
                fallbackClassLoader
        );
    }
}
