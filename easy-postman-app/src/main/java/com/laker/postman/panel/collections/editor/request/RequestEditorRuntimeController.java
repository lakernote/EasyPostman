package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.execution.RequestPreparationResult;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.request.AppRequestHeaderDefaults;
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
        RequestViewComponents view = config.view();
        RequestExecutionState executionState = new RequestExecutionState();
        RequestDirtyStateHelper dirtyStateHelper = new RequestDirtyStateHelper(
                config.currentRequestFromModelSupplier(),
                config.tabDirtyUpdater(),
                AppRequestHeaderDefaults.generatedHeaderPolicy()
        );
        RequestExecutionUiHelper executionUiHelper = new RequestExecutionUiHelper(
                view.responsePanel,
                view.requestLinePanel,
                view.requestBodyPanel,
                view.reqTabs,
                config.sendAction(),
                config.baseHttpProtocolSupplier(),
                config.effectiveHttpProtocolSupplier(),
                config.effectiveSseProtocolSupplier(),
                config.effectiveWebSocketProtocolSupplier()
        );
        RequestStreamUiHelper streamUiHelper = new RequestStreamUiHelper(view.responsePanel, TIME_FORMATTER);
        RequestEditorActionsController actionsController = new RequestEditorActionsController(
                view.urlField,
                view.headersPanel,
                view.requestBodyPanel,
                view.requestLinePanel,
                streamUiHelper,
                view.responsePanel,
                config.sendAction(),
                () -> config.baseHttpProtocolSupplier().getAsBoolean(),
                () -> config.effectiveSseProtocolSupplier().getAsBoolean(),
                () -> config.effectiveWebSocketProtocolSupplier().getAsBoolean(),
                executionState,
                config.currentProtocolSupplier(),
                config.currentProtocolSetter(),
                config.protocolTabUpdater(),
                new RequestCurlImportController(view.urlField, config.requestImporter()),
                config.updateTabDirtyAction()
        );
        RequestResponseHelper responseHelper = new RequestResponseHelper(
                config.owner(),
                view.responsePanel,
                view.responsePanel::setTestResults,
                config.exchangeRecorder()
        );
        HttpRequestExecutionHelper httpExecutionHelper = new HttpRequestExecutionHelper(
                view.responsePanel,
                executionUiHelper,
                streamUiHelper,
                responseHelper,
                actionsController::convertCurrentRequestToSse,
                executionState
        );
        SseRequestExecutionHelper sseExecutionHelper = new SseRequestExecutionHelper(
                view.responsePanel,
                executionUiHelper,
                streamUiHelper,
                responseHelper,
                executionState
        );
        WebSocketRequestExecutionHelper webSocketExecutionHelper = new WebSocketRequestExecutionHelper(
                view.responsePanel,
                executionUiHelper,
                streamUiHelper,
                responseHelper,
                executionState
        );
        RequestProtocolDispatchHelper protocolDispatchHelper = new RequestProtocolDispatchHelper(
                view.responsePanel,
                httpExecutionHelper,
                sseExecutionHelper,
                webSocketExecutionHelper,
                executionState,
                config.maxRedirectCount()
        );
        RequestSendCoordinator sendCoordinator = new RequestSendCoordinator(
                executionState,
                actionsController::cancelCurrentRequest,
                view.urlField,
                config.preparationFeedbackHelper(),
                view.requestLinePanel,
                config.sendAction(),
                view.responsePanel,
                config.preparationSupplier(),
                executionUiHelper::updateUIForRequesting,
                protocolDispatchHelper::dispatch
        );
        RequestSplitLayoutHelper splitLayoutHelper = new RequestSplitLayoutHelper(
                view.splitPane,
                view.responsePanel,
                () -> config.effectiveSseProtocolSupplier().getAsBoolean(),
                () -> config.effectiveWebSocketProtocolSupplier().getAsBoolean()
        );
        applyInitialSplitLayout(view.splitPane, splitLayoutHelper);
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
                  RequestViewComponents view,
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
