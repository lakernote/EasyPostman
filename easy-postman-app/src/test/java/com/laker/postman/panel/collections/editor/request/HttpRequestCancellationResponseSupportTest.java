package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEvent;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class HttpRequestCancellationResponseSupportTest {

    @Test
    public void shouldCreateCancellationResponseWhenWorkerFinishesBeforeBackgroundCatch() {
        PreparedRequest request = new PreparedRequest();
        request.method = "GET";
        request.url = "https://example.test/waiting";
        request.enableNetworkLog = true;
        List<NetworkLogEvent> events = new ArrayList<>();
        request.networkLogSink = events::add;
        events.add(new NetworkLogEvent(NetworkLogEventStage.DNS_START, "dnsStart example.test", 12L));

        HttpResponse response = HttpRequestCancellationResponseSupport.resolveTerminalResponse(
                request, null, true, 1_000L, 1_150L);

        assertNotNull(response);
        assertEquals(response.code, 0);
        assertEquals(response.costMs, 150L);
        assertEquals(events.get(0).stage(), NetworkLogEventStage.DNS_START);
        assertTrue(events.stream().anyMatch(event -> event.stage() == NetworkLogEventStage.CANCELED));
    }

    @Test
    public void shouldKeepExistingResponse() {
        PreparedRequest request = new PreparedRequest();
        HttpResponse existing = new HttpResponse();

        HttpResponse response = HttpRequestCancellationResponseSupport.resolveTerminalResponse(
                request, existing, true, 1_000L, 1_150L);

        assertSame(response, existing);
    }

    @Test
    public void shouldKeepNullWhenWorkerWasNotCanceled() {
        PreparedRequest request = new PreparedRequest();

        HttpResponse response = HttpRequestCancellationResponseSupport.resolveTerminalResponse(
                request, null, false, 1_000L, 1_150L);

        assertNull(response);
    }
}
