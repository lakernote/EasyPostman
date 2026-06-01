package com.laker.postman.service.collections;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class CollectionRequestSaveServiceTest {

    @Test
    public void shouldAddRequestToMatchingGroupAndPersistOnce() {
        RequestGroup persistedGroup = new RequestGroup("Target");
        persistedGroup.setId("group-1");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(persistedGroup);
        DefaultMutableTreeNode root = rootWith(groupNode);
        AtomicInteger persistCount = new AtomicInteger();
        CollectionRequestSaveService saveService = new CollectionRequestSaveService(root, persistCount::incrementAndGet);

        RequestGroup selectedGroup = new RequestGroup("Renamed in dialog");
        selectedGroup.setId("group-1");
        HttpRequestItem request = new HttpRequestItem();
        request.setId("request-1");

        CollectionRequestSaveService.RequestSaveResult result = saveService
                .addRequestToGroup(selectedGroup, request)
                .orElseThrow();

        assertSame(result.groupNode(), groupNode);
        assertSame(CollectionTreeNodes.request(result.requestNode()).orElseThrow(), request);
        assertEquals(groupNode.getChildCount(), 1);
        assertEquals(persistCount.get(), 1);
    }

    @Test
    public void shouldUpdateExistingRequestAndPersistOnce() {
        HttpRequestItem persisted = new HttpRequestItem();
        persisted.setId("request-1");
        persisted.setName("Persisted name");
        DefaultMutableTreeNode requestNode = CollectionTreeNodes.requestNode(persisted);
        AtomicInteger persistCount = new AtomicInteger();
        CollectionRequestSaveService saveService = new CollectionRequestSaveService(
                rootWithRequest(requestNode),
                persistCount::incrementAndGet
        );

        HttpRequestItem edited = new HttpRequestItem();
        edited.setId("request-1");
        edited.setName("Edited name");

        CollectionRequestSaveService.RequestSaveResult result = saveService
                .updateExistingRequest(edited)
                .orElseThrow();

        assertSame(result.requestNode(), requestNode);
        assertSame(result.requestItem(), edited);
        assertEquals(edited.getName(), "Persisted name");
        assertEquals(persistCount.get(), 1);
    }

    @Test
    public void shouldAppendSavedResponseAndPersistOnce() {
        HttpRequestItem treeItem = new HttpRequestItem();
        treeItem.setId("request-1");
        DefaultMutableTreeNode requestNode = CollectionTreeNodes.requestNode(treeItem);
        AtomicInteger persistCount = new AtomicInteger();
        CollectionRequestSaveService saveService = new CollectionRequestSaveService(
                rootWithRequest(requestNode),
                persistCount::incrementAndGet
        );

        HttpRequestItem editorItem = new HttpRequestItem();
        editorItem.setId("request-1");
        SavedResponse savedResponse = new SavedResponse();
        savedResponse.setId("response-1");

        CollectionRequestSaveService.SavedResponseSaveResult result = saveService
                .appendSavedResponse(editorItem, savedResponse)
                .orElseThrow();

        assertSame(result.requestNode(), requestNode);
        assertSame(result.treeRequestItem(), treeItem);
        assertEquals(requestNode.getChildCount(), 1);
        assertEquals(treeItem.getResponse().size(), 1);
        assertEquals(persistCount.get(), 1);
    }

    @Test
    public void shouldNotPersistWhenTargetCannotBeResolved() {
        AtomicInteger persistCount = new AtomicInteger();
        CollectionRequestSaveService saveService = new CollectionRequestSaveService(rootWith(), persistCount::incrementAndGet);

        assertTrue(saveService.addRequestToGroup(new RequestGroup("missing"), new HttpRequestItem()).isEmpty());
        assertTrue(saveService.updateExistingRequest(new HttpRequestItem()).isEmpty());
        assertTrue(saveService.appendSavedResponse(new HttpRequestItem(), new SavedResponse()).isEmpty());
        assertEquals(persistCount.get(), 0);
    }

    private DefaultMutableTreeNode rootWith(DefaultMutableTreeNode... children) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        for (DefaultMutableTreeNode child : children) {
            root.add(child);
        }
        return root;
    }

    private DefaultMutableTreeNode rootWithRequest(DefaultMutableTreeNode requestNode) {
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(new RequestGroup("Group"));
        groupNode.add(requestNode);
        return rootWith(groupNode);
    }
}
