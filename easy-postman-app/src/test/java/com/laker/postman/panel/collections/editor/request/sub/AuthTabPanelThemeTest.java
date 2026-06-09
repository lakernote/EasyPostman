package com.laker.postman.panel.collections.editor.request.sub;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AuthTabPanelThemeTest {
    @Test
    public void infoBlocksShouldUseThemeAwareSwingComponents() throws IOException {
        String source = Files.readString(findProjectRoot().resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/AuthTabPanel.java"
        ));

        assertTrue(source.contains("ToolWindowSurfaceStyle.applySectionHeader(infoPanel"));
        assertTrue(source.contains("FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1)"));
        assertTrue(source.contains("FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2)"));
        assertTrue(source.contains("ModernColors.getPrimary()"));
        assertTrue(source.contains("ModernColors.getTextSecondary()"));
        assertFalse(source.contains("AuthTabTheme"));
        assertFalse(source.contains("font-size"));
    }

    private static Path findProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("easy-postman-app"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find easy-postman project root");
    }
}
