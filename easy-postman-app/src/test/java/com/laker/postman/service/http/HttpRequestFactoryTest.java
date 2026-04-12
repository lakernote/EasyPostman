package com.laker.postman.service.http;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class HttpRequestFactoryTest {

    @Test
    public void shouldUseVersionedUserAgentInDefaultRequest() {
        HttpRequestItem request = HttpRequestFactory.createDefaultRequest();

        assertNotNull(request.getHeadersList());
        assertEquals(findHeaderValues(request.getHeadersList(), HttpRequestFactory.USER_AGENT),
                List.of(HttpRequestFactory.EASY_POSTMAN_CLIENT));
        assertTrue(StandardCharsets.US_ASCII.newEncoder().canEncode(HttpRequestFactory.EASY_POSTMAN_CLIENT));
    }

    @Test
    public void shouldIncludeExpectedDefaultHeadersOnce() {
        HttpRequestItem request = HttpRequestFactory.createDefaultRequest();

        assertEquals(findHeaderValues(request.getHeadersList(), HttpRequestFactory.USER_AGENT).size(), 1);
        assertEquals(findHeaderValues(request.getHeadersList(), HttpRequestFactory.ACCEPT),
                List.of("*/*"));
        assertEquals(findHeaderValues(request.getHeadersList(), HttpRequestFactory.ACCEPT_ENCODING),
                List.of(HttpRequestFactory.ACCEPT_ENCODING_VALUE));
        assertEquals(findHeaderValues(request.getHeadersList(), HttpRequestFactory.CONNECTION),
                List.of(HttpRequestFactory.CONNECTION_VALUE));
    }

    private List<String> findHeaderValues(List<HttpHeader> headers, String key) {
        return headers.stream()
                .filter(HttpHeader::isEnabled)
                .filter(header -> key.equalsIgnoreCase(header.getKey()))
                .map(HttpHeader::getValue)
                .toList();
    }
}
