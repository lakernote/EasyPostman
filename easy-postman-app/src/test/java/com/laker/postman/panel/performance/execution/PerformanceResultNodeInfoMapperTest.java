package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import org.testng.annotations.Test;

import java.util.List;
import java.util.LinkedHashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class PerformanceResultNodeInfoMapperTest {

    @Test
    public void shouldSimplifyDisplayFieldsAndDropEventInfoWhenDisabled() {
        PreparedRequest request = new PreparedRequest();
        request.id = "req-1";
        request.body = "{\"hello\":\"world\"}";
        request.bodyType = "json";
        request.headersList = List.of(new HttpHeader(true, "X-Test", "1"));
        request.collectEventInfo = false;

        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.filePath = "/tmp/result.bin";
        response.fileName = "result.bin";
        response.httpEventInfo = new HttpEventInfo();
        response.costMs = 123L;

        PerformanceRequestExecutionResult executionResult = new PerformanceRequestExecutionResult(
                "api-1",
                "API 1",
                request,
                response,
                null,
                null,
                false,
                false,
                false,
                1000L,
                0L
        );

        ResultNodeInfo resultNodeInfo = PerformanceResultNodeInfoMapper.toDisplayNodeInfo(executionResult);

        assertSame(resultNodeInfo.req, request);
        assertSame(resultNodeInfo.resp, response);
        assertNull(request.id);
        assertNull(request.body);
        assertNull(request.bodyType);
        assertNull(request.headersList);
        assertNull(response.filePath);
        assertNull(response.fileName);
        assertNull(response.httpEventInfo);
        assertEquals(resultNodeInfo.costMs, 123);
    }

    @Test
    public void shouldHideInternalStreamHeadersAndPromoteStreamErrorForDisplay() {
        PreparedRequest request = new PreparedRequest();
        request.collectEventInfo = false;

        HttpResponse response = new HttpResponse();
        response.code = 0;
        response.costMs = 26L;
        response.headers = new LinkedHashMap<>();
        response.addHeader("Content-Type", List.of("text/plain"));
        response.addHeader("X-Easy-WS-Sent-Count", List.of("0"));
        response.addHeader("X-Easy-WS-Completion-Reason", List.of("failure"));
        response.addHeader("X-Easy-WS-Error", List.of("Resource temporarily unavailable"));

        PerformanceRequestExecutionResult executionResult = new PerformanceRequestExecutionResult(
                "api-1",
                "WebSocket API",
                request,
                response,
                "",
                List.of(),
                true,
                false,
                true,
                1000L,
                0L
        );

        ResultNodeInfo resultNodeInfo = PerformanceResultNodeInfoMapper.toDisplayNodeInfo(executionResult);

        assertEquals(resultNodeInfo.errorMsg, "Resource temporarily unavailable");
        assertTrue(resultNodeInfo.resp.headers.containsKey("Content-Type"));
        assertFalse(resultNodeInfo.resp.headers.containsKey("X-Easy-WS-Sent-Count"));
        assertFalse(resultNodeInfo.resp.headers.containsKey("X-Easy-WS-Completion-Reason"));
        assertFalse(resultNodeInfo.resp.headers.containsKey("X-Easy-WS-Error"));
    }
}
