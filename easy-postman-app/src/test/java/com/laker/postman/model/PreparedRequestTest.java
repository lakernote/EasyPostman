package com.laker.postman.model;

import com.laker.postman.http.trace.NetworkLogEvent;
import com.laker.postman.http.trace.NetworkLogSink;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertSame;

public class PreparedRequestTest {

    @Test
    public void shallowCopyShouldKeepInjectedNetworkLogSink() {
        AtomicReference<NetworkLogEvent> receivedEvent = new AtomicReference<>();
        NetworkLogSink sink = receivedEvent::set;
        PreparedRequest request = new PreparedRequest();
        request.networkLogSink = sink;

        PreparedRequest copy = request.shallowCopy();

        assertSame(copy.networkLogSink, sink,
                "Redirect working copies must keep the caller-injected network log sink");
    }
}
