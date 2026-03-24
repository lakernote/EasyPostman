package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class RequestCollectionsServiceTest {

    @Test
    public void shouldBuildRestorableRequestsInOriginalOrder() {
        HttpRequestItem persistedRequest = request("persisted", "Persisted");
        HttpRequestItem treeResolvedRequest = request("persisted", "Resolved");
        HttpRequestItem missingRequest = request("missing", "Missing");
        HttpRequestItem newRequest = request("new", "");

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        rootNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, treeResolvedRequest}));

        List<HttpRequestItem> restorable = RequestCollectionsService.buildRestorableOpenedRequests(
                List.of(persistedRequest, missingRequest, newRequest),
                rootNode
        );

        assertEquals(restorable.size(), 2);
        assertSame(restorable.get(0), treeResolvedRequest);
        assertSame(restorable.get(1), newRequest);
    }

    @Test
    public void shouldRestoreRequestsIncrementallyAndOnlySelectFinalTab() throws Exception {
        List<HttpRequestItem> requests = List.of(
                request("1", "one"),
                request("2", "two"),
                request("3", "three")
        );
        List<String> restoredIds = new ArrayList<>();
        List<Boolean> selectedFlags = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> RequestCollectionsService.restoreOpenedRequestsIncrementally(
                requests,
                1,
                1,
                0,
                (item, selected) -> {
                    restoredIds.add(item.getId());
                    selectedFlags.add(selected);
                },
                () -> {
                    completed.set(true);
                    latch.countDown();
                }
        ));

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(completed.get());
        assertEquals(restoredIds, List.of("1", "2", "3"));
        assertEquals(selectedFlags, List.of(false, false, true));
    }

    private HttpRequestItem request(String id, String name) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        return item;
    }
}
