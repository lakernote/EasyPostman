package com.laker.postman.plugin.api;

/**
 * Status bar shortcut action contributed by a plugin.
 * <p>
 * The plugin declares intent and resources only. The host owns how the action is rendered
 * and how a target such as a toolbox page is opened.
 * </p>
 */
public record StatusBarActionContribution(
        String id,
        String tooltip,
        String iconPath,
        String targetType,
        String targetId,
        int order,
        ClassLoader iconClassLoader
) {
    public static final String TARGET_TOOLBOX = "toolbox";

    public StatusBarActionContribution(
            String id,
            String tooltip,
            String iconPath,
            String targetType,
            String targetId,
            int order
    ) {
        this(id, tooltip, iconPath, targetType, targetId, order, null);
    }

    public StatusBarActionContribution {
        id = normalize(id);
        tooltip = normalize(tooltip);
        iconPath = normalize(iconPath);
        targetType = normalize(targetType);
        targetId = normalize(targetId);
    }

    public StatusBarActionContribution withIconClassLoader(ClassLoader fallbackClassLoader) {
        return new StatusBarActionContribution(
                id,
                tooltip,
                iconPath,
                targetType,
                targetId,
                order,
                iconClassLoader == null ? fallbackClassLoader : iconClassLoader
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
