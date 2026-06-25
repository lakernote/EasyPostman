package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CaptureSessionStoreTest {

    @Test
    public void shouldThrottleRepeatedTlsIssueRowsForSameHost() {
        CaptureSessionStore store = new CaptureSessionStore();

        store.recordTlsIssue("api.apple-cloudkit.com", 443, "first");
        store.recordTlsIssue("api.apple-cloudkit.com", 443, "second");

        assertEquals(store.snapshot().size(), 1);
        assertEquals(store.snapshot().get(0).method(), "TLS");
        assertEquals(store.snapshot().get(0).statusCode(), 495);
    }
}
