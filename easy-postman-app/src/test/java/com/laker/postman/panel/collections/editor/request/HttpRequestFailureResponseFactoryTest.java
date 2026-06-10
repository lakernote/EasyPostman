package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEvent;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import org.testng.annotations.Test;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class HttpRequestFailureResponseFactoryTest {

    @Test
    public void shouldBuildDisplayableFailureResponseForInterruptedRequest() {
        PreparedRequest request = new PreparedRequest();
        request.method = "GET";
        request.url = "https://example.test/slow";
        request.enableNetworkLog = true;
        List<NetworkLogEvent> events = new ArrayList<>();
        request.networkLogSink = events::add;

        HttpEventInfo eventInfo = new HttpEventInfo();
        eventInfo.setQueueStart(1_000L);
        eventInfo.setCallStart(1_010L);
        request.exchangeEventInfo = eventInfo;

        InterruptedIOException timeout = new InterruptedIOException("timeout");
        HttpResponse response = HttpRequestFailureResponseFactory.fromException(request, timeout, 1_000L, 1_250L);

        assertEquals(response.code, 0);
        assertEquals(response.body, "");
        assertEquals(response.bodySize, 0L);
        assertEquals(response.costMs, 250L);
        assertEquals(response.endTime, 1_250L);
        assertSame(response.httpEventInfo, eventInfo);
        assertEquals(eventInfo.getErrorMessage(), "timeout");
        assertSame(eventInfo.getError(), timeout);
        assertTrue(events.stream().anyMatch(event -> event.stage() == NetworkLogEventStage.FAILED
                && event.message().contains("GET https://example.test/slow")
                && event.message().contains("timeout")));
    }

    @Test
    public void shouldUseWorkerStartTimeWhenHttpEventInfoIsMissing() {
        PreparedRequest request = new PreparedRequest();
        request.method = "GET";
        request.url = "https://example.test/slow";

        InterruptedIOException timeout = new InterruptedIOException("timeout");
        HttpResponse response = HttpRequestFailureResponseFactory.fromException(request, timeout, 2_000L, 2_750L);

        assertEquals(response.body, "");
        assertEquals(response.bodySize, 0L);
        assertEquals(response.costMs, 750L);
        assertEquals(response.endTime, 2_750L);
        assertEquals(response.httpEventInfo.getQueueStart(), 2_000L);
        assertEquals(response.httpEventInfo.getCallFailed(), 2_750L);
        assertEquals(response.httpEventInfo.getErrorMessage(), "timeout");
    }
}
