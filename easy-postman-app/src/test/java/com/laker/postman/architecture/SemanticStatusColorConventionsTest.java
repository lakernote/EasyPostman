package com.laker.postman.architecture;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class SemanticStatusColorConventionsTest {
    private static final Path ROOT = findProjectRoot();
    private static final List<String> STATUS_COLOR_COMPONENTS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/history/HistoryPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/result/PerformanceReportPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/DiffPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ElasticsearchPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/InfluxDbPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/JsonToolPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/SqlToolPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components/WorkspaceDetailPanel.java"
    );
    private static final List<String> PLUGIN_CHIP_COMPONENTS = List.of(
            "easy-postman-plugins/plugin-capture/src/main/java/com/laker/postman/plugin/capture/CapturePanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/consumer/ui/KafkaConsumerPanel.java"
    );
    private static final List<String> HTML_COLOR_COMPONENTS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/common/component/EasyTextField.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/VariableTooltipHtmlBuilder.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/RequestMethodUiMetadata.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/dialog/WorkspaceSelectionDialog.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/tree/RequestTreeCellRenderer.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/history/HistoryPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/GroupEditPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/RequestBodyVariableTooltipBuilder.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/EasyRequestHttpHeadersPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/ResponseTabBadgeController.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ElasticsearchPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/InfluxDbPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/topmenu/TopMenuAboutDialog.java",
            "easy-postman-app/src/main/java/com/laker/postman/service/render/HttpHtmlRenderer.java",
            "easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPanel.java"
    );

    @Test
    public void statusColorsShouldUseModernColorsSemantics() {
        List<String> violations = STATUS_COLOR_COMPONENTS.stream()
                .filter(SemanticStatusColorConventionsTest::containsLegacyStatusColor)
                .toList();

        assertTrue(violations.isEmpty(), "Use ModernColors for status/result colors: " + violations);
    }

    @Test
    public void pluginChipsShouldUseSharedThemeAwareRendering() {
        List<String> violations = PLUGIN_CHIP_COMPONENTS.stream()
                .filter(SemanticStatusColorConventionsTest::containsLegacyPluginChipColor)
                .toList();

        assertTrue(violations.isEmpty(), "Use ChipLabel and ModernColors for plugin chips: " + violations);
    }

    @Test
    public void htmlSnippetsShouldUseThemeAwareModernColors() {
        List<String> violations = HTML_COLOR_COMPONENTS.stream()
                .filter(SemanticStatusColorConventionsTest::containsLegacyHtmlColor)
                .toList();

        assertTrue(violations.isEmpty(), "Use ModernColors.toHtmlColor(...) for HTML snippets: " + violations);
    }

    private static boolean containsLegacyStatusColor(String relativePath) {
        String source = read(relativePath);
        return source.contains("new Color(0, 128, 0)")
                || source.contains("new Color(180, 0, 0)")
                || source.contains("new Color(46, 125, 50)")
                || source.contains("new Color(198, 40, 40)")
                || source.contains("new Color(0, 160, 0)")
                || source.contains("new Color(200, 50, 50)")
                || source.contains("new Color(210, 130, 0)")
                || source.contains("new Color(40, 167, 69)")
                || source.contains("new Color(220, 140, 20)")
                || source.contains("new Color(80, 160, 230)")
                || source.contains("new Color(130, 100, 200)")
                || source.contains("Color.BLUE")
                || source.contains("UIManager.getColor(\"Actions.Red\")")
                || source.contains("UIManager.getColor(\"Table.foreground\")");
    }

    private static boolean containsLegacyPluginChipColor(String relativePath) {
        String source = read(relativePath);
        return source.contains("UIManager.getColor(\"Label.foreground\")")
                || source.contains("Math.min(bgColor.getRed() + 80")
                || source.contains("new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue()")
                || source.contains("new Color(120, 80, 200)")
                || source.contains("new Color(20, 150, 100)")
                || source.contains("new Color(180, 100, 0)")
                || source.contains("new Color(160, 60, 60)")
                || source.contains("new Color(120, 120, 120)")
                || source.contains("ModernColors.PRIMARY")
                || source.contains("ModernColors.INFO")
                || source.contains("ModernColors.SUCCESS")
                || source.contains("ModernColors.NEUTRAL");
    }

    private static boolean containsLegacyHtmlColor(String relativePath) {
        String source = read(relativePath);
        return source.contains("#64748b")
                || source.contains("#009900")
                || source.contains("#d32f2f")
                || source.contains("#D32F2F")
                || source.contains("style='color:gray'")
                || source.contains("style='color: gray'")
                || source.contains("color='gray'")
                || source.contains("color:#999999")
                || source.contains("#cce0ff")
                || source.contains("#888888")
                || source.contains("#d9822b")
                || source.contains("#e8a000")
                || source.contains("#5cacee")
                || source.contains("#e04040")
                || source.contains("#28a745")
                || source.contains("#2a7ec8")
                || source.contains("#9e9e9e")
                || source.contains("#009688")
                || source.contains("#e67c00")
                || source.contains("#8e24aa")
                || source.contains("#1565c0")
                || source.contains("#2e7d32")
                || source.contains("#00838f")
                || source.contains("#60a5fa")
                || source.contains("#1a0dab")
                || source.contains("#E0E0E0")
                || source.contains("#424242")
                || source.contains("#757575")
                || source.contains("#F5F5F5");
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
            if (Files.isDirectory(current.resolve("easy-postman-app"))
                    && Files.isDirectory(current.resolve("easy-postman-ui"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find easy-postman project root");
    }
}
