package com.laker.postman.collection;

import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.CollectionRequestContext;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CollectionTreeQueriesTest {

    @Test
    public void shouldFindRequestWithGroupChain() {
        RequestGroup outer = new RequestGroup("Outer");
        RequestGroup inner = new RequestGroup("Inner");
        HttpRequestItem request = new HttpRequestItem();
        request.setId("req-1");
        request.setName("Get Orders");

        CollectionNode root = CollectionNode.group(outer);
        CollectionNode folder = CollectionNode.group(inner);
        folder.addChild(CollectionNode.request(request));
        root.addChild(folder);

        Optional<CollectionRequestContext> context = CollectionTreeQueries.findRequestContextById(
                new CollectionDocument(List.of(root)),
                "req-1"
        );

        assertTrue(context.isPresent());
        assertEquals(context.get().getRequest().getName(), "Get Orders");
        assertEquals(context.get().getGroupChain().stream().map(RequestGroup::getName).toList(), List.of("Outer", "Inner"));
    }

    @Test
    public void shouldCollectRequestsInTraversalOrder() {
        HttpRequestItem first = request("req-1");
        HttpRequestItem second = request("req-2");
        CollectionNode root = CollectionNode.group(new RequestGroup("Root"));
        root.addChild(CollectionNode.request(first));
        root.addChild(CollectionNode.request(second));

        List<HttpRequestItem> requests = CollectionTreeQueries.collectRequests(new CollectionDocument(List.of(root)));

        assertEquals(requests.stream().map(HttpRequestItem::getId).toList(), List.of("req-1", "req-2"));
    }

    private static HttpRequestItem request(String id) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        return item;
    }
}
