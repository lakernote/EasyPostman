package com.laker.postman.http.runtime.error;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEvent;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class HttpFailureResponseFactoryTest {

    @Test
    public void shouldBuildDisplayableFailureResponseForPlainIOException() {
        PreparedRequest request = new PreparedRequest();
        request.method = "POST";
        request.url = "https://example.test/api";
        request.enableNetworkLog = true;
        List<NetworkLogEvent> events = new ArrayList<>();
        request.networkLogSink = events::add;

        HttpEventInfo eventInfo = new HttpEventInfo();
        eventInfo.setQueueStart(1_000L);
        eventInfo.setCallStart(1_010L);
        eventInfo.setProtocol("HTTP/1.1");
        request.exchangeEventInfo = eventInfo;

        IOException exception = new IOException("unexpected end of stream on https://example.test/...");
        HttpResponse response = HttpFailureResponseFactory.fromException(request, exception, 1_000L, 1_250L);

        assertEquals(response.code, 0);
        assertEquals(response.body, "");
        assertEquals(response.bodySize, 0L);
        assertEquals(response.costMs, 250L);
        assertEquals(response.endTime, 1_250L);
        assertSame(response.httpEventInfo, eventInfo);
        assertEquals(response.protocol, "HTTP/1.1");
        assertSame(eventInfo.getError(), exception);
        assertTrue(eventInfo.getErrorMessage().contains("unexpected end of stream"));
        assertEquals(eventInfo.getCallFailed(), 1_250L);
        assertTrue(events.stream().anyMatch(event -> event.stage() == NetworkLogEventStage.FAILED
                && event.message().contains("POST https://example.test/api")
                && event.message().contains("unexpected end of stream")));
    }

    @Test
    public void shouldUseRequestStartWhenEventInfoIsMissing() {
        PreparedRequest request = new PreparedRequest();
        request.method = "GET";
        request.url = "https://example.test/slow";

        InterruptedIOException exception = new InterruptedIOException("timeout");
        HttpResponse response = HttpFailureResponseFactory.fromException(request, exception, 2_000L, 2_750L);

        assertEquals(response.code, 0);
        assertEquals(response.costMs, 750L);
        assertEquals(response.httpEventInfo.getQueueStart(), 2_000L);
        assertEquals(response.httpEventInfo.getCallFailed(), 2_750L);
        assertEquals(response.httpEventInfo.getErrorMessage(), "timeout");
    }
}
