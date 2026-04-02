package com.laker.postman.service.http;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class HttpRequestUtilTest {

    @Test
    public void shouldEncodeUnicodeQueryParameterValues() {
        String url = "https://httpbin.org/get?q=easytools&lang=en&page=1&size=10&sort=desc&filter=测试中文";

        String encoded = HttpRequestUtil.buildEncodedUrl(url, List.of());

        assertEquals(
                encoded,
                "https://httpbin.org/get?q=easytools&lang=en&page=1&size=10&sort=desc&filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87"
        );
    }

    @Test
    public void shouldNotDoubleEncodeExistingEncodedUnicodeQueryValues() {
        String url = "https://httpbin.org/get?filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87";

        String encoded = HttpRequestUtil.buildEncodedUrl(url, List.of());

        assertEquals(
                encoded,
                "https://httpbin.org/get?filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87"
        );
    }

    @Test
    public void shouldEncodeUnicodeParamsFromParamsList() {
        String encoded = HttpRequestUtil.buildEncodedUrl(
                "https://httpbin.org/get?q=easytools",
                List.of(new com.laker.postman.model.HttpParam(true, "filter", "测试中文"))
        );

        assertEquals(
                encoded,
                "https://httpbin.org/get?q=easytools&filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87"
        );
    }

    @Test
    public void shouldDecodeOnlyQueryPartForDisplay() {
        String displayUrl = HttpUtil.decodeUrlQueryForDisplay(
                "https://example.com/a%2Fb?filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87&lang=en#frag"
        );

        assertEquals(
                displayUrl,
                "https://example.com/a%2Fb?filter=测试中文&lang=en#frag"
        );
    }

    @Test
    public void shouldKeepPlusSignWhenDecodingComponent() {
        assertEquals(HttpUtil.decodeURIComponent("a+b%20c"), "a+b c");
    }
}
