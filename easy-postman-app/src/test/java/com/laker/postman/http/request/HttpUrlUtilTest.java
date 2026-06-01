package com.laker.postman.http.request;

import com.laker.postman.request.util.HttpUrlUtil;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HttpUrlUtilTest {

    @Test
    public void shouldDecodeOnlyQueryPartForDisplay() {
        String displayUrl = HttpUrlUtil.decodeQueryForDisplay(
                "https://example.com/a%2Fb?filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87&lang=en#frag"
        );

        assertEquals(
                displayUrl,
                "https://example.com/a%2Fb?filter=测试中文&lang=en#frag"
        );
    }

    @Test
    public void shouldKeepPlusSignWhenDecodingComponent() {
        assertEquals(HttpUrlUtil.decodeComponent("a+b%20c"), "a+b c");
    }
}
