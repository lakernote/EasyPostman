package com.laker.postman.service;

import cn.hutool.json.JSONObject;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestHistoryItem;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class HistoryPersistenceServiceTest {

    @Test
    public void shouldTruncateLargeRequestBodyBeforePersistingHistory() throws Exception {
        HistoryPersistenceService service = new HistoryPersistenceService();
        String largeBody = "x".repeat(12 * 1024);

        PreparedRequest request = new PreparedRequest();
        request.method = "POST";
        request.url = "https://example.com/api/test";
        request.body = largeBody;
        request.okHttpRequestBody = largeBody;

        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.body = "{\"ok\":true}";

        RequestHistoryItem item = new RequestHistoryItem(request, response, 123456789L);

        JSONObject json = invokeConvertToJson(service, item);
        RequestHistoryItem restored = invokeConvertFromJson(service, json);

        assertNotNull(restored.request);
        assertTrue(restored.request.body.length() < largeBody.length());
        assertEquals(restored.request.body, restored.request.okHttpRequestBody);
        assertTrue(restored.request.body.contains("内容过大，已截断"));
    }

    private JSONObject invokeConvertToJson(HistoryPersistenceService service, RequestHistoryItem item) throws Exception {
        Method method = HistoryPersistenceService.class.getDeclaredMethod("convertToJson", RequestHistoryItem.class);
        method.setAccessible(true);
        return (JSONObject) method.invoke(service, item);
    }

    private RequestHistoryItem invokeConvertFromJson(HistoryPersistenceService service, JSONObject json) throws Exception {
        Method method = HistoryPersistenceService.class.getDeclaredMethod("convertFromJson", JSONObject.class);
        method.setAccessible(true);
        return (RequestHistoryItem) method.invoke(service, json);
    }
}
