package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertNull;

public class CollectionTreeQueryServiceTest {

    @Test
    public void shouldFindLastPersistedRequest() {
        HttpRequestItem first = request("first", "First");
        HttpRequestItem second = request("second", "Second");
        HttpRequestItem unsaved = request("unsaved", "");

        HttpRequestItem result = CollectionTreeQueryService.findLastPersistedRequest(
                List.of(first, second, unsaved)
        );

        assertSame(result, second);
    }

    @Test
    public void shouldReturnNullWhenNoPersistedRequestExists() {
        assertNull(CollectionTreeQueryService.findLastPersistedRequest(List.of(request("unsaved", ""))));
        assertNull(CollectionTreeQueryService.findLastPersistedRequest(List.of()));
        assertNull(CollectionTreeQueryService.findLastPersistedRequest(null));
    }

    private HttpRequestItem request(String id, String name) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        return item;
    }
}
