package com.laker.postman.request.edit;

import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class HttpRequestSaveMergerTest {

    @Test
    public void shouldPreservePersistedNameAndResponsesWhenSavingExistingRequest() {
        SavedResponse response = new SavedResponse();
        response.setId("response-1");
        HttpRequestItem persisted = new HttpRequestItem();
        persisted.setId("request-1");
        persisted.setName("Persisted name");
        persisted.setResponse(List.of(response));

        HttpRequestItem edited = new HttpRequestItem();
        edited.setId("request-1");
        edited.setName("Edited tab title");
        edited.setUrl("https://api.example.com");

        HttpRequestItem merged = HttpRequestSaveMerger.mergeExisting(persisted, edited);

        assertSame(merged, edited);
        assertEquals(merged.getName(), "Persisted name");
        assertSame(merged.getResponse(), persisted.getResponse());
    }

    @Test
    public void shouldLeaveEmptyResponsesEmpty() {
        HttpRequestItem persisted = new HttpRequestItem();
        persisted.setName("Persisted");
        persisted.setResponse(List.of());

        HttpRequestItem edited = new HttpRequestItem();
        edited.setName("Edited");

        HttpRequestItem merged = HttpRequestSaveMerger.mergeExisting(persisted, edited);

        assertEquals(merged.getName(), "Persisted");
        assertTrue(merged.getResponse().isEmpty());
    }
}
