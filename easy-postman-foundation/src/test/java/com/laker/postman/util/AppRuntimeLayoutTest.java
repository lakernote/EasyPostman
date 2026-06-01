package com.laker.postman.util;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;

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

}
