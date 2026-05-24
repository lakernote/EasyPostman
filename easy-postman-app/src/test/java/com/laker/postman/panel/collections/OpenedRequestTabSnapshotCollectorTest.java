package com.laker.postman.panel.collections;

import com.laker.postman.model.HttpRequestItem;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class OpenedRequestTabSnapshotCollectorTest {

    @Test
    public void shouldKeepMostRecentOpenedRequestsWhenOverLimit() {
        List<HttpRequestItem> items = List.of(
                request("first"),
                request("second"),
                request("third"),
                request("fourth")
        );

        List<HttpRequestItem> limited = OpenedRequestTabSnapshotCollector.limitToMostRecent(items, 2);

        assertEquals(limited.stream().map(HttpRequestItem::getId).toList(), List.of("third", "fourth"));
    }

    @Test
    public void shouldReturnEmptySnapshotWhenLimitIsZero() {
        List<HttpRequestItem> limited = OpenedRequestTabSnapshotCollector.limitToMostRecent(
                List.of(request("first")),
                0
        );

        assertTrue(limited.isEmpty());
    }

    private static HttpRequestItem request(String id) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(id);
        return item;
    }
}
