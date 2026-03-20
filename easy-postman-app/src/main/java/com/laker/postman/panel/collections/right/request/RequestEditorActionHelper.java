package com.laker.postman.panel.collections.right.request;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.MessageType;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.CurlImportUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class RequestEditorActionHelper {
    private static final int WEBSOCKET_NORMAL_CLOSURE = 1000;

    private final JTextField urlField;
    private final EasyRequestHttpHeadersPanel headersPanel;
    private final RequestBodyPanel requestBodyPanel;
    private final RequestLinePanel requestLinePanel;
    private final RequestStreamUiHelper requestStreamUiHelper;
    private final ResponsePanel responsePanel;
    private final ActionListener sendAction;
    private final Supplier<Boolean> isBaseHttpProtocolSupplier;
    private final Supplier<Boolean> isEffectiveSseProtocolSupplier;
    private final Supplier<Boolean> isEffectiveWebSocketProtocolSupplier;
    private final Supplier<WebSocket> currentWebSocketSupplier;
    private final Consumer<WebSocket> currentWebSocketSetter;
    private final Supplier<EventSource> currentEventSourceSupplier;
    private final Consumer<EventSource> currentEventSourceSetter;
    private final Supplier<SwingWorker<Void, Void>> currentWorkerSupplier;
    private final Consumer<SwingWorker<Void, Void>> currentWorkerSetter;
    private final Consumer<String> currentWebSocketConnectionIdSetter;
    private final AtomicBoolean currentSseCancelled;
    private final Supplier<Boolean> httpSseStreamOpenedSupplier;
    private final Supplier<RequestItemProtocolEnum> currentProtocolSupplier;
    private final Consumer<RequestItemProtocolEnum> currentProtocolSetter;
    private final Consumer<RequestItemProtocolEnum> protocolTabUpdater;
    private final Consumer<HttpRequestItem> requestImporter;
    private final Runnable updateTabDirtyAction;

    RequestEditorActionHelper(JTextField urlField,
                              EasyRequestHttpHeadersPanel headersPanel,
                              RequestBodyPanel requestBodyPanel,
                              RequestLinePanel requestLinePanel,
                              RequestStreamUiHelper requestStreamUiHelper,
                              ResponsePanel responsePanel,
                              ActionListener sendAction,
                              Supplier<Boolean> isBaseHttpProtocolSupplier,
                              Supplier<Boolean> isEffectiveSseProtocolSupplier,
                              Supplier<Boolean> isEffectiveWebSocketProtocolSupplier,
                              Supplier<WebSocket> currentWebSocketSupplier,
                              Consumer<WebSocket> currentWebSocketSetter,
                              Supplier<EventSource> currentEventSourceSupplier,
                              Consumer<EventSource> currentEventSourceSetter,
                              Supplier<SwingWorker<Void, Void>> currentWorkerSupplier,
                              Consumer<SwingWorker<Void, Void>> currentWorkerSetter,
                              Consumer<String> currentWebSocketConnectionIdSetter,
                              AtomicBoolean currentSseCancelled,
                              Supplier<Boolean> httpSseStreamOpenedSupplier,
                              Supplier<RequestItemProtocolEnum> currentProtocolSupplier,
                              Consumer<RequestItemProtocolEnum> currentProtocolSetter,
                              Consumer<RequestItemProtocolEnum> protocolTabUpdater,
                              Consumer<HttpRequestItem> requestImporter,
                              Runnable updateTabDirtyAction) {
        this.urlField = urlField;
        this.headersPanel = headersPanel;
        this.requestBodyPanel = requestBodyPanel;
        this.requestLinePanel = requestLinePanel;
        this.requestStreamUiHelper = requestStreamUiHelper;
        this.responsePanel = responsePanel;
        this.sendAction = sendAction;
        this.isBaseHttpProtocolSupplier = isBaseHttpProtocolSupplier;
        this.isEffectiveSseProtocolSupplier = isEffectiveSseProtocolSupplier;
        this.isEffectiveWebSocketProtocolSupplier = isEffectiveWebSocketProtocolSupplier;
        this.currentWebSocketSupplier = currentWebSocketSupplier;
        this.currentWebSocketSetter = currentWebSocketSetter;
        this.currentEventSourceSupplier = currentEventSourceSupplier;
        this.currentEventSourceSetter = currentEventSourceSetter;
        this.currentWorkerSupplier = currentWorkerSupplier;
        this.currentWorkerSetter = currentWorkerSetter;
        this.currentWebSocketConnectionIdSetter = currentWebSocketConnectionIdSetter;
        this.currentSseCancelled = currentSseCancelled;
        this.httpSseStreamOpenedSupplier = httpSseStreamOpenedSupplier;
        this.currentProtocolSupplier = currentProtocolSupplier;
        this.currentProtocolSetter = currentProtocolSetter;
        this.protocolTabUpdater = protocolTabUpdater;
        this.requestImporter = requestImporter;
        this.updateTabDirtyAction = updateTabDirtyAction;
    }

    void sendWebSocketMessage() {
        WebSocket currentWebSocket = currentWebSocketSupplier.get();
        if (currentWebSocket == null) {
            requestStreamUiHelper.appendWebSocketMessage(MessageType.INFO,
                    I18nUtil.getMessage(MessageKeys.WEBSOCKET_NOT_CONNECTED));
            return;
        }

        String message = requestBodyPanel.getRawBody();
        if (CharSequenceUtil.isNotBlank(message)) {
            currentWebSocket.send(message);
            requestStreamUiHelper.appendWebSocketMessage(MessageType.SENT, message);
        }
    }

    void cancelCurrentRequest() {
        EventSource currentEventSource = currentEventSourceSupplier.get();
        if (currentEventSource != null) {
            currentSseCancelled.set(true);
            currentEventSource.cancel();
            currentEventSourceSetter.accept(null);
        }

        WebSocket currentWebSocket = currentWebSocketSupplier.get();
        if (currentWebSocket != null) {
            currentWebSocket.close(WEBSOCKET_NORMAL_CLOSURE, "User canceled");
            SwingWorker<Void, Void> currentWorker = currentWorkerSupplier.get();
            if (currentWorker instanceof UserClosableWebSocketWorker closeableWebSocketWorker) {
                closeableWebSocketWorker.requestUserClose();
                responsePanel.hideLoadingOverlay();
                return;
            }
            currentWebSocketSetter.accept(null);
        }

        currentWebSocketConnectionIdSetter.accept(null);

        SwingWorker<Void, Void> currentWorker = currentWorkerSupplier.get();
        if (currentWorker != null) {
            currentWorker.cancel(true);
        }
        requestLinePanel.setSendButtonToSend(sendAction);
        currentWorkerSetter.accept(null);
        responsePanel.hideLoadingOverlay();

        if (Boolean.TRUE.equals(isBaseHttpProtocolSupplier.get()) && Boolean.TRUE.equals(httpSseStreamOpenedSupplier.get())) {
            responsePanel.switchTabButtonHttpOrSse("sse");
            requestStreamUiHelper.appendSseMessage(MessageType.CLOSED, null, "closed", null, "User canceled", null);
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

        String normalizedUrl = RequestUrlHelper.prependProtocolIfNeeded(
                url,
                Boolean.TRUE.equals(isEffectiveWebSocketProtocolSupplier.get()),
                SettingManager.getDefaultProtocol()
        );
        if (!Objects.equals(normalizedUrl, url)) {
            urlField.setText(normalizedUrl);
        }
    }

    void detectAndParseCurl(boolean isLoadingData) {
        if (isLoadingData) {
            return;
        }

        String text = urlField.getText();
        if (text != null && text.trim().toLowerCase().startsWith("curl")) {
            SwingUtilities.invokeLater(() -> parseCurl(text.trim()));
        }
    }

    private void parseCurl(String text) {
        try {
            CurlRequest curlRequest = CurlParser.parse(text);
            if (curlRequest == null || curlRequest.url == null) {
                return;
            }

            HttpRequestItem item = CurlImportUtil.fromCurlRequest(curlRequest);
            if (item == null) {
                return;
            }

            requestImporter.accept(item);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.PARSE_CURL_SUCCESS));
        } catch (Exception ignored) {
            // 用户可能还在输入，不提示
        }
    }
}
