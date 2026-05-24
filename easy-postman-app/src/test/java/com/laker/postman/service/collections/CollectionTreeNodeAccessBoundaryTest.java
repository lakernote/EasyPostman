package com.laker.postman.service.collections;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class CollectionTreeNodeAccessBoundaryTest {

    private static final List<String> FILES_THAT_MUST_USE_COLLECTION_TREE_NODES = List.of(
            "src/main/java/com/laker/postman/panel/collections/editor/request/SavedResponseHelper.java",
            "src/main/java/com/laker/postman/panel/collections/editor/RequestEditorPanel.java",
            "src/main/java/com/laker/postman/panel/collections/tree/CollectionGroupSelectionDialog.java",
            "src/main/java/com/laker/postman/panel/collections/tree/CollectionTreePanel.java",
            "src/main/java/com/laker/postman/panel/collections/tree/CollectionTreeToolbar.java",
            "src/main/java/com/laker/postman/panel/collections/tree/action/RequestTreeActions.java",
            "src/main/java/com/laker/postman/panel/collections/tree/handler/RequestTreeKeyboardHandler.java",
            "src/main/java/com/laker/postman/panel/collections/tree/handler/RequestTreeMouseHandler.java",
            "src/main/java/com/laker/postman/panel/collections/tree/handler/RequestTreePopupMenu.java",
            "src/main/java/com/laker/postman/plugin/bridge/AppRequestCollectionImportService.java",
            "src/main/java/com/laker/postman/service/collections/GroupInheritanceHelper.java"
    );

    @Test
    public void collectionTreeConsumersShouldNotIndexNodeUserObjectArraysDirectly() throws IOException {
        List<String> violations = FILES_THAT_MUST_USE_COLLECTION_TREE_NODES.stream()
                .flatMap(relativePath -> violationsIn(relativePath).stream())
                .toList();

        assertTrue(violations.isEmpty(),
                "Collection tree consumers must use CollectionTreeNodes accessors: " + violations);
    }

    private List<String> violationsIn(String relativePath) {
        Path sourceFile = moduleDir().resolve(relativePath);
        try {
            String source = Files.readString(sourceFile);
            return List.of(
                            "obj[0]",
                            "obj[1]",
                            "Object[] nodeObj",
                            "Object[] groupObj",
                            "(Object[]) requestNode.getUserObject()",
                            "node.getUserObject() instanceof Object[]"
                    ).stream()
                    .filter(source::contains)
                    .map(pattern -> relativePath + " contains " + pattern)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + sourceFile, e);
        }
    }

    private Path moduleDir() {
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(moduleDir.resolve("src/main/java"))) {
            moduleDir = moduleDir.resolve("easy-postman-app");
        }
        return moduleDir;
    }
}
