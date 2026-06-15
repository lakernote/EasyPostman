package com.laker.postman.http.execution;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEvent;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import com.laker.postman.request.model.RequestBodyTypes;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RequestPreparationNetworkLogPublisherTest {

    @Test
    public void shouldPublishPreparedRequestSummaryToNetworkLog() {
        PreparedRequest request = new PreparedRequest();
        request.method = "POST";
        request.url = "https://example.test/api?debug=true";
        request.bodyType = RequestBodyTypes.BODY_TYPE_RAW;
        request.headersList = new ArrayList<>();
        request.headersList.add(new HttpHeader(true, "Authorization", "Bearer token"));
        request.headersList.add(new HttpHeader(false, "X-Disabled", "no"));
        request.paramsList = new ArrayList<>();
        request.paramsList.add(new HttpParam(true, "debug", "true", ""));
        request.prescript = "pm.environment.set('token', 'abc')";
        request.postscript = "";
        request.followRedirects = true;
        request.cookieJarEnabled = true;
        request.proxyPolicy = HttpRequestProxyPolicy.DEFAULT;
        request.sslVerificationEnabled = false;
        request.httpVersion = "Auto";
        request.requestTimeoutMs = 5_000;
        request.enableNetworkLog = true;
        List<NetworkLogEvent> events = new ArrayList<>();
        request.networkLogSink = events::add;

        RequestPreparationNetworkLogPublisher.publish(request);

        assertEquals(events.size(), 1);
        NetworkLogEvent event = events.get(0);
        assertEquals(event.stage(), NetworkLogEventStage.REQUEST_PREPARED);
        assertTrue(event.message().contains("Method: POST"));
        assertTrue(event.message().contains("URL: https://example.test/api?debug=true"));
        assertTrue(event.message().contains("Body Type: raw"));
        assertTrue(event.message().contains("Headers: 1 enabled / 2 total"));
        assertTrue(event.message().contains("Header Names: Authorization, X-Disabled"));
        assertTrue(event.message().contains("Query Params: 1 enabled / 1 total"));
        assertTrue(event.message().contains("Pre-script: present"));
        assertTrue(event.message().contains("Post-script: none"));
        assertTrue(event.message().contains("Redirects: enabled"));
        assertTrue(event.message().contains("Cookie Jar: enabled"));
        assertTrue(event.message().contains("Proxy Policy: DEFAULT"));
        assertTrue(event.message().contains("SSL Verification: disabled"));
        assertTrue(event.message().contains("HTTP Version: Auto"));
        assertTrue(event.message().contains("Timeout: 5000ms"));
    }
}
