package com.laker.postman.http.runtime.model;

import com.laker.postman.http.runtime.observation.NetworkLogEvent;
import com.laker.postman.http.runtime.observation.NetworkLogSink;
import com.laker.postman.request.model.HttpHeader;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertNotSame;
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

    @Test
    public void shallowCopyShouldOwnMutableRequestLists() {
        PreparedRequest request = new PreparedRequest();
        request.headersList = new ArrayList<>(List.of(header("x-one", "1")));

        PreparedRequest copy = request.shallowCopy();
        copy.headersList.add(header("x-two", "2"));

        assertNotSame(copy.headersList, request.headersList);
        org.testng.Assert.assertEquals(request.headersList.size(), 1);
        org.testng.Assert.assertEquals(copy.headersList.size(), 2);
    }

    private static HttpHeader header(String key, String value) {
        HttpHeader header = new HttpHeader();
        header.setKey(key);
        header.setValue(value);
        return header;
    }
}
