package com.laker.postman.service.collections;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DefaultCollectionDocumentFactoryTest {

    @Test
    public void shouldCreateDefaultCollectionDocumentWithoutSwingTreeState() {
        CollectionDocument document = DefaultCollectionDocumentFactory.create();

        assertEquals(document.getRoots().size(), 3);
        List<String> groupNames = document.getRoots().stream()
                .map(CollectionNode::asGroup)
                .map(com.laker.postman.collection.model.RequestGroup::getName)
                .toList();
        assertTrue(groupNames.contains("Basic HTTP Examples"));
        assertTrue(groupNames.contains("Script Examples (EN)"));
        assertTrue(groupNames.contains("脚本示例（中文）"));
        assertTrue(document.getRoots().stream().allMatch(CollectionNode::isGroup));
        assertTrue(document.getRoots().stream().allMatch(root -> !root.getChildren().isEmpty()));
    }

    @Test
    public void defaultDocumentFactoryShouldNotDependOnSwingTreeTypes() throws Exception {
        String source = Files.readString(mainSourcePath(
                "com/laker/postman/service/collections/DefaultCollectionDocumentFactory.java"));

        assertFalse(source.contains("DefaultMutableTreeNode"));
        assertFalse(source.contains("DefaultTreeModel"));
        assertFalse(source.contains("CollectionTreeNodes"));
    }

    private Path mainSourcePath(String relativePath) {
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(moduleDir.resolve("src/main/java"))) {
            moduleDir = moduleDir.resolve("easy-postman-app");
        }
        return moduleDir.resolve("src/main/java").resolve(relativePath);
    }
}
