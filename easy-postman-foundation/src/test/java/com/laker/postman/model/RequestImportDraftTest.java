package com.laker.postman.model;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class RequestImportDraftTest {

    @Test
    public void shouldNormalizeDefaultsAndProtectHeaders() {
        List<RequestImportHeader> headers = new ArrayList<>();
        headers.add(new RequestImportHeader(true, "Accept", "application/json"));

        RequestImportDraft draft = new RequestImportDraft(
                null,
                null,
                null,
                null,
                null,
                headers,
                null,
                null,
                null
        );
        headers.add(new RequestImportHeader(true, "X-Late", "ignored"));

        assertEquals(draft.id(), "");
        assertEquals(draft.name(), "");
        assertEquals(draft.url(), "");
        assertEquals(draft.method(), "GET");
        assertEquals(draft.protocol(), RequestImportProtocol.HTTP);
        assertEquals(draft.bodyType(), RequestImportBodyTypes.NONE);
        assertEquals(draft.body(), "");
        assertEquals(draft.headers().size(), 1);
        assertThrows(UnsupportedOperationException.class,
                () -> draft.headers().add(new RequestImportHeader(true, "X-Edit", "blocked")));
    }
}
