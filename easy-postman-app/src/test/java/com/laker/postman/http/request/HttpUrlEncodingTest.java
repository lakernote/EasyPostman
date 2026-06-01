package com.laker.postman.http.request;

import com.laker.postman.request.util.HttpUrlUtil;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class HttpUrlEncodingTest {

    @Test
    public void shouldEncodeUnicodeQueryParameterValues() {
        String url = "https://httpbin.org/get?q=easytools&lang=en&page=1&size=10&sort=desc&filter=测试中文";

        String encoded = HttpUrlUtil.buildEncodedUrl(url, List.of());

        assertEquals(
                encoded,
                "https://httpbin.org/get?q=easytools&lang=en&page=1&size=10&sort=desc&filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87"
        );
    }

    @Test
    public void shouldNotDoubleEncodeExistingEncodedUnicodeQueryValues() {
        String url = "https://httpbin.org/get?filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87";

        String encoded = HttpUrlUtil.buildEncodedUrl(url, List.of());

        assertEquals(
                encoded,
                "https://httpbin.org/get?filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87"
        );
    }

    @Test
    public void shouldEncodeUnicodeParamsFromParamsList() {
        String encoded = HttpUrlUtil.buildEncodedUrl(
                "https://httpbin.org/get?q=easytools",
                List.of(new com.laker.postman.request.model.HttpParam(true, "filter", "测试中文"))
        );

        assertEquals(
                encoded,
                "https://httpbin.org/get?q=easytools&filter=%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87"
        );
    }
}
