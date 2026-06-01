package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.execution.RequestPreparationResult;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.request.AppRequestHeaderDefaults;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.service.setting.SettingManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 请求编辑器运行时控制器集合。
 * <p>
 * 核心点：发送、取消、协议分发、流式连接、响应回填和布局联动都属于运行态编排，
 * 不放在 RequestEditSubPanel 的视图初始化代码里。
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class RequestEditorRuntimeController {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final RequestExecutionState executionState;
    private final RequestDirtyStateHelper dirtyStateHelper;
    private final RequestEditorActionsController actionsController;
    private final RequestSendCoordinator sendCoordinator;
    private final RequestSplitLayoutHelper splitLayoutHelper;

    static RequestEditorRuntimeController create(Config config) {
        RequestExecutionState executionState = new RequestExecutionState();
        RequestDirtyStateHelper dirtyStateHelper = new RequestDirtyStateHelper(
                config.currentRequestFromModelSupplier(),
                config.tabDirtyUpdater(),
                AppRequestHeaderDefaults.generatedHeaderPolicy()
        );
        RequestExecutionUiHelper executionUiHelper = new RequestExecutionUiHelper(
                config.responsePanel(),
                config.requestLinePanel(),
                config.requestBodyPanel(),
                config.requestTabs(),
                config.sendAction(),
                config.baseHttpProtocolSupplier(),
                config.effectiveHttpProtocolSupplier(),
                config.effectiveSseProtocolSupplier(),
                config.effectiveWebSocketProtocolSupplier()
        );
        RequestStreamUiHelper streamUiHelper = new RequestStreamUiHelper(config.responsePanel(), TIME_FORMATTER);
        RequestEditorActionsController actionsController = new RequestEditorActionsController(
                config.urlField(),
                config.headersPanel(),
                config.requestBodyPanel(),
                config.requestLinePanel(),
                streamUiHelper,
                config.responsePanel(),
                config.sendAction(),
                () -> config.baseHttpProtocolSupplier().getAsBoolean(),
                () -> config.effectiveSseProtocolSupplier().getAsBoolean(),
                () -> config.effectiveWebSocketProtocolSupplier().getAsBoolean(),
                executionState,
                config.currentProtocolSupplier(),
                config.currentProtocolSetter(),
                config.protocolTabUpdater(),
                new RequestCurlImportController(config.urlField(), config.requestImporter()),
                config.updateTabDirtyAction()
        );
        RequestResponseHelper responseHelper = new RequestResponseHelper(
                config.owner(),
                config.responsePanel(),
                config.responsePanel()::setTestResults,
                config.exchangeRecorder()
        );
        HttpRequestExecutionHelper httpExecutionHelper = new HttpRequestExecutionHelper(
                config.responsePanel(),
                executionUiHelper,
                streamUiHelper,
                responseHelper,
                actionsController::convertCurrentRequestToSse,
                executionState
        );
        SseRequestExecutionHelper sseExecutionHelper = new SseRequestExecutionHelper(
                config.responsePanel(),
                executionUiHelper,
                streamUiHelper,
                responseHelper,
                executionState
        );
        WebSocketRequestExecutionHelper webSocketExecutionHelper = new WebSocketRequestExecutionHelper(
                config.responsePanel(),
                executionUiHelper,
                streamUiHelper,
                responseHelper,
                executionState
        );
        RequestProtocolDispatchHelper protocolDispatchHelper = new RequestProtocolDispatchHelper(
                config.responsePanel(),
                httpExecutionHelper,
                sseExecutionHelper,
                webSocketExecutionHelper,
                executionState,
                config.maxRedirectCount()
        );
        RequestSendCoordinator sendCoordinator = new RequestSendCoordinator(
                executionState,
                actionsController::cancelCurrentRequest,
                config.urlField(),
                config.preparationFeedbackHelper(),
                config.requestLinePanel(),
                config.sendAction(),
                config.responsePanel(),
                config.preparationSupplier(),
                executionUiHelper::updateUIForRequesting,
                protocolDispatchHelper::dispatch
        );
        RequestSplitLayoutHelper splitLayoutHelper = new RequestSplitLayoutHelper(
                config.splitPane(),
                config.responsePanel(),
                () -> config.effectiveSseProtocolSupplier().getAsBoolean(),
                () -> config.effectiveWebSocketProtocolSupplier().getAsBoolean()
        );
        applyInitialSplitLayout(config.splitPane(), splitLayoutHelper);
        return new RequestEditorRuntimeController(
                executionState,
                dirtyStateHelper,
                actionsController,
                sendCoordinator,
                splitLayoutHelper
        );
    }

    RequestDirtyStateHelper dirtyStateHelper() {
        return dirtyStateHelper;
    }

    void updateTabDirty() {
        dirtyStateHelper.updateTabDirty();
    }

    void sendRequest() {
        sendCoordinator.sendRequest();
    }

    void sendWebSocketMessage() {
        actionsController.sendWebSocketMessage();
    }

    void disposeOpenConnections() {
        executionState.disposeOpenConnections();
    }

    void autoPrependProtocolIfNeeded() {
        actionsController.autoPrependProtocolIfNeeded();
    }

    void detectAndParseCurl(boolean loadingData) {
        actionsController.detectAndParseCurl(loadingData);
    }

    void updateLayoutOrientation(boolean vertical) {
        splitLayoutHelper.updateLayoutOrientation(vertical);
    }

    void handleInitialLayout() {
        splitLayoutHelper.handleInitialLayout();
    }

    private static void applyInitialSplitLayout(JSplitPane splitPane, RequestSplitLayoutHelper splitLayoutHelper) {
        boolean vertical = SettingManager.isLayoutVertical();
        double initialRatio = vertical ? splitLayoutHelper.getDefaultResizeWeight() : 0.5;
        splitPane.setResizeWeight(initialRatio);
    }

    record Config(Component owner,
                  JTextField urlField,
                  EasyRequestHttpHeadersPanel headersPanel,
                  RequestBodyPanel requestBodyPanel,
                  RequestLinePanel requestLinePanel,
                  JTabbedPane requestTabs,
                  ResponsePanel responsePanel,
                  JSplitPane splitPane,
                  RequestPreparationFeedbackHelper preparationFeedbackHelper,
                  Supplier<RequestPreparationResult> preparationSupplier,
                  ActionListener sendAction,
                  BooleanSupplier baseHttpProtocolSupplier,
                  BooleanSupplier effectiveHttpProtocolSupplier,
                  BooleanSupplier effectiveSseProtocolSupplier,
                  BooleanSupplier effectiveWebSocketProtocolSupplier,
                  Supplier<RequestItemProtocolEnum> currentProtocolSupplier,
                  Consumer<RequestItemProtocolEnum> currentProtocolSetter,
                  Consumer<RequestItemProtocolEnum> protocolTabUpdater,
                  Consumer<HttpRequestItem> requestImporter,
                  Supplier<HttpRequestItem> currentRequestFromModelSupplier,
                  Consumer<Boolean> tabDirtyUpdater,
                  BiConsumer<PreparedRequest, HttpResponse> exchangeRecorder,
                  Runnable updateTabDirtyAction,
                  int maxRedirectCount) {
    }
}
