package com.laker.postman.service.collections;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;

public class CollectionTreeQueryServiceBoundaryTest {

    @Test
    public void shouldNotDependOnUiSingletonsOrDialogs() throws IOException {
        assertNoUiDependencies(readMainSource("com/laker/postman/service/collections/CollectionTreeQueryService.java"));
    }

    @Test
    public void openedRequestsStoreShouldOnlyHandlePersistence() throws IOException {
        assertNoUiDependencies(readMainSource("com/laker/postman/service/collections/OpenedRequestTabsStore.java"));
    }

    @Test
    public void collectionServicePackageShouldNotImportPanelLayer() throws IOException {
        Path collectionsPackage = mainSourcePath("com/laker/postman/service/collections");
        try (var paths = Files.walk(collectionsPackage)) {
            for (Path sourceFile : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertNoUiDependencies(Files.readString(sourceFile));
            }
        }
    }

    @Test
    public void uiTabCoordinationShouldNotUseServiceNames() {
        assertFalse(Files.exists(mainSourcePath("com/laker/postman/service/collections/RequestsTabsService.java")),
                "request tab UI coordination must live in panel layer");
        assertFalse(Files.exists(mainSourcePath("com/laker/postman/service/collections/OpenedRequestsService.java")),
                "opened request tab UI coordination must live in panel layer");
        assertFalse(Files.exists(mainSourcePath("com/laker/postman/service/collections/DefaultTreeNodeRepository.java")),
                "active collection tree lookup should use the registered root repository name");
    }

    private String readMainSource(String relativePath) throws IOException {
        return Files.readString(mainSourcePath(relativePath));
    }

    private Path mainSourcePath(String relativePath) {
        return moduleDir().resolve("src/main/java").resolve(relativePath);
    }

    private Path moduleDir() {
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(moduleDir.resolve("src/main/java"))) {
            moduleDir = moduleDir.resolve("easy-postman-app");
        }
        return moduleDir;
    }

    private void assertNoUiDependencies(String source) {
        assertFalse(source.contains("UiSingletonFactory"), "service layer must not create UI singletons");
        assertFalse(source.contains("JDialog"), "service layer must not own Swing dialogs");
        assertFalse(source.contains("JOptionPane"), "service layer must not own Swing dialogs");
        assertFalse(source.contains("MainFrame"), "service layer must not depend on app windows");
        assertFalse(source.contains("RequestEditSubPanel"), "service layer must not open request editor tabs");
        assertFalse(source.contains("com.laker.postman.panel"), "service layer must not import panel classes");
    }
}
