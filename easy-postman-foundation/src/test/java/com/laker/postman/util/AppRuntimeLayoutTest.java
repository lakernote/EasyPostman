package com.laker.postman.util;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AppRuntimeLayoutTest {

    @Test
    public void shouldPreferAppScopedDataDirectoryForPackagedLayout() throws IOException {
        Path rootDir = Files.createTempDirectory("portable-root");
        Path appDir = Files.createDirectories(rootDir.resolve("app"));

        assertEquals(AppRuntimeLayout.resolvePortableDataDirectory(rootDir, appDir), appDir.resolve("data"));
    }

    @Test
    public void shouldFallbackToApplicationRootDataDirectoryOutsidePackagedLayout() throws IOException {
        Path rootDir = Files.createTempDirectory("portable-root");
        Path classesDir = Files.createDirectories(rootDir.resolve("target").resolve("classes"));

        assertEquals(AppRuntimeLayout.resolvePortableDataDirectory(rootDir, classesDir), rootDir.resolve("data"));
    }

    @Test
    public void shouldMigrateLegacyPortableDataDirectoryToPreferredLocation() throws IOException {
        Path rootDir = Files.createTempDirectory("portable-root");
        Path legacyDataDir = Files.createDirectories(rootDir.resolve("data"));
        Path preferredDataDir = rootDir.resolve("app").resolve("data");
        Files.writeString(legacyDataDir.resolve("collections.json"), "{}");

        Path resolved = AppRuntimeLayout.harmonizePortableDataDirectory(preferredDataDir, legacyDataDir);

        assertEquals(resolved, preferredDataDir);
        assertTrue(Files.exists(preferredDataDir.resolve("collections.json")));
        assertFalse(Files.exists(legacyDataDir));
    }
}
