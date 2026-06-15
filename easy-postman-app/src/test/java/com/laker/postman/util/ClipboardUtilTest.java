package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class ClipboardUtilTest {

    @Test
    public void extractCurlTextShouldUseLightweightCurlCommandPrefix() {
        assertEquals(ClipboardUtil.extractCurlText("  curl https://example.com\n"), "curl https://example.com");
        assertEquals(ClipboardUtil.extractCurlText("CURL https://example.com"), "CURL https://example.com");
        assertNull(ClipboardUtil.extractCurlText("curlx https://example.com"));
        assertNull(ClipboardUtil.extractCurlText("echo curl https://example.com"));
        assertNull(ClipboardUtil.extractCurlText(null));
    }
}
