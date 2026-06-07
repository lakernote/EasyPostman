package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.PreparedRequest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class WebSocketRequestExecutorTest {

    @Test(description = "WebSocket 发送应打开握手请求快照和网络日志，和 HTTP/SSE 保持一致")
    public void shouldEnableNetworkCaptureForWebSocketRequests() {
        PreparedRequest request = new PreparedRequest();

        new WebSocketRequestExecutor(
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
