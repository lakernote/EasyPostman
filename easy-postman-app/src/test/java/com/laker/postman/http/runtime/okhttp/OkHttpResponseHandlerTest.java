package com.laker.postman.http.runtime.okhttp;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class OkHttpResponseHandlerTest {

    @Test
    public void shouldReturnFirstContentDispositionHeaderIgnoringCase() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("content-disposition", List.of("attachment; filename=\"report.csv\""));
        headers.put("X-Test", List.of("ok"));

        assertEquals(OkHttpResponseHandler.getContentDisposition(headers),
                "attachment; filename=\"report.csv\"");
    }

    @Test
    public void shouldReturnNullWhenContentDispositionHeaderIsMissingOrEmpty() {
        Map<String, List<String>> missing = new LinkedHashMap<>();
        Map<String, List<String>> empty = new LinkedHashMap<>();
        empty.put("Content-Disposition", List.of());

        assertNull(OkHttpResponseHandler.getContentDisposition(missing));
        assertNull(OkHttpResponseHandler.getContentDisposition(empty));
    }

    @Test
    public void shouldParseQuotedFilenameFromContentDisposition() {
        String header = "attachment; filename=\"report.csv\"";

        assertEquals(OkHttpResponseHandler.parseFileNameFromContentDisposition(header), "report.csv");
    }

    @Test
    public void shouldParseUtf8FilenameStarFromContentDisposition() {
        String header = "attachment; filename*=UTF-8''%E6%B5%8B%E8%AF%95.txt";

        assertEquals(OkHttpResponseHandler.parseFileNameFromContentDisposition(header), "%E6%B5%8B%E8%AF%95.txt");
    }

    @Test
    public void shouldReturnNullWhenFilenameCannotBeParsed() {
        assertNull(OkHttpResponseHandler.parseFileNameFromContentDisposition("attachment"));
        assertNull(OkHttpResponseHandler.parseFileNameFromContentDisposition(null));
    }

    @Test
    public void shouldDiscardResponseBodyWhenMetadataOnlyModeIsUsed() throws Exception {
        String largeBody = "x".repeat(256 * 1024);
        HttpResponse response = new HttpResponse();

        OkHttpResponseHandler.handleResponse(
                responseWithBody(largeBody),
                response,
                null,
                PreparedRequest.ResponseBodyMode.METADATA_ONLY,
                1024
        );

        assertEquals(response.bodySize, largeBody.length());
        assertFalse(response.body.contains(largeBody.substring(0, 1024)));
        assertTrue(response.body.contains("skipped") || response.body.contains("跳过"), response.body);
    }

    @Test
    public void shouldKeepOnlyPreviewWhenPreviewModeIsUsed() throws Exception {
        String largeBody = "0123456789".repeat(2048);
        HttpResponse response = new HttpResponse();

        OkHttpResponseHandler.handleResponse(
                responseWithBody(largeBody),
                response,
                null,
                PreparedRequest.ResponseBodyMode.PREVIEW,
                128
        );

        assertEquals(response.bodySize, largeBody.length());
        assertTrue(response.body.startsWith("0123456789"), response.body);
        assertTrue(response.body.length() < largeBody.length() / 10, "Preview should not retain the full body");
        assertTrue(response.body.contains("truncated") || response.body.contains("截断"), response.body);
    }

    private static Response responseWithBody(String body) throws IOException {
        return new Response.Builder()
                .request(new Request.Builder().url("http://example.test/large").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", "text/plain; charset=utf-8")
                .body(ResponseBody.create(body, MediaType.get("text/plain; charset=utf-8")))
                .build();
    }
}
