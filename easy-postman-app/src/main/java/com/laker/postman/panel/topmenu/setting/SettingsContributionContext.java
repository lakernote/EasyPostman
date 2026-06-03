package com.laker.postman.panel.topmenu.setting;

import java.awt.Window;

/**
 * Context available when a settings contribution creates its panel.
 */
public record SettingsContributionContext(Window parentWindow) {
}
