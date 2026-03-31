package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.function.BooleanSupplier;

final class RequestExecutionUiHelper {
    private final ResponsePanel responsePanel;
    private final RequestLinePanel requestLinePanel;
    private final RequestBodyPanel requestBodyPanel;
    private final JTabbedPane reqTabs;
    private final ActionListener sendAction;
    private final BooleanSupplier baseHttpProtocol;
    private final BooleanSupplier effectiveHttpProtocol;
    private final BooleanSupplier effectiveSseProtocol;
    private final BooleanSupplier effectiveWebSocketProtocol;

    RequestExecutionUiHelper(ResponsePanel responsePanel,
                             RequestLinePanel requestLinePanel,
                             RequestBodyPanel requestBodyPanel,
                             JTabbedPane reqTabs,
                             ActionListener sendAction,
                             BooleanSupplier baseHttpProtocol,
                             BooleanSupplier effectiveHttpProtocol,
                             BooleanSupplier effectiveSseProtocol,
                             BooleanSupplier effectiveWebSocketProtocol) {
        this.responsePanel = responsePanel;
        this.requestLinePanel = requestLinePanel;
        this.requestBodyPanel = requestBodyPanel;
        this.reqTabs = reqTabs;
        this.sendAction = sendAction;
        this.baseHttpProtocol = baseHttpProtocol;
        this.effectiveHttpProtocol = effectiveHttpProtocol;
        this.effectiveSseProtocol = effectiveSseProtocol;
        this.effectiveWebSocketProtocol = effectiveWebSocketProtocol;
    }

    void updateUIForRequesting() {
        requestLinePanel.setSendButtonToCancel(sendAction);
        responsePanel.clearInFlightRequestDetails();
        boolean keepResponseTabsEnabled = baseHttpProtocol.getAsBoolean()
                && !effectiveSseProtocol.getAsBoolean()
                && !effectiveWebSocketProtocol.getAsBoolean()
                && responsePanel.hasResponseData();
        if (!keepResponseTabsEnabled) {
            responsePanel.setResponseTabButtonsEnable(false);
        }
        if (baseHttpProtocol.getAsBoolean()) {
            responsePanel.setResponseBodyEnabled(false);
        }
        // 普通 HTTP 请求发送时保留上一份响应内容，避免当前响应标签页先被清空后再重绘造成闪烁。
        // 流式协议仍沿用清空策略，防止新旧消息混在一起。
        if (!baseHttpProtocol.getAsBoolean() || effectiveSseProtocol.getAsBoolean() || effectiveWebSocketProtocol.getAsBoolean()) {
            responsePanel.clearAll();
        }
        responsePanel.showLoadingOverlay();
    }

    void updateUIForResponse(HttpResponse resp) {
        responsePanel.hideLoadingOverlay();

        if (resp == null) {
            responsePanel.clearAll();
            responsePanel.setStatus(0);
            if (baseHttpProtocol.getAsBoolean()) {
                responsePanel.setResponseBodyEnabled(effectiveHttpProtocol.getAsBoolean());
            }
            return;
        }

        responsePanel.setResponseHeaders(resp);
        if (!effectiveWebSocketProtocol.getAsBoolean()) {
            responsePanel.setTiming(resp);
        }
        if (effectiveHttpProtocol.getAsBoolean() && !resp.isSse) {
            responsePanel.setResponseBody(resp);
            responsePanel.setResponseBodyEnabled(true);
        } else if (baseHttpProtocol.getAsBoolean()) {
            responsePanel.setResponseBodyEnabled(false);
        }
        responsePanel.setStatus(resp.code);
        responsePanel.setResponseTime(resp.costMs);
        responsePanel.setResponseSize(resp.bodySize, resp);
        responsePanel.markResponseDataLoaded();
    }

    void resetSendButton() {
        requestLinePanel.setSendButtonToSend(sendAction);
    }

    void switchSendButtonToClose() {
        requestLinePanel.setSendButtonToClose(sendAction);
    }

    void setWebSocketConnected(boolean connected) {
        requestBodyPanel.setWebSocketConnected(connected);
    }

    void activateWebSocketBodyTab() {
        reqTabs.setSelectedComponent(requestBodyPanel);
        requestBodyPanel.getWsSendButton().requestFocusInWindow();
    }
}
