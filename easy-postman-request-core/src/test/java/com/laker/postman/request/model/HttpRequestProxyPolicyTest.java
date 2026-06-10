package com.laker.postman.request.model;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HttpRequestProxyPolicyTest {

    @Test
    public void shouldNormalizeStoredStringValuesToEnum() {
        assertEquals(HttpRequestProxyPolicy.normalize("use_proxy"), HttpRequestProxyPolicy.USE_PROXY);
        assertEquals(HttpRequestProxyPolicy.normalize("NO_PROXY"), HttpRequestProxyPolicy.NO_PROXY);
        assertEquals(HttpRequestProxyPolicy.normalize("invalid"), HttpRequestProxyPolicy.DEFAULT);
    }
}
