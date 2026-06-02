package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.PreparedRequest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class SseRequestExecutionHelperTest {

    @Test(description = "SSE 发送应打开实际请求快照和网络日志，便于对比 cURL")
    public void shouldEnableNetworkCaptureForSseRequests() {
        PreparedRequest request = new PreparedRequest();

        new SseRequestExecutionHelper(
                null,
                null,
                null,
                null,
                new RequestExecutionState()
        ).createWorker(request, null);

        assertTrue(request.collectBasicInfo);
        assertTrue(request.collectMetricsInfo);
        assertTrue(request.collectEventInfo);
        assertTrue(request.enableNetworkLog);
    }
}
