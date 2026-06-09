package com.laker.postman.architecture;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.testng.Assert.assertTrue;

public class ToolWindowStyleConventionsTest {
    private static final Path ROOT = findProjectRoot();
    private static final List<String> ALLOWED_NATIVE_SPLIT_PANES = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/RequestViewFactory.java"
    );
    private static final List<String> ALLOWED_RAW_TOOL_WINDOW_SPLITS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/common/component/dialog/SnippetDialog.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/SidebarConsoleArea.java"
    );
    private static final List<String> THEME_CONSTANTS = List.of(
            "ModernColors.PRIMARY",
            "ModernColors.PRIMARY_DARK",
            "ModernColors.SECONDARY",
            "ModernColors.ACCENT",
            "ModernColors.SUCCESS",
            "ModernColors.ERROR",
            "ModernColors.WARNING",
            "ModernColors.INFO",
            "ModernColors.NEUTRAL"
    );
    private static final List<String> NOTIFICATION_OVERLAYS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/update/AutoUpdateNotification.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/update/PluginUpdateNotification.java"
    );
    private static final List<String> SIDEBAR_SEARCH_TOOLBARS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/tree/CollectionTreeToolbar.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/env/EnvironmentPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ToolboxPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ElasticsearchPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/InfluxDbPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/ui/KafkaTopicPanel.java",
            "easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPanel.java"
    );
    private static final List<String> SIDEBAR_SECTION_HEADERS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ElasticsearchPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/InfluxDbPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/ui/KafkaTopicPanel.java",
            "easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPanel.java"
    );
    private static final List<String> TOOL_WINDOW_ACTION_TOOLBARS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/env/EnvironmentPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/functional/ExecutionResultsPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/functional/FunctionalPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/history/HistoryPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformancePanelViewFactory.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/config/CsvDataSetPropertyPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/result/PerformanceReportPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/result/PerformanceTrendPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/ConsolePanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/global/GlobalVariablesPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/CronPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/CryptoPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/DiffPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/EncoderPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/HashPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/JsonToolPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/SqlToolPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/TimestampPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/UuidPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java",
            "easy-postman-plugins/plugin-client-cert/src/main/java/com/laker/postman/plugin/clientcert/ClientCertificateSettingsPanel.java",
            "easy-postman-plugins/plugin-decompiler/src/main/java/com/laker/postman/plugin/decompiler/DecompilerPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/consumer/ui/KafkaConsumerPanel.java"
    );
    private static final List<String> COMPACT_TOOL_WINDOW_FONT_PANELS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/common/component/dialog/WorkspaceSelectionDialog.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/AuthTabPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/CronPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/DiffPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ElasticsearchPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/InfluxDbPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/JsonToolPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/SqlToolPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ToolboxPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/update/AutoUpdateNotification.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/update/PluginUpdateNotification.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformancePanelViewFactory.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components/ConvertToGitDialog.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components/GitOperationDialog.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/consumer/ui/KafkaConsumerPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/producer/ui/KafkaProducerPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/shared/ui/KafkaPropertiesEditorPanel.java",
            "easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPanel.java"
    );
    private static final Pattern ABSOLUTE_UI_FONT_PATTERN = Pattern.compile(
            "deriveFont\\([^\\n;]*\\d+(?:\\.\\d+)?f\\)|new Font\\("
    );
    private static final List<String> PRIMARY_TOOL_WINDOW_SURFACES = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/MainPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/SidebarTabPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/RequestCollectionsPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/env/EnvironmentPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/functional/FunctionalPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/history/HistoryPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformancePanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ToolboxPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/global/GlobalVariablesPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/ConsolePanel.java",
            "easy-postman-plugins/plugin-capture/src/main/java/com/laker/postman/plugin/capture/CapturePanel.java",
            "easy-postman-plugins/plugin-client-cert/src/main/java/com/laker/postman/plugin/clientcert/ClientCertificateSettingsPanel.java",
            "easy-postman-plugins/plugin-decompiler/src/main/java/com/laker/postman/plugin/decompiler/DecompilerPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/KafkaPanel.java",
            "easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPanel.java"
    );
    private static final List<String> LARGE_TOOL_WINDOW_LAYOUTS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/RequestCollectionsPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/env/EnvironmentPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/functional/FunctionalPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/history/HistoryPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformancePanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ToolboxPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java",
            "easy-postman-plugins/plugin-capture/src/main/java/com/laker/postman/plugin/capture/CapturePanel.java",
            "easy-postman-plugins/plugin-decompiler/src/main/java/com/laker/postman/plugin/decompiler/DecompilerPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/KafkaPanel.java",
            "easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPanel.java"
    );
    private static final List<String> PRIMARY_HORIZONTAL_SPLITS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/RequestCollectionsPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/env/EnvironmentPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/functional/ExecutionResultsPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/history/HistoryPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformancePanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ToolboxPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components/GitOperationDialog.java"
    );
    private static final List<String> STACKED_TOOL_WINDOW_SPLITS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java"
    );
    private static final List<String> TOOL_WINDOW_DIALOG_SHELLS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/cookie/CookieManagerDialog.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/global/GlobalVariablesDialog.java"
    );
    private static final List<String> SETTINGS_TABLE_SURFACES = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/topmenu/setting/ClientCertificateSettingsPanelModern.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/topmenu/setting/TrustedCertificatesSettingsPanelModern.java"
    );
    private static final List<String> LIST_SCROLL_SURFACES = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/env/EnvironmentPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/history/HistoryPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ElasticsearchPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/InfluxDbPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/ui/KafkaTopicPanel.java",
            "easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPanel.java"
    );
    private static final List<String> TREE_SCROLL_SURFACES = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/tree/CollectionTreePanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/functional/ExecutionResultsPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformancePanelViewFactory.java",
            "easy-postman-plugins/plugin-decompiler/src/main/java/com/laker/postman/plugin/decompiler/DecompilerPanel.java"
    );
    private static final List<String> BORDERLESS_SECTION_SURFACES = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/common/component/MarkdownEditorPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/placeholder/RequestEditorPlaceholderPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/placeholder/StartupShellPlaceholderPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/AuthTabPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/RequestSettingsPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/ResponsePanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/ResponseSizeTooltipWindow.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/ResponseTooltipBuilder.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/tree/CollectionGroupSelectionDialog.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/history/HistoryPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/global/GlobalVariablesPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/ElasticsearchPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/toolbox/InfluxDbPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/topmenu/plugin/PluginManagerDialog.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/topmenu/setting/ModernSettingsPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components/ConvertToGitDialog.java",
            "easy-postman-plugins/plugin-capture/src/main/java/com/laker/postman/plugin/capture/CapturePanel.java",
            "easy-postman-plugins/plugin-decompiler/src/main/java/com/laker/postman/plugin/decompiler/DecompilerPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/connection/ui/KafkaConnectionPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/consumer/ui/KafkaConsumerPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/producer/ui/KafkaProducerPanel.java",
            "easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/shared/ui/KafkaPropertiesEditorPanel.java",
            "easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPanel.java"
    );
    private static final List<String> PLACEHOLDER_SHELLS_WITHOUT_DRAWN_DIVIDERS = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/common/component/placeholder/RequestEditorPlaceholderPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/placeholder/StartupShellPlaceholderPanel.java"
    );
    private static final List<String> ALLOWED_FUNCTIONAL_BORDER_FILES = List.of(
            "easy-postman-app/src/main/java/com/laker/postman/common/component/CsvDataPanel.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/EasyTextField.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/StepIndicator.java",
            "easy-postman-app/src/main/java/com/laker/postman/common/component/table/TableUIConstants.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/sub/RequestBodyVariableAutocompleteController.java",
            "easy-postman-app/src/main/java/com/laker/postman/panel/performance/result/PerformanceResultTablePanel.java"
    );

    @Test
    public void swingPanelsShouldNotIntroduceTitledBorders() throws IOException {
        List<String> violations = javaSources().filter(path -> {
                    String source = readUnchecked(path);
                    return source.contains("TitledBorder") || source.contains("createTitledBorder");
                })
                .map(ROOT::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), "Use explicit label/header sections instead of TitledBorder: " + violations);
    }

    @Test
    public void toolWindowsShouldUseSharedSplitPaneChrome() throws IOException {
        List<String> violations = javaSources()
                .filter(path -> readUnchecked(path).contains("new JSplitPane"))
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .filter(path -> !ALLOWED_NATIVE_SPLIT_PANES.contains(path))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowChrome.create*SplitPane for tool-window layouts: " + violations);
    }

    @Test
    public void largeToolWindowsShouldUseCardSplitPaneConvenienceApi() throws IOException {
        List<String> violations = javaSources().filter(path -> {
                    String source = readUnchecked(path);
                    return source.contains("ToolWindowChrome.createHorizontalSplitPane(")
                            || source.contains("ToolWindowChrome.createVerticalSplitPane(");
                })
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .filter(path -> !ALLOWED_RAW_TOOL_WINDOW_SPLITS.contains(path))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use ToolWindowChrome.create*CardSplitPane for rounded tool-window layouts: " + violations);
    }

    @Test
    public void popupMenusShouldUseSharedToolWindowPopupStyle() throws IOException {
        List<String> violations = javaSources()
                .filter(path -> {
                    String[] lines = readUnchecked(path).split("\\R");
                    for (int i = 0; i < lines.length; i++) {
                        if (!lines[i].contains("new JPopupMenu(")) {
                            continue;
                        }
                        String nextLines = String.join("\n",
                                lines[Math.min(i + 1, lines.length - 1)],
                                lines[Math.min(i + 2, lines.length - 1)]);
                        if (!nextLines.contains("ToolWindowSurfaceStyle.applyPopupMenuCard")) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSurfaceStyle.applyPopupMenuCard for popup menus: " + violations);
    }

    @Test
    public void toolWindowsShouldUseSemanticThemeColorsForDividersAndSecondaryText() throws IOException {
        List<String> violations = javaSources().filter(path -> {
                    String source = readUnchecked(path);
                    return source.contains("UIManager.getColor(\"Separator.foreground\")")
                            || source.contains("UIManager.getColor(\"Label.disabledForeground\")")
                            || source.contains("UIManager.getColor(\"List.hoverBackground\")")
                            || source.contains("UIManager.getColor(SEPARATOR_FG)")
                            || source.contains("Color.GRAY")
                            || source.contains("Color.LIGHT_GRAY")
                            || source.contains("new Color(128, 128, 128)")
                            || source.contains("new Color(150, 150, 150)")
                            || source.contains("new Color(220, 53, 69)");
                })
                .map(ROOT::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), "Use ModernColors semantic colors for dividers and secondary text: " + violations);
    }

    @Test
    public void customPaintedNotificationsShouldUseSemanticNotificationColors() {
        List<String> violations = NOTIFICATION_OVERLAYS.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return source.contains("UIManager.getColor(\"Panel.background\")")
                            || source.contains("UIManager.getColor(\"List.selectionBackground\")")
                            || source.contains("ModernColors.getBorderLightColor()");
                })
                .toList();

        assertTrue(violations.isEmpty(), "Use ModernColors notification colors for painted notifications: " + violations);
    }

    @Test
    public void primarySidebarSearchBarsShouldUseSharedToolbarMetrics() {
        List<String> violations = SIDEBAR_SEARCH_TOOLBARS.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSidebarToolbar"))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSidebarToolbar for primary sidebar search bars: " + violations);
    }

    @Test
    public void primarySidebarSectionHeadersShouldUseSharedHeaderMetrics() {
        List<String> violations = SIDEBAR_SECTION_HEADERS.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSidebarHeader"))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSidebarHeader for compact sidebar section headers: " + violations);
    }

    @Test
    public void compactToolWindowActionRowsShouldUseSharedToolbarMetrics() {
        List<String> violations = TOOL_WINDOW_ACTION_TOOLBARS.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowActionToolbar"))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowActionToolbar for compact tool-window action rows: " + violations);
    }

    @Test
    public void primaryToolWindowPanelsShouldUseSharedSurfaceStyle() {
        List<String> violations = PRIMARY_TOOL_WINDOW_SURFACES.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return !source.contains("ToolWindowSurfaceStyle.applyBackground(this)")
                            && !source.contains("ToolWindowSurfaceStyle.applyCard(this)");
                })
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSurfaceStyle at primary tool-window roots: " + violations);
    }

    @Test
    public void compactToolWindowPanelsShouldRespectConfiguredUiFontSize() {
        List<String> violations = COMPACT_TOOL_WINDOW_FONT_PANELS.stream()
                .filter(path -> ABSOLUTE_UI_FONT_PATTERN.matcher(readUnchecked(ROOT.resolve(path))).find())
                .toList();

        assertTrue(violations.isEmpty(), "Use FontsUtil offsets instead of absolute UI font sizes: " + violations);
    }

    @Test
    public void largeToolWindowLayoutsShouldUseSharedChromeWrappers() {
        List<String> violations = LARGE_TOOL_WINDOW_LAYOUTS.stream()
                .filter(path -> {
                    String source = readUnchecked(ROOT.resolve(path));
                    return !source.contains("ToolWindowChrome.createHorizontalCardSplitPane")
                            && !source.contains("ToolWindowChrome.createVerticalCardSplitPane")
                            && !source.contains("ToolWindowChrome.wrapToolWindow");
                })
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowChrome wrappers for large tool-window layouts: " + violations);
    }

    @Test
    public void primaryHorizontalSplitsShouldUseSharedDefaultSidebarWidth() {
        List<String> violations = PRIMARY_HORIZONTAL_SPLITS.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowChrome.DEFAULT_SIDE_WIDTH"))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowChrome.DEFAULT_SIDE_WIDTH for primary side panes: " + violations);
    }

    @Test
    public void stackedToolWindowSplitsShouldAvoidDoubleWrappingNestedCards() {
        List<String> violations = STACKED_TOOL_WINDOW_SPLITS.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("createVerticalStackedCardSplitPane"))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use ToolWindowChrome.createVerticalStackedCardSplitPane when the top pane already has card chrome: "
                        + violations);
    }

    @Test
    public void toolWindowDialogsShouldUseSharedRoundedShells() {
        List<String> violations = TOOL_WINDOW_DIALOG_SHELLS.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowChrome.wrapDialogToolWindow"))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use ToolWindowChrome.wrapDialogToolWindow for tool-window dialogs: " + violations);
    }

    @Test
    public void settingsTablesShouldUseSharedSurfaceStyle() {
        List<String> violations = SETTINGS_TABLE_SURFACES.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSurfaceStyle.applyTableScrollPaneCard"))
                .toList();

        assertTrue(violations.isEmpty(), "Use ToolWindowSurfaceStyle table wrappers for settings tables: " + violations);
    }

    @Test
    public void sideListAndTreeScrollPanesShouldUseSharedSurfaceStyle() {
        List<String> listViolations = LIST_SCROLL_SURFACES.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSurfaceStyle.applyListScrollPaneCard"))
                .toList();
        List<String> treeViolations = TREE_SCROLL_SURFACES.stream()
                .filter(path -> !readUnchecked(ROOT.resolve(path)).contains("ToolWindowSurfaceStyle.applyTreeScrollPaneCard"))
                .toList();

        assertTrue(listViolations.isEmpty(), "Use shared list scroll-pane surface style: " + listViolations);
        assertTrue(treeViolations.isEmpty(), "Use shared tree scroll-pane surface style: " + treeViolations);
    }

    @Test
    public void highLevelSectionSurfacesShouldNotUseHandDrawnDividers() {
        List<String> violations = BORDERLESS_SECTION_SURFACES.stream()
                .filter(path -> containsLegacySectionBorder(readUnchecked(ROOT.resolve(path))))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use ToolWindowSurfaceStyle.applySectionHeader or borderless section surfaces instead of hand-drawn dividers: "
                        + violations);
    }

    @Test
    public void placeholderShellsShouldNotPaintLegacyDividerLines() {
        List<String> violations = PLACEHOLDER_SHELLS_WITHOUT_DRAWN_DIVIDERS.stream()
                .filter(path -> readUnchecked(ROOT.resolve(path)).contains("drawLine("))
                .toList();

        assertTrue(violations.isEmpty(),
                "Use card gaps/backgrounds in placeholder shells instead of painted divider lines: " + violations);
    }

    @Test
    public void legacySwingBordersShouldRemainLimitedToFunctionalFeedback() throws IOException {
        List<String> violations = javaSources()
                .filter(path -> containsLegacySectionBorder(readUnchecked(path)))
                .map(ROOT::relativize)
                .map(path -> path.toString().replace('\\', '/'))
                .filter(path -> !ALLOWED_FUNCTIONAL_BORDER_FILES.contains(path))
                .toList();

        assertTrue(violations.isEmpty(),
                "Do not add ad-hoc Swing borders outside explicit focus, popup, table-button, or status-stripe feedback: "
                        + violations);
    }

    @Test
    public void appAndPluginCodeShouldUseThemeAwareModernColorGetters() throws IOException {
        List<String> violations = javaSources().filter(path -> {
                    String source = readUnchecked(path);
                    return THEME_CONSTANTS.stream().anyMatch(source::contains);
                })
                .map(ROOT::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), "Use ModernColors getter methods so light/dark themes can diverge safely: " + violations);
    }

    private static boolean containsLegacySectionBorder(String source) {
        return source.contains("createMatteBorder")
                || source.contains("createLineBorder")
                || source.contains("createCompoundBorder")
                || source.contains("new MatteBorder");
    }

    private static Stream<Path> javaSources() throws IOException {
        Stream<Path> appSources = Stream.of(ROOT.resolve("easy-postman-app/src/main/java"));
        Stream<Path> pluginSources = pluginMainSourceRoots();
        return Stream.concat(appSources, pluginSources)
                .filter(Files::isDirectory)
                .flatMap(ToolWindowStyleConventionsTest::walkUnchecked)
                .filter(path -> path.toString().endsWith(".java"));
    }

    private static Stream<Path> pluginMainSourceRoots() throws IOException {
        Path pluginsRoot = ROOT.resolve("easy-postman-plugins");
        if (!Files.isDirectory(pluginsRoot)) {
            return Stream.empty();
        }
        try (Stream<Path> pluginDirs = Files.list(pluginsRoot)) {
            return pluginDirs
                    .filter(Files::isDirectory)
                    .map(path -> path.resolve("src/main/java"))
                    .toList()
                    .stream();
        }
    }

    private static Stream<Path> walkUnchecked(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new IllegalStateException(e);
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
