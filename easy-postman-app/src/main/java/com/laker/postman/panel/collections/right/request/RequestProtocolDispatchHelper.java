package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.js.ScriptExecutionPipeline;

import javax.swing.*;
import java.util.function.Consumer;

final class RequestProtocolDispatchHelper {
    private final ResponsePanel responsePanel;
    private final HttpRequestExecutionHelper httpRequestExecutionHelper;
    private final SseRequestExecutionHelper sseRequestExecutionHelper;
    private final WebSocketRequestExecutionHelper webSocketRequestExecutionHelper;
    private final Consumer<Boolean> httpSseStreamOpenedSetter;
    private final Consumer<SwingWorker<Void, Void>> currentWorkerSetter;
    private final int maxRedirectCount;

    RequestProtocolDispatchHelper(ResponsePanel responsePanel,
                                  HttpRequestExecutionHelper httpRequestExecutionHelper,
                                  SseRequestExecutionHelper sseRequestExecutionHelper,
                                  WebSocketRequestExecutionHelper webSocketRequestExecutionHelper,
                                  Consumer<Boolean> httpSseStreamOpenedSetter,
                                  Consumer<SwingWorker<Void, Void>> currentWorkerSetter,
                                  int maxRedirectCount) {
        this.responsePanel = responsePanel;
        this.httpRequestExecutionHelper = httpRequestExecutionHelper;
        this.sseRequestExecutionHelper = sseRequestExecutionHelper;
        this.webSocketRequestExecutionHelper = webSocketRequestExecutionHelper;
        this.httpSseStreamOpenedSetter = httpSseStreamOpenedSetter;
        this.currentWorkerSetter = currentWorkerSetter;
        this.maxRedirectCount = maxRedirectCount;
    }

    void dispatch(RequestPreparationResult result) {
        RequestItemProtocolEnum protocol = result.getItem().getProtocol();
        PreparedRequest request = result.getRequest();
        ScriptExecutionPipeline pipeline = result.getPipeline();
        boolean expectedHttpSse = protocol.isHttpProtocol() && HttpUtil.isSSERequest(request);

        SwingWorker<Void, Void> worker;
        // 这里是协议分发的唯一出口：上游不用关心 HTTP / SSE / WebSocket 的执行细节。
        if (protocol.isWebSocketProtocol()) {
            worker = webSocketRequestExecutionHelper.createWorker(request, pipeline);
        } else if (protocol.isSseProtocol()) {
            worker = sseRequestExecutionHelper.createWorker(request, pipeline);
        } else {
            httpSseStreamOpenedSetter.accept(false);
            if (expectedHttpSse) {
                responsePanel.clearAll();
                responsePanel.setResponseTabButtonsEnable(false);
            }
            worker = httpRequestExecutionHelper.createWorker(request, pipeline, maxRedirectCount);
        }

        currentWorkerSetter.accept(worker);
        worker.execute();
    }
}
