package com.laker.postman.service.render;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.http.runtime.model.HttpResponse;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HttpHtmlRendererTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.PRIMARY,
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_HINT,
                ThemeColors.BACKGROUND,
                ThemeColors.HOVER_BACKGROUND,
                ThemeColors.BORDER_LIGHT,
                ThemeColors.ERROR
        );
    }

    @AfterMethod
    public void restoreThemeTokens() {
        restore(previousThemeTokens);
    }

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
        UIManager.put(ThemeColors.ERROR, new Color(31, 32, 33));
        HttpResponse response = new HttpResponse();
        response.code = 0;

        String html = HttpHtmlRenderer.renderResponse(response);

        assertTrue(html.contains("color:#1f2021"));
    }

    @Test
    public void shouldRenderHtmlWithSemanticThemeTokens() {
        UIManager.put(ThemeColors.PRIMARY, new Color(1, 2, 3));
        UIManager.put(ThemeColors.TEXT_PRIMARY, new Color(4, 5, 6));
        UIManager.put(ThemeColors.TEXT_HINT, new Color(7, 8, 9));
        UIManager.put(ThemeColors.BACKGROUND, new Color(10, 11, 12));
        UIManager.put(ThemeColors.HOVER_BACKGROUND, new Color(13, 14, 15));
        UIManager.put(ThemeColors.BORDER_LIGHT, new Color(16, 17, 18));
        UIManager.put(ThemeColors.ERROR, new Color(19, 20, 21));

        HttpResponse response = new HttpResponse();
        response.code = 0;
        response.protocol = "http/1.1";
        response.body = "payload";

        String html = HttpHtmlRenderer.renderResponse(response);

        assertTrue(html.contains("color:#040506"));
        assertTrue(html.contains("color:#010203"));
        assertTrue(html.contains("color:#070809"));
        assertTrue(html.contains("background:#0a0b0c"));
        assertTrue(html.contains("background:#0d0e0f"));
        assertTrue(html.contains("border:1px solid #131415"));
    }
}
