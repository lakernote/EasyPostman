package com.laker.postman.service.render;

import com.laker.postman.model.HttpResponse;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HttpHtmlRendererTest {

    @Test(description = "响应 body 的 HTML code block 应消除 pre 默认外边距，避免 Swing 详情页首行错位")
    public void testRenderResponseShouldWrapPreWithZeroMargin() {
        HttpResponse response = new HttpResponse();
        response.code = 101;
        response.protocol = "http/1.1";
        response.body = "[18:04:01] message";

        String html = HttpHtmlRenderer.renderResponse(response);

        assertTrue(html.contains("<div style='background:"));
        assertTrue(html.contains("<pre style='margin:0;"));
    }

    @Test
    public void shouldNotRenderInternalPerformanceStreamHeaders() {
        HttpResponse response = new HttpResponse();
        response.code = 0;
        response.protocol = "-";
        response.headers = new LinkedHashMap<>();
        response.addHeader("Content-Type", List.of("text/plain"));
        response.addHeader("X-Easy-WS-Sent-Count", List.of("0"));
        response.addHeader("X-Easy-WS-Error", List.of("Resource temporarily unavailable"));

        String html = HttpHtmlRenderer.renderResponse(response);

        assertTrue(html.contains("Content-Type"));
        assertFalse(html.contains("X-Easy-WS-Sent-Count"));
        assertFalse(html.contains("X-Easy-WS-Error"));
    }

    @Test
    public void shouldRenderZeroStatusAsError() {
        HttpResponse response = new HttpResponse();
        response.code = 0;

        String html = HttpHtmlRenderer.renderResponse(response);

        assertTrue(html.contains("color:#d32f2f"));
    }
}
