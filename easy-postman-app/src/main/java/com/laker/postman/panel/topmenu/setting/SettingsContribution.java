package com.laker.postman.panel.topmenu.setting;

import javax.swing.JComponent;
import java.util.Objects;
import java.util.function.Function;

/**
 * One tab contribution in the host settings dialog.
 */
public record SettingsContribution(
        String id,
        String titleKey,
        int order,
        String category,
        Function<SettingsContributionContext, ? extends JComponent> panelFactory
) {

    public SettingsContribution {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Settings contribution id must not be blank");
        }
        if (titleKey == null || titleKey.isBlank()) {
            throw new IllegalArgumentException("Settings contribution titleKey must not be blank");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Settings contribution category must not be blank");
        }
        panelFactory = Objects.requireNonNull(panelFactory, "panelFactory");
    }

    public JComponent createPanel(SettingsContributionContext context) {
        return Objects.requireNonNull(
                panelFactory.apply(context == null ? new SettingsContributionContext(null) : context),
                "settings contribution panel"
        );
    }
}
