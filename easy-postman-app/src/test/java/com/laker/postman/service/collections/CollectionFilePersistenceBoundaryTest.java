package com.laker.postman.service.collections;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;

public class CollectionFilePersistenceBoundaryTest {

    @Test
    public void shouldRejectStringBackedGroupNodes() throws IOException {
        String source = Files.readString(moduleDir()
                .resolve("src/main/java/com/laker/postman/service/collections/CollectionFilePersistence.java"));

        assertFalse(source.contains("groupData instanceof String"),
                "CollectionFilePersistence should no longer support string-backed group nodes");
        assertFalse(source.contains("new Object[]{\"group\""),
                "CollectionFilePersistence should create group nodes through CollectionTreeNodes");
        assertFalse(source.contains("new Object[]{\"request\""),
                "CollectionFilePersistence should create request nodes through CollectionTreeNodes");
        assertFalse(source.contains("new Object[]{\"response\""),
                "CollectionFilePersistence should create saved response nodes through CollectionTreeNodes");
        assertFalse(source.contains("JSONUtil"),
                "CollectionFilePersistence should delegate JSON parsing/writing to CollectionDocumentJsonCodec");
        assertFalse(source.contains("JSONArray"),
                "CollectionFilePersistence should not own collection JSON array mapping");
        assertFalse(source.contains("JSONObject"),
                "CollectionFilePersistence should not own collection JSON object mapping");
        assertFalse(source.contains("parseGroupNode"),
                "CollectionFilePersistence should not expose old JSON-to-tree helpers");
        assertFalse(source.contains("buildGroupJson"),
                "CollectionFilePersistence should not expose old tree-to-JSON helpers");
        assertFalse(source.contains("DefaultMutableTreeNode"),
                "CollectionFilePersistence should persist CollectionDocument and must not depend on Swing tree nodes");
        assertFalse(source.contains("DefaultTreeModel"),
                "CollectionFilePersistence should not reload Swing tree models");
        assertFalse(source.contains("javax.swing"),
                "CollectionFilePersistence must stay UI-neutral");
        assertFalse(source.contains("SwingCollectionTreeDocumentMapper"),
                "CollectionFilePersistence should not map Swing tree nodes");
    }

    private Path moduleDir() {
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(moduleDir.resolve("src/main/java"))) {
            moduleDir = moduleDir.resolve("easy-postman-app");
        }
        return moduleDir;
    }
}
