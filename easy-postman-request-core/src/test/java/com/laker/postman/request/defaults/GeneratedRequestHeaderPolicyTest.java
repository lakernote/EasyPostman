package com.laker.postman.request.defaults;

import com.laker.postman.request.model.HttpHeader;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class GeneratedRequestHeaderPolicyTest {

    @Test
    public void shouldCreateStandardGeneratedHeadersInStableOrder() {
        GeneratedRequestHeaderPolicy policy = GeneratedRequestHeaderPolicy.standard("EasyPostman/test");

        List<HttpHeader> headers = policy.generatedHeaders();

        assertEquals(headers.size(), 4);
        assertEquals(headers.get(0).getKey(), HttpRequestDefaults.USER_AGENT);
        assertEquals(headers.get(0).getValue(), "EasyPostman/test");
        assertEquals(headers.get(1).getKey(), HttpRequestDefaults.ACCEPT);
        assertEquals(headers.get(2).getKey(), HttpRequestDefaults.ACCEPT_ENCODING);
        assertEquals(headers.get(3).getKey(), HttpRequestDefaults.CONNECTION);
    }

    @Test
    public void shouldApplyGeneratedHeadersBeforeCustomHeadersAndPreserveOverrides() {
        GeneratedRequestHeaderPolicy policy = GeneratedRequestHeaderPolicy.standard("EasyPostman/test");
        List<HttpHeader> merged = policy.applyDefaults(List.of(
                new HttpHeader(true, "X-Trace", "1"),
                new HttpHeader(false, "accept", "application/json")
        ));

        assertEquals(merged.size(), 5);
        assertEquals(merged.get(0).getKey(), HttpRequestDefaults.USER_AGENT);
        assertEquals(merged.get(1).getKey(), "accept");
        assertEquals(merged.get(1).getValue(), "application/json");
        assertEquals(merged.get(1).isEnabled(), false);
        assertEquals(merged.get(4).getKey(), "X-Trace");
    }

    @Test
    public void shouldMatchGeneratedHeaderKeysIgnoringCase() {
        GeneratedRequestHeaderPolicy policy = GeneratedRequestHeaderPolicy.standard("EasyPostman/test");

        assertTrue(policy.isGeneratedHeaderKey("user-agent"));
        assertTrue(policy.isGeneratedHeaderKey(" ACCEPT-ENCODING "));
    }
}
