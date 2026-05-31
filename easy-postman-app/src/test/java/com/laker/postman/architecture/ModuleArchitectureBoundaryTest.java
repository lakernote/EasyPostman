package com.laker.postman.architecture;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ModuleArchitectureBoundaryTest {

    @Test
    public void mavenModulesUseFoundationAndUiNames() throws IOException {
        Path root = repositoryRoot();
        assertTrue(Files.isDirectory(root.resolve("easy-postman-foundation")));
        assertTrue(Files.isDirectory(root.resolve("easy-postman-platform")));
        assertTrue(Files.isDirectory(root.resolve("easy-postman-ui")));
        assertFalse(Files.exists(root.resolve("easy-postman-performance-runtime-okhttp")));
        assertFalse(Files.exists(root.resolve("easy-postman-plugin-bridge")));
        assertFalse(Files.exists(root.resolve("easy-postman-plugin-ui")));

        for (Path pom : pomFiles(root)) {
            String source = Files.readString(pom);
            assertFalse(source.contains("easy-postman-performance-runtime-okhttp"), pom + " still references the retired performance runtime module");
            assertFalse(source.contains("easy-postman-plugin-bridge"), pom + " still references the old bridge module");
            assertFalse(source.contains("easy-postman-plugin-ui"), pom + " still references the old plugin UI module");
        }
    }

    @Test
    public void packagingRuntimeModulesIncludePerformanceWorkerHttpServer() throws IOException {
        Path root = repositoryRoot();
        List<Path> packagingFiles = List.of(
                root.resolve(".github/workflows/pr-check.yml"),
                root.resolve(".github/workflows/release.yml"),
                root.resolve("build/mac.sh"),
                root.resolve("build/linux-deb.sh"),
                root.resolve("build/linux-rpm.sh"),
                root.resolve("build/win-exe.bat")
        );

        for (Path file : packagingFiles) {
            String source = Files.readString(file);
            assertTrue(source.contains("java.net.http"), file + " must keep java.net.http for master HTTP client");
            assertTrue(source.contains("jdk.httpserver"), file + " must keep jdk.httpserver for worker mode");
        }
    }

    @Test
    public void pluginServiceContractsLiveInPluginApi() {
        Path root = repositoryRoot();
        Path servicePackage = root.resolve("easy-postman-plugin-api/src/main/java/com/laker/postman/plugin/api/service");
        assertTrue(Files.isRegularFile(servicePackage.resolve("GitPluginService.java")));
        assertTrue(Files.isRegularFile(servicePackage.resolve("ClientCertificatePluginService.java")));
        assertTrue(Files.isRegularFile(servicePackage.resolve("RequestCollectionImportService.java")));
        assertFalse(Files.exists(root.resolve("easy-postman-foundation/src/main/java/com/laker/postman/plugin/bridge")),
                "Foundation must not own plugin service contracts");
    }

    @Test
    public void platformOwnsIocFramework() {
        Path root = repositoryRoot();
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-platform/src/main/java/com/laker/postman/ioc/BeanFactory.java")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-platform/src/main/java/com/laker/postman/ioc/ApplicationContext.java")));
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/ioc")),
                "The app module must consume the IOC framework from easy-postman-platform instead of owning it");
    }

    @Test
    public void platformOwnsUpdateDiscoveryCore() {
        Path root = repositoryRoot();
        List<String> platformUpdateCoreFiles = List.of(
                "UpdateSettingsProvider.java",
                "VersionChecker.java",
                "WindowsRegistryChecker.java",
                "model/UpdateInfo.java",
                "model/UpdateCheckFrequency.java",
                "version/VersionComparator.java",
                "asset/AssetFinder.java",
                "asset/PlatformDownloadUrlResolver.java",
                "asset/WindowsVersionDetector.java",
                "source/AbstractUpdateSource.java",
                "source/AppReleaseSelector.java",
                "source/GitHubUpdateSource.java",
                "source/GiteeUpdateSource.java",
                "source/UpdateSource.java",
                "source/UpdateSourceSelector.java",
                "changelog/ChangelogFormatter.java",
                "changelog/ChangelogService.java"
        );

        for (String updateCoreFile : platformUpdateCoreFiles) {
            assertTrue(Files.isRegularFile(root.resolve("easy-postman-platform/src/main/java/com/laker/postman/platform/update/" + updateCoreFile)),
                    updateCoreFile + " belongs in easy-postman-platform");
        }

        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/model/UpdateInfo.java")),
                "UpdateInfo is platform update discovery data, not app UI state");
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/model/UpdateCheckFrequency.java")),
                "UpdateCheckFrequency is platform update discovery policy, not app UI state");
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/service/update/VersionChecker.java")),
                "VersionChecker belongs in easy-postman-platform");
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/service/update/version")),
                "Version comparison belongs in easy-postman-platform");
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/service/update/source")),
                "Update sources belong in easy-postman-platform");
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/service/update/asset")),
                "Update asset resolution belongs in easy-postman-platform");
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/service/update/changelog")),
                "Changelog fetching/formatting belongs in easy-postman-platform");

        assertTrue(Files.isRegularFile(root.resolve("easy-postman-app/src/main/java/com/laker/postman/service/update/AutoUpdateManager.java")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-app/src/main/java/com/laker/postman/service/update/UpdateUIManager.java")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-app/src/main/java/com/laker/postman/service/update/UpdateDownloader.java")));
    }

    @Test
    public void uiModuleOwnsReusableSwingSingletonFramework() {
        Path root = repositoryRoot();
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/java/com/laker/postman/common/UiSingletonFactory.java")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/java/com/laker/postman/common/UiSingletonPanel.java")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/java/com/laker/postman/common/UiSingletonMenuBar.java")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/java/com/laker/postman/common/exception/GetInstanceException.java")));
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/common/UiSingletonFactory.java")),
                "Reusable Swing singleton framework belongs in easy-postman-ui");
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/common/UiSingletonPanel.java")),
                "Reusable Swing singleton framework belongs in easy-postman-ui");
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/common/UiSingletonMenuBar.java")),
                "Reusable Swing singleton framework belongs in easy-postman-ui");
    }

    @Test
    public void uiModuleOwnsReusableSwingRefreshAndSaveHelpers() {
        Path root = repositoryRoot();
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/java/com/laker/postman/common/DebouncedSaveSupport.java")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/java/com/laker/postman/common/IRefreshable.java")));
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/common/DebouncedSaveSupport.java")),
                "Reusable Swing save helper belongs in easy-postman-ui");
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/common/IRefreshable.java")),
                "Reusable Swing refresh contract belongs in easy-postman-ui");
    }

    @Test
    public void uiModuleOwnsReusableSwingFormControls() {
        Path root = repositoryRoot();
        List<String> controlFiles = List.of(
                "EasyComboBox.java",
                "EasyJSpinner.java",
                "EasyPasswordField.java"
        );

        for (String controlFile : controlFiles) {
            assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/java/com/laker/postman/common/component/" + controlFile)),
                    controlFile + " belongs in easy-postman-ui");
            assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/common/component/" + controlFile)),
                    controlFile + " must not be owned by the app module");
        }
    }

    @Test
    public void uiModuleOwnsReusableToolbarButtons() {
        Path root = repositoryRoot();
        List<String> buttonFiles = List.of(
                "DownloadButton.java",
                "EditButton.java",
                "ExportButton.java",
                "FormatButton.java",
                "HelpButton.java",
                "ImportButton.java",
                "LoadButton.java",
                "PlusButton.java",
                "SaveButton.java",
                "SearchButton.java",
                "StartButton.java",
                "StopButton.java",
                "WrapToggleButton.java"
        );

        for (String buttonFile : buttonFiles) {
            assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/java/com/laker/postman/common/component/button/" + buttonFile)),
                    buttonFile + " belongs in easy-postman-ui");
            assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/common/component/button/" + buttonFile)),
                    buttonFile + " must not be owned by the app module");
        }
    }

    @Test
    public void foundationOwnsReusableNonUiUtilities() {
        Path root = repositoryRoot();
        List<String> utilityFiles = List.of(
                "FileExtensionUtil.java",
                "FileSizeDisplayUtil.java",
                "HttpHeaderConstants.java",
                "CronExpressionUtil.java",
                "JsonPathUtil.java",
                "XmlUtil.java",
                "TimeDisplayUtil.java"
        );

        for (String utilityFile : utilityFiles) {
            assertTrue(Files.isRegularFile(root.resolve("easy-postman-foundation/src/main/java/com/laker/postman/util/" + utilityFile)),
                    utilityFile + " belongs in easy-postman-foundation");
            assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/util/" + utilityFile)),
                    utilityFile + " must not be owned by the app module");
        }
    }

    @Test
    public void foundationDoesNotOwnUiTypes() throws IOException {
        Path foundationSource = repositoryRoot().resolve("easy-postman-foundation/src/main/java");
        List<String> uiImports = javaSourceFiles(foundationSource).stream()
                .flatMap(file -> uiImportViolations(file).stream())
                .toList();

        assertTrue(uiImports.isEmpty(),
                "Foundation must stay non-UI and must not import AWT/Swing/UI libraries: " + uiImports);
    }

    @Test
    public void appModelPackageStaysHeadless() throws IOException {
        Path modelSource = repositoryRoot().resolve("easy-postman-app/src/main/java/com/laker/postman/model");
        List<String> violations = sourcePackageViolations(modelSource, List.of(
                "javax.swing",
                "java.awt",
                "com.formdev",
                "org.fife",
                "UiSingletonFactory",
                "IconUtil",
                "com.laker.postman.panel."
        ));

        assertTrue(violations.isEmpty(),
                "App model package must stay headless and must not depend on Swing/editor/UI panel types: " + violations);
    }

    @Test
    public void serviceLayerDoesNotDependOnSwingPanels() throws IOException {
        Path serviceSource = repositoryRoot().resolve("easy-postman-app/src/main/java/com/laker/postman/service");
        List<String> violations = sourcePackageViolations(serviceSource, List.of(
                "com.laker.postman.panel.",
                "com.laker.postman.common.component.CsvDataPanel",
                "CsvDataPanel.CsvState"
        ));

        assertTrue(violations.isEmpty(),
                "App service layer must not depend on concrete Swing panels or panel-owned DTOs: " + violations);
    }

    @Test
    public void pluginApiDoesNotExposeEditorImplementationTypes() throws IOException {
        Path pluginApiSource = repositoryRoot().resolve("easy-postman-plugin-api/src/main/java");
        List<String> violations = sourcePackageViolations(pluginApiSource, List.of(
                "org.fife.ui.autocomplete"
        ));

        assertTrue(violations.isEmpty(),
                "Plugin API must not expose concrete editor implementation types: " + violations);
    }

    @Test
    public void performanceHeadlessPackagesStayOutOfPanelNamespace() throws IOException {
        Path root = repositoryRoot();
        List<Path> retiredHeadlessPanelPackages = List.of(
                root.resolve("easy-postman-app/src/main/java/com/laker/postman/panel/performance/execution"),
                root.resolve("easy-postman-app/src/main/java/com/laker/postman/panel/performance/runtime"),
                root.resolve("easy-postman-app/src/main/java/com/laker/postman/panel/performance/model"),
                root.resolve("easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan"),
                root.resolve("easy-postman-app/src/main/java/com/laker/postman/panel/performance/report")
        );
        List<Path> existingRetiredPackages = retiredHeadlessPanelPackages.stream()
                .filter(Files::exists)
                .toList();

        assertTrue(existingRetiredPackages.isEmpty(),
                "Headless performance execution/runtime/model/plan/report packages must move out of panel namespace: "
                        + existingRetiredPackages);

        Path performanceSource = root.resolve("easy-postman-app/src/main/java/com/laker/postman/performance");
        List<String> violations = sourcePackageViolations(performanceSource, List.of(
                "import com.laker.postman.panel.performance.execution.",
                "import com.laker.postman.panel.performance.runtime.",
                "import com.laker.postman.panel.performance.model.",
                "import com.laker.postman.panel.performance.plan.",
                "import com.laker.postman.panel.performance.report."
        ));

        assertTrue(violations.isEmpty(),
                "Headless performance packages must not import retired panel.performance implementation packages: "
                        + violations);
    }

    @Test
    public void appPluginRuntimeAccessorsUseHostPackageName() {
        Path root = repositoryRoot();
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-app/src/main/java/com/laker/postman/plugin/host/PluginAccess.java")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-app/src/main/java/com/laker/postman/plugin/host/GitServiceAccess.java")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-app/src/main/java/com/laker/postman/plugin/host/ClientCertificatePluginAccess.java")));
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/java/com/laker/postman/plugin/bridge")),
                "The retired app-side plugin.bridge package must stay removed");
    }

    @Test
    public void architectureDocsAndSkillsUseCurrentModuleNames() throws IOException {
        Path root = repositoryRoot();
        List<Path> files = List.of(
                root.resolve("AGENTS.md"),
                root.resolve("docs/ARCHITECTURE_MODULES_zh.md"),
                root.resolve("docs/PLUGINS_zh.md"),
                root.resolve("docs/PLUGIN_RUNTIME_ARCHITECTURE_zh.md"),
                root.resolve(".codex/skills/module-architecture-boundaries/SKILL.md"),
                root.resolve(".codex/skills/fontsutil-font-usage/SKILL.md"),
                root.resolve(".codex/skills/swing-flatlaf-miglayout-principles/SKILL.md")
        );

        for (Path file : files) {
            String source = Files.readString(file);
            assertFalse(source.contains("easy-postman-plugin-bridge"), file + " still documents the old bridge module");
            assertFalse(source.contains("easy-postman-plugin-ui"), file + " still documents the old plugin UI module");
            assertFalse(source.contains("api / bridge / ui"), file + " still documents the old bridge wording");
            assertFalse(source.contains("easy-postman-performance-runtime-okhttp"),
                    file + " still documents the retired performance runtime module");
        }
    }

    @Test
    public void appAndUiIconResourcesHaveSingleOwner() throws IOException {
        Path root = repositoryRoot();
        Set<String> appIcons = resourceFileNames(root.resolve("easy-postman-app/src/main/resources/icons"));
        Set<String> uiIcons = resourceFileNames(root.resolve("easy-postman-ui/src/main/resources/icons"));
        appIcons.retainAll(uiIcons);

        assertTrue(appIcons.isEmpty(),
                "Icon resources must have a single owner. Shared control icons belong in easy-postman-ui, "
                        + "and app/domain icons belong in easy-postman-app: " + appIcons);
    }

    @Test
    public void uiModuleOwnsReusableUiResources() {
        Path root = repositoryRoot();
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-foundation/src/main/resources/common-messages.properties")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-foundation/src/main/resources/common-messages_zh.properties")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/resources/ui-messages.properties")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/resources/ui-messages_zh.properties")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/resources/themes/easypostman-light.xml")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-ui/src/main/resources/themes/easypostman-dark.xml")));
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/resources/themes/easypostman-light.xml")));
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/resources/themes/easypostman-dark.xml")));
    }

    @Test
    public void sharedControlIconsStayInUiResources() throws IOException {
        Path root = repositoryRoot();
        Set<String> uiIcons = resourceFileNames(root.resolve("easy-postman-ui/src/main/resources/icons"));
        Set<String> appIcons = resourceFileNames(root.resolve("easy-postman-app/src/main/resources/icons"));
        List<String> sharedIcons = List.of(
                "arrow-down.svg",
                "arrow-up.svg",
                "cancel.svg",
                "check.svg",
                "chevron-down.svg",
                "chevron-right.svg",
                "clear.svg",
                "close.svg",
                "collapse.svg",
                "connect.svg",
                "copy.svg",
                "delete.svg",
                "detail.svg",
                "download.svg",
                "duplicate.svg",
                "edit.svg",
                "expand.svg",
                "export.svg",
                "eye-close.svg",
                "eye-open.svg",
                "file.svg",
                "format.svg",
                "import.svg",
                "info.svg",
                "load.svg",
                "matchCase.svg",
                "matchCaseHovered.svg",
                "matchCaseSelected.svg",
                "more.svg",
                "paste.svg",
                "plus.svg",
                "refresh.svg",
                "replace-all.svg",
                "replace.svg",
                "save.svg",
                "search.svg",
                "send.svg",
                "start.svg",
                "stop.svg",
                "text-file.svg",
                "warning.svg",
                "words.svg",
                "wordsHovered.svg",
                "wordsSelected.svg",
                "wrap.svg",
                "ws-close.svg"
        );

        for (String icon : sharedIcons) {
            assertTrue(uiIcons.contains(icon), icon + " is a reusable control/status icon and belongs in easy-postman-ui");
            assertFalse(appIcons.contains(icon), icon + " must not be duplicated or owned by easy-postman-app");
        }
    }

    @Test
    public void genericCommonMessageKeysStayOutOfModuleOwnedBundles() throws IOException {
        Path root = repositoryRoot();
        Set<String> commonKeys = resourceKeys(root.resolve("easy-postman-foundation/src/main/resources/common-messages.properties"));
        List<Path> moduleBundles;
        try (Stream<Path> files = Files.walk(root)) {
            moduleBundles = files
                    .filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .filter(path -> path.getFileName().toString().contains("messages"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> !path.toString().contains("/easy-postman-foundation/src/main/resources/common-messages"))
                    .toList();
        }

        List<String> violations = moduleBundles.stream()
                .flatMap(file -> resourceKeys(file).stream()
                        .filter(commonKeys::contains)
                        .map(key -> file + " duplicates common key " + key))
                .toList();

        assertTrue(violations.isEmpty(),
                "Generic short labels belong only in foundation common-messages bundles: " + violations);
    }

    @Test
    public void uiModuleDoesNotDependOnAppMessageBundleKeys() throws IOException {
        Path root = repositoryRoot();
        List<String> uiSourceViolations = javaSourceFiles(root.resolve("easy-postman-ui/src/main/java")).stream()
                .flatMap(file -> uiMessageKeyViolations(file).stream())
                .toList();
        assertTrue(uiSourceViolations.isEmpty(),
                "Shared UI code must use UiI18n/UiMessageKeys or CommonI18n/CommonMessageKeys instead of app MessageKeys: "
                        + uiSourceViolations);

        List<String> appBundleViolations = Stream.of(
                        root.resolve("easy-postman-app/src/main/resources/messages_en.properties"),
                        root.resolve("easy-postman-app/src/main/resources/messages_zh.properties")
                )
                .flatMap(file -> resourceBundleOwnerViolations(file, "table.", "notification.").stream())
                .toList();
        assertTrue(appBundleViolations.isEmpty(),
                "Shared UI table/notification strings belong in easy-postman-ui ui-messages bundles: "
                        + appBundleViolations);
    }

    @Test
    public void officialPluginMessagesStayWithPluginModules() throws IOException {
        Path root = repositoryRoot();
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-plugins/plugin-decompiler/src/main/resources/decompiler-messages.properties")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-plugins/plugin-decompiler/src/main/resources/decompiler-messages_zh.properties")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-plugins/plugin-client-cert/src/main/resources/client-cert-messages.properties")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-plugins/plugin-client-cert/src/main/resources/client-cert-messages_zh.properties")));

        List<String> appBundleViolations = Stream.of(
                        root.resolve("easy-postman-app/src/main/resources/messages_en.properties"),
                        root.resolve("easy-postman-app/src/main/resources/messages_zh.properties")
                )
                .flatMap(file -> resourceBundleOwnerViolations(file, "toolbox.decompiler").stream())
                .toList();
        assertTrue(appBundleViolations.isEmpty(),
                "Decompiler plugin strings belong in plugin-decompiler message bundles: " + appBundleViolations);

        List<String> pluginSourceViolations = Stream.of(
                        root.resolve("easy-postman-plugins/plugin-decompiler/src/main/java"),
                        root.resolve("easy-postman-plugins/plugin-client-cert/src/main/java")
                )
                .flatMap(sourceRoot -> {
                    try {
                        return javaSourceFiles(sourceRoot).stream();
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to read " + sourceRoot, e);
                    }
                })
                .flatMap(file -> pluginAppMessageKeyViolations(file).stream())
                .toList();
        assertTrue(pluginSourceViolations.isEmpty(),
                "Plugins must use plugin-local message keys/bundles instead of app MessageKeys: "
                        + pluginSourceViolations);
    }

    @Test
    public void pluginEntryIconsStayWithPlugins() {
        Path root = repositoryRoot();
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-plugins/plugin-kafka/src/main/resources/icons/kafka.svg")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-plugins/plugin-decompiler/src/main/resources/icons/decompile.svg")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-plugins/plugin-redis/src/main/resources/icons/redis.svg")));
        assertTrue(Files.isRegularFile(root.resolve("easy-postman-plugins/plugin-capture/src/main/resources/icons/capture.svg")));
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/resources/icons/kafka.svg")));
        assertFalse(Files.exists(root.resolve("easy-postman-app/src/main/resources/icons/decompile.svg")));
    }

    @Test
    public void pluginIconReferencesUsePluginOrSharedUiResources() throws IOException {
        Path root = repositoryRoot();
        Set<String> uiIcons = resourceFileNames(root.resolve("easy-postman-ui/src/main/resources/icons"));
        Set<String> pluginIcons = pluginIconFileNames(root.resolve("easy-postman-plugins"));
        Set<String> appIcons = resourceFileNames(root.resolve("easy-postman-app/src/main/resources/icons"));

        List<String> violations = javaSourceFiles(root.resolve("easy-postman-plugins")).stream()
                .flatMap(file -> iconReferences(file).stream()
                        .filter(icon -> !uiIcons.contains(icon) && !pluginIcons.contains(icon))
                        .map(icon -> file + " references app-only or missing icon " + icon
                                + (appIcons.contains(icon) ? " from easy-postman-app" : "")))
                .toList();

        assertTrue(violations.isEmpty(),
                "Official plugins must package plugin-specific icons or use shared icons from easy-postman-ui: "
                        + violations);
    }

    @Test
    public void resourcesDoNotContainMacMetadataFiles() throws IOException {
        Path root = repositoryRoot();
        List<Path> metadataFiles;
        try (Stream<Path> files = Files.walk(root)) {
            metadataFiles = files
                    .filter(path -> path.getFileName().toString().equals(".DS_Store"))
                    .filter(path -> !path.toString().contains("/.git/"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .toList();
        }

        assertTrue(metadataFiles.isEmpty(), "Resource tree must not contain macOS metadata files: " + metadataFiles);
    }

    private static List<Path> javaSourceFiles(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
        }
    }

    private static List<String> sourceContainsViolations(Path file, List<String> forbiddenPatterns) {
        try {
            String source = Files.readString(file);
            return forbiddenPatterns.stream()
                    .filter(source::contains)
                    .map(pattern -> file + " contains " + pattern)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }

    private static List<String> sourcePackageViolations(Path sourceRoot, List<String> forbiddenPatterns) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return List.of();
        }
        return javaSourceFiles(sourceRoot).stream()
                .flatMap(file -> sourceContainsViolations(file, forbiddenPatterns).stream())
                .toList();
    }

    private static List<String> uiImportViolations(Path file) {
        try {
            String source = Files.readString(file);
            return List.of("java.awt", "javax.swing", "com.formdev", "net.miginfocom", "org.fife").stream()
                    .filter(source::contains)
                    .map(pattern -> file + " contains " + pattern)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }

    private static List<String> uiMessageKeyViolations(Path file) {
        try {
            String source = Files.readString(file)
                    .replace("UiMessageKeys.", "")
                    .replace("CommonMessageKeys.", "");
            return List.of(
                            "import com.laker.postman.util.MessageKeys",
                            "MessageKeys.",
                            "I18nUtil.getMessage(MessageKeys"
                    ).stream()
                    .filter(source::contains)
                    .map(pattern -> file + " contains " + pattern)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }

    private static List<String> pluginAppMessageKeyViolations(Path file) {
        try {
            String source = Files.readString(file);
            return List.of(
                            "import com.laker.postman.util.MessageKeys",
                            "I18nUtil.getMessage(MessageKeys"
                    ).stream()
                    .filter(source::contains)
                    .map(pattern -> file + " contains " + pattern)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }

    private static List<String> resourceBundleOwnerViolations(Path file, String... forbiddenPrefixes) {
        try {
            return Files.readString(file).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .filter(line -> Stream.of(forbiddenPrefixes).anyMatch(line::startsWith))
                    .map(line -> file + " contains " + line)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }

    private static Set<String> resourceFileNames(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Set<String> resourceKeys(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return Set.of();
            }
            return Files.readString(file).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .filter(line -> line.contains("="))
                    .map(line -> line.substring(0, line.indexOf('=')).trim())
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }

    private static Set<String> pluginIconFileNames(Path pluginsRoot) throws IOException {
        if (!Files.isDirectory(pluginsRoot)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.walk(pluginsRoot)) {
            return files
                    .filter(path -> path.toString().contains("/src/main/resources/icons/"))
                    .filter(path -> path.getFileName().toString().endsWith(".svg"))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static List<String> iconReferences(Path file) {
        try {
            String source = Files.readString(file);
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("icons/([A-Za-z0-9_.-]+\\.svg)")
                    .matcher(source);
            List<String> icons = new java.util.ArrayList<>();
            while (matcher.find()) {
                icons.add(matcher.group(1));
            }
            return icons;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file, e);
        }
    }

    private static List<Path> pomFiles(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .toList();
        }
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("easy-postman-app"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root");
    }
}
