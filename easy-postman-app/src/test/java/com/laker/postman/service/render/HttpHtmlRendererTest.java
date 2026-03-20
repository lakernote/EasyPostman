package com.laker.postman.service.render;

import com.laker.postman.model.HttpResponse;
import org.testng.annotations.Test;

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
}
