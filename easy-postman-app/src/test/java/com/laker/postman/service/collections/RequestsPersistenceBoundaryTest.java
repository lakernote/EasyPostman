package com.laker.postman.service.collections;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;

public class RequestsPersistenceBoundaryTest {

    @Test
    public void shouldNotPersistLegacyStringGroupNodes() throws IOException {
        String source = Files.readString(moduleDir()
                .resolve("src/main/java/com/laker/postman/service/collections/RequestsPersistence.java"));

        assertFalse(source.contains("groupData instanceof String"),
                "RequestsPersistence should no longer support legacy String group nodes");
        assertFalse(source.contains("new Object[]{\"group\""),
                "RequestsPersistence should create group nodes through CollectionTreeNodes");
        assertFalse(source.contains("new Object[]{\"request\""),
                "RequestsPersistence should create request nodes through CollectionTreeNodes");
        assertFalse(source.contains("new Object[]{\"response\""),
                "RequestsPersistence should create saved response nodes through CollectionTreeNodes");
    }

    private Path moduleDir() {
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(moduleDir.resolve("src/main/java"))) {
            moduleDir = moduleDir.resolve("easy-postman-app");
        }
        return moduleDir;
    }
}
