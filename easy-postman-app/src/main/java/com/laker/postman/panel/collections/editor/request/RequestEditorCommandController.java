package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.stream.MessageType;
import com.laker.postman.request.model.RequestItemProtocolEnum;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 请求编辑器命令控制器。
 * 负责发送态命令、协议切换和输入辅助，不承载布局初始化。
 */
@RequiredArgsConstructor
final class RequestEditorCommandController {
    private static final int WEBSOCKET_NORMAL_CLOSURE = 1000;

    private final JTextField urlField;
    private final EasyRequestHttpHeadersPanel headersPanel;
    private final RequestBodyPanel requestBodyPanel;
    private final RequestLinePanel requestLinePanel;
    private final RequestStreamUiAppender requestStreamUiAppender;
    private final ResponsePanel responsePanel;
    private final ActionListener sendAction;
    private final Supplier<Boolean> isBaseHttpProtocolSupplier;
    private final Supplier<Boolean> isEffectiveSseProtocolSupplier;
    private final Supplier<Boolean> isEffectiveWebSocketProtocolSupplier;
    private final RequestExecutionState executionState;
    private final Supplier<RequestItemProtocolEnum> currentProtocolSupplier;
    private final Consumer<RequestItemProtocolEnum> currentProtocolSetter;
    private final Consumer<RequestItemProtocolEnum> protocolTabUpdater;
    private final RequestCurlImportController curlImportController;
    private final Runnable updateTabDirtyAction;

    void sendWebSocketMessage() {
        RealtimeWebSocketConnection currentWebSocket = executionState.currentWebSocket();
        if (currentWebSocket == null) {
            requestStreamUiAppender.appendWebSocketMessage(MessageType.INFO,
                    I18nUtil.getMessage(MessageKeys.WEBSOCKET_NOT_CONNECTED));
            return;
        }

        String message = requestBodyPanel.getRawBody();
        if (CharSequenceUtil.isNotBlank(message)) {
            currentWebSocket.send(message);
            requestStreamUiAppender.appendWebSocketMessage(MessageType.SENT, message);
        }
    }

    void cancelCurrentRequest() {
        var currentEventSource = executionState.currentEventSource();
        if (currentEventSource != null) {
            executionState.markSseCancelled();
            currentEventSource.cancel();
            executionState.clearCurrentEventSource();
        }

        RealtimeWebSocketConnection currentWebSocket = executionState.currentWebSocket();
        if (currentWebSocket != null) {
            currentWebSocket.close(WEBSOCKET_NORMAL_CLOSURE, "User canceled");
            SwingWorker<Void, Void> currentWorker = executionState.currentWorker();
            if (currentWorker instanceof UserClosableWebSocketWorker closeableWebSocketWorker) {
                closeableWebSocketWorker.requestUserClose();
                responsePanel.hideLoadingOverlay();
                return;
            }
            executionState.clearCurrentWebSocket();
        }

        executionState.clearCurrentWebSocketConnectionId();
        executionState.cancelCurrentHttpCall();

        SwingWorker<Void, Void> currentWorker = executionState.currentWorker();
        if (currentWorker != null) {
            currentWorker.cancel(true);
        }
        requestLinePanel.setSendButtonToSend(sendAction);
        executionState.clearCurrentWorker();
        responsePanel.hideLoadingOverlay();

        if (Boolean.TRUE.equals(isBaseHttpProtocolSupplier.get()) && executionState.isAutoDetectedHttpSseOpen()) {
            responsePanel.switchTabButtonHttpOrSse("sse");
            requestStreamUiAppender.appendSseMessage(MessageType.CLOSED, null, "closed", null,
                    I18nUtil.getMessage(MessageKeys.STREAM_USER_CANCELED), null);
        }
    }

    void convertCurrentRequestToSse() {
        if (!Boolean.TRUE.equals(isBaseHttpProtocolSupplier.get()) || Boolean.TRUE.equals(isEffectiveSseProtocolSupplier.get())) {
            return;
        }

        currentProtocolSetter.accept(RequestItemProtocolEnum.SSE);
        headersPanel.setOrUpdateHeader("Accept", "text/event-stream");
        protocolTabUpdater.accept(currentProtocolSupplier.get());
        updateTabDirtyAction.run();
    }

    void autoPrependProtocolIfNeeded() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            return;
        }

        String normalizedUrl = RequestUrlEditorSupport.prependProtocolIfNeeded(
                url,
                Boolean.TRUE.equals(isEffectiveWebSocketProtocolSupplier.get()),
                SettingManager.getDefaultProtocol()
        );
        if (!Objects.equals(normalizedUrl, url)) {
            urlField.setText(normalizedUrl);
        }
    }

    void detectAndParseCurl(boolean isLoadingData) {
        curlImportController.detectAndImport(isLoadingData);
    }
}
