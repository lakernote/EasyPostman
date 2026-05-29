package com.laker.postman.architecture;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        assertFalse(Files.exists(root.resolve("easy-postman-plugin-bridge")));
        assertFalse(Files.exists(root.resolve("easy-postman-plugin-ui")));

        for (Path pom : pomFiles(root)) {
            String source = Files.readString(pom);
            assertFalse(source.contains("easy-postman-plugin-bridge"), pom + " still references the old bridge module");
            assertFalse(source.contains("easy-postman-plugin-ui"), pom + " still references the old plugin UI module");
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
        }
    }

    private static List<Path> javaSourceFiles(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
        }
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
