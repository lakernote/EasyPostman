package com.laker.postman.service.render;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
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
                ThemeColors.SURFACE,
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
        UIManager.put(ThemeColors.SURFACE, new Color(10, 11, 12));
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

    @Test
    public void shouldRenderKeyValueRowsWithoutFullWidthBackgroundBands() {
        PreparedRequest request = new PreparedRequest();
        request.url = "https://example.test/api";
        request.method = "POST";
        request.headersList = List.of(new HttpHeader(true, "Accept", "*/*"));

        String html = HttpHtmlRenderer.renderRequest(request);

        assertTrue(html.contains("line-height:1.35"));
        assertFalse(html.contains("border-bottom:1px solid"));
        assertFalse(html.contains("padding:3px 8px;background:"));
    }

    @Test
    public void shouldRenderTimelineWithThemeAwareSurfaceAndTrackColors() {
        UIManager.put(ThemeColors.SURFACE, new Color(10, 11, 12));
        UIManager.put(ThemeColors.HOVER_BACKGROUND, new Color(13, 14, 15));
        UIManager.put(ThemeColors.BORDER_LIGHT, new Color(16, 17, 18));

        HttpEventInfo eventInfo = new HttpEventInfo();
        eventInfo.setQueueStart(990L);
        eventInfo.setCallStart(1_000L);
        eventInfo.setConnectStart(1_010L);
        eventInfo.setConnectEnd(1_030L);
        eventInfo.setRequestHeadersStart(1_030L);
        eventInfo.setRequestHeadersEnd(1_040L);
        eventInfo.setResponseHeadersStart(1_080L);
        eventInfo.setResponseBodyStart(1_085L);
        eventInfo.setResponseBodyEnd(1_100L);
        eventInfo.setCallEnd(1_100L);

        HttpResponse response = new HttpResponse();
        response.httpEventInfo = eventInfo;

        String html = HttpHtmlRenderer.renderTimingInfo(response);

        assertTrue(html.contains("Timeline"));
        assertTrue(html.contains("background:#0a0b0c"));
        assertTrue(html.contains("background:#0d0e0f"));
        assertTrue(html.contains("border-bottom:1px solid"));
        assertTrue(html.contains("border-top:1px solid"));
        assertFalse(html.contains("border-bottom:1px solid #101112"));
        assertFalse(html.contains("background:#101112;height:8px"));
    }

    @Test
    public void shouldRenderConfiguredRequestWhenSentSnapshotMissing() {
        PreparedRequest request = new PreparedRequest();
        request.url = "https://example.test/api";
        request.method = "POST";
        request.headersList = List.of(
                new HttpHeader(true, "Cookie", "cf_clearance=clearance"),
                new HttpHeader(false, "Disabled", "hidden")
        );
        request.body = "{\"action\":\"next\"}";

        String html = HttpHtmlRenderer.renderRequest(request);

        assertTrue(html.contains("Configured Headers"));
        assertTrue(html.contains("Cookie"));
        assertTrue(html.contains("cf_clearance=clearance"));
        assertFalse(html.contains("Disabled"));
        assertTrue(html.contains("Configured Body"));
        assertTrue(html.contains("{&quot;action&quot;:&quot;next&quot;}"));
    }

    @Test
    public void shouldPreferSentRequestSnapshotInRequestDetails() {
        PreparedRequest request = new PreparedRequest();
        request.url = "https://example.test/api";
        request.method = "POST";
        request.headersList = List.of(new HttpHeader(true, "Cookie", "configured-cookie"));
        request.body = "configured body";
        request.sentHeadersList = List.of(new HttpHeader(true, "Cookie", "sent-cookie"));
        request.sentRequestBody = "sent body";

        String html = HttpHtmlRenderer.renderRequest(request);

        assertTrue(html.contains("Sent Headers"));
        assertTrue(html.contains("sent-cookie"));
        assertFalse(html.contains("configured-cookie"));
        assertTrue(html.contains("Sent Body"));
        assertTrue(html.contains("sent body"));
        assertFalse(html.contains("configured body"));
    }

    @Test
    public void shouldKeepRequestBodyPreviewSmall() {
        String body = "x".repeat(3 * 1024);
        PreparedRequest request = new PreparedRequest();
        request.url = "https://example.test/api";
        request.method = "POST";
        request.sentRequestBody = body;

        String html = HttpHtmlRenderer.renderRequest(request);

        assertFalse(html.contains(body));
        assertTrue(html.contains("Truncated"));
        assertTrue(html.contains("showing first 2KB"));
    }

    @Test
    public void shouldNotShowConfiguredBodyWhenSentSnapshotHasNoBody() {
        PreparedRequest request = new PreparedRequest();
        request.url = "wss://example.test/socket";
        request.method = "GET";
        request.body = "configured websocket message";
        request.sentHeadersList = List.of(new HttpHeader(true, "Host", "example.test"));

        String html = HttpHtmlRenderer.renderRequest(request);

        assertTrue(html.contains("Sent Headers"));
        assertFalse(html.contains("Configured Body"));
        assertFalse(html.contains("configured websocket message"));
    }

    @Test
    public void shouldRenderThreadFromEventInfoWhenResponseThreadMissing() {
        HttpEventInfo eventInfo = new HttpEventInfo();
        eventInfo.setThreadName("SwingWorker-pool-3-thread-1");
        eventInfo.setLocalAddress("127.0.0.1:51000");
        eventInfo.setRemoteAddress("127.0.0.1:8080");

        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.protocol = "http/1.1";
        response.body = "";
        response.httpEventInfo = eventInfo;

        String html = HttpHtmlRenderer.renderResponse(response);

        assertTrue(html.contains("SwingWorker-pool-3-thread-1"));
        assertTrue(html.contains("127.0.0.1:51000"));
        assertTrue(html.contains("127.0.0.1:8080"));
    }

    @Test
    public void shouldRenderNetworkErrorMessageInResponseDetails() {
        HttpEventInfo eventInfo = new HttpEventInfo();
        eventInfo.setErrorMessage("timeout");

        HttpResponse response = new HttpResponse();
        response.code = 0;
        response.body = "";
        response.httpEventInfo = eventInfo;

        String html = HttpHtmlRenderer.renderResponse(response);

        assertTrue(html.contains("Network Error"));
        assertTrue(html.contains("timeout"));
    }
}
