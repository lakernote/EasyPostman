package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

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
}
