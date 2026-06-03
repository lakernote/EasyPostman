package com.laker.postman.common.themes;

import java.util.function.BooleanSupplier;

/**
 * Describes one installable application theme and the editor theme paired with it.
 */
public record ThemeDescriptor(
        String id,
        String nameKey,
        boolean dark,
        BooleanSupplier lookAndFeelSetup,
        String editorThemeResourcePath,
        String fallbackEditorThemeResourcePath,
        String flatLafDefaultsSource
) {

    public ThemeDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Theme id must not be blank");
        }
        if (nameKey == null || nameKey.isBlank()) {
            throw new IllegalArgumentException("Theme name key must not be blank");
        }
        if (lookAndFeelSetup == null) {
            throw new IllegalArgumentException("Theme Look and Feel setup action must not be null");
        }
        if (editorThemeResourcePath == null || editorThemeResourcePath.isBlank()) {
            throw new IllegalArgumentException("Editor theme resource path must not be blank");
        }
        if (fallbackEditorThemeResourcePath == null || fallbackEditorThemeResourcePath.isBlank()) {
            throw new IllegalArgumentException("Fallback editor theme resource path must not be blank");
        }
    }

    public boolean applyLookAndFeel() {
        return lookAndFeelSetup.getAsBoolean();
    }
}
