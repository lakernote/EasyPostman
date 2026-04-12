package com.laker.postman.service.http.okhttp;

import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

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
}
