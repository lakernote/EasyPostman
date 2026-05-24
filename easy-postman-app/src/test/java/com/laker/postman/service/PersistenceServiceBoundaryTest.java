package com.laker.postman.service;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;

public class PersistenceServiceBoundaryTest {

    @Test
    public void functionalPersistenceServiceShouldNotDependOnUiSingletons() throws IOException {
        assertNoUiDependency(readMainSource("com/laker/postman/service/FunctionalPersistenceService.java"));
    }

    @Test
    public void performancePersistenceServiceShouldNotDependOnUiSingletons() throws IOException {
        assertNoUiDependency(readMainSource("com/laker/postman/service/PerformancePersistenceService.java"));
    }

    private String readMainSource(String relativePath) throws IOException {
        return Files.readString(moduleDir().resolve("src/main/java").resolve(relativePath));
    }

    private Path moduleDir() {
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(moduleDir.resolve("src/main/java"))) {
            moduleDir = moduleDir.resolve("easy-postman-app");
        }
        return moduleDir;
    }

    private void assertNoUiDependency(String source) {
        assertFalse(source.contains("UiSingletonFactory"), "persistence service must not create UI singletons");
        assertFalse(source.contains("RequestCollectionsLeftPanel"), "persistence service must not depend on collection panel");
    }
}
