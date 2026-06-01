package com.laker.postman.service.collections;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.SavedResponse;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class SavedResponseSnapshotMapperTest {

    @Test
    public void shouldCreateSavedResponseSnapshotFromRuntimeExchange() {
        PreparedRequest request = new PreparedRequest();
        request.method = "POST";
        request.url = "https://api.example.com/users";
        request.bodyType = "raw";
        request.body = "{\"name\":\"easy\"}";
        request.headersList = List.of(new HttpHeader(true, "Content-Type", "application/json"));
        request.paramsList = List.of(new HttpParam(true, "verbose", "true"));

        HttpResponse response = new HttpResponse();
        response.code = 201;
        response.body = "{\"id\":1}";
        response.costMs = 123;
        response.bodySize = 8;
        response.headersSize = 42;
        response.headers = new LinkedHashMap<>();
        response.headers.put("Content-Type", List.of("application/json"));
        response.headers.put("X-Trace", List.of("a", "b"));

        SavedResponse savedResponse = SavedResponseSnapshotMapper.fromExchange("Created", request, response);

        assertNotNull(savedResponse.getId());
        assertEquals(savedResponse.getName(), "Created");
        assertEquals(savedResponse.getOriginalRequest().getMethod(), "POST");
        assertEquals(savedResponse.getOriginalRequest().getHeaders().size(), 1);
        assertEquals(savedResponse.getOriginalRequest().getParams().size(), 1);
        assertEquals(savedResponse.getCode(), 201);
        assertEquals(savedResponse.getHeaders().size(), 2);
        assertEquals(savedResponse.getHeaders().get(1).getValue(), "a, b");
        assertEquals(savedResponse.getPreviewLanguage(), "json");
    }

    @Test
    public void shouldCreateRuntimeResponseFromSavedResponse() {
        SavedResponse savedResponse = new SavedResponse();
        savedResponse.setCode(200);
        savedResponse.setBody("<html></html>");
        savedResponse.setHeaders(List.of(new HttpHeader(true, "Content-Type", "text/html")));
        savedResponse.setCostMs(88);
        savedResponse.setBodySize(13);
        savedResponse.setHeadersSize(20);

        HttpResponse response = SavedResponseSnapshotMapper.toRuntimeResponse(savedResponse);

        assertEquals(response.code, 200);
        assertEquals(response.body, "<html></html>");
        assertEquals(response.headers.get("Content-Type"), List.of("text/html"));
        assertEquals(response.costMs, 88);
        assertEquals(response.bodySize, 13);
        assertEquals(response.headersSize, 20);
    }
}
