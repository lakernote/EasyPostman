package com.laker.postman.service.collections;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.RequestGroup;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class CollectionFilePersistenceTest {

    @Test
    public void shouldSaveAndLoadCollectionDocumentWithoutSwingTreeState() throws Exception {
        Path file = Files.createTempFile("collection-file-store", ".json");
        CollectionFilePersistence persistence = new CollectionFilePersistence(file.toString());
        CollectionDocument document = document("group-1", "Group");

        persistence.save(document);
        CollectionDocument loaded = persistence.loadOrCreate(CollectionDocument::empty);

        assertEquals(loaded.getRoots().size(), 1);
        assertEquals(loaded.getRoots().get(0).asGroup().getId(), "group-1");
    }

    @Test
    public void shouldCreateMissingFileFromDefaultDocument() throws Exception {
        Path file = Files.createTempDirectory("collection-file-store")
                .resolve("missing-collections.json");
        CollectionFilePersistence persistence = new CollectionFilePersistence(file.toString());

        CollectionDocument loaded = persistence.loadOrCreate(() -> document("default-group", "Default"));

        assertEquals(loaded.getRoots().get(0).asGroup().getId(), "default-group");
        assertEquals(Files.exists(file), true);
    }

    @Test
    public void shouldSwitchFilePathAndLoadTargetDocument() throws Exception {
        Path firstFile = Files.createTempFile("collection-file-store-first", ".json");
        Path secondFile = Files.createTempFile("collection-file-store-second", ".json");
        CollectionFilePersistence first = new CollectionFilePersistence(firstFile.toString());
        CollectionFilePersistence second = new CollectionFilePersistence(secondFile.toString());
        second.save(document("target-group", "Target"));

        CollectionDocument loaded = first.switchFilePathAndLoad(secondFile.toString(), CollectionDocument::empty);

        assertEquals(loaded.getRoots().get(0).asGroup().getId(), "target-group");
    }

    private CollectionDocument document(String id, String name) {
        RequestGroup group = new RequestGroup(name);
        group.setId(id);
        return new CollectionDocument(List.of(CollectionNode.group(group)));
    }
}
