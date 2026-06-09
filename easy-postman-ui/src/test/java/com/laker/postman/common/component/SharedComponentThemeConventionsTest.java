package com.laker.postman.common.component;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class SharedComponentThemeConventionsTest {
    private static final Path ROOT = findProjectRoot();
    private static final List<String> THEME_SENSITIVE_COMPONENTS = List.of(
            "easy-postman-ui/src/main/java/com/laker/postman/common/component/ChipLabel.java",
            "easy-postman-ui/src/main/java/com/laker/postman/common/component/PlaceholderTextArea.java",
            "easy-postman-ui/src/main/java/com/laker/postman/common/component/SearchReplacePanel.java",
            "easy-postman-ui/src/main/java/com/laker/postman/common/component/ToolWindowActionToolbar.java",
            "easy-postman-ui/src/main/java/com/laker/postman/common/component/ToolWindowSidebarHeader.java",
            "easy-postman-ui/src/main/java/com/laker/postman/common/component/button/SwitchButton.java",
            "easy-postman-ui/src/main/java/com/laker/postman/common/component/connection/ConnectionToolbarUi.java",
            "easy-postman-ui/src/main/java/com/laker/postman/common/component/table/EnhancedTablePanel.java"
    );

    @Test
    public void sharedComponentsShouldUseSemanticFallbackColors() {
        List<String> violations = THEME_SENSITIVE_COMPONENTS.stream()
                .filter(SharedComponentThemeConventionsTest::containsLegacyThemeFallback)
                .toList();

        assertTrue(violations.isEmpty(),
                "Use ModernColors semantic fallbacks in shared UI components: " + violations);
    }

    private static boolean containsLegacyThemeFallback(String relativePath) {
        String source = read(relativePath);
        return source.contains("UIManager.getColor(\"Panel.background\")")
                || source.contains("UIManager.getColor(\"Separator.foreground\")")
                || source.contains("UIManager.getColor(\"Label.disabledForeground\")")
                || source.contains("UIManager.getColor(\"Table.background\")")
                || source.contains("UIManager.getColor(SEPARATOR_FG)")
                || source.contains("UIManager.getColor(LABEL_DISABLED)")
                || source.contains("new Color(128, 128, 128)")
                || source.contains("new Color(196, 201, 208)")
                || source.contains("Color.WHITE")
                || source.contains("style='color:gray'")
                || source.contains("Table.selectionBackground\").brighter()");
    }

    private static String read(String relativePath) {
        try {
            return Files.readString(ROOT.resolve(relativePath));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + relativePath, e);
        }
    }

    private static Path findProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("easy-postman-ui"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find easy-postman project root");
    }
}
