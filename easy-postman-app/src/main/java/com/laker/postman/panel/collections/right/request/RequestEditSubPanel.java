package com.laker.postman.panel.collections.right.request;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.placeholder.RequestEditorPlaceholderPanel;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.*;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.sub.*;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.startup.StartupDiagnostics;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单个请求编辑子面板，包含 URL、方法选择、Headers、Body 和响应展示
 */
@Slf4j
public class RequestEditSubPanel extends JPanel {
    // 常量定义
    private static final int MAX_REDIRECT_COUNT = 10;
    private static final int WEBSOCKET_NORMAL_CLOSURE = 1000;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private JTextField urlField;
    private JComboBox<String> methodBox;
    private EasyRequestParamsPanel paramsPanel;
    private EasyRequestHttpHeadersPanel headersPanel;
    @Getter
    private String id;
    private String name;
    private final RequestItemProtocolEnum protocol;
    private volatile RequestItemProtocolEnum currentProtocol;
    private final boolean deferEditorInitialization;
    private boolean editorInitialized;
    private HttpRequestItem pendingRequestItem;
    private HttpRequestItem pendingOriginalRequestItem;
    private Boolean pendingLayoutVertical;

    // 面板类型
    @Getter
    private final RequestEditSubPanelType panelType;

    // 如果是 SAVED_RESPONSE 类型，保存关联的 savedResponse 和 parentRequest
    @Getter
    private final SavedResponse savedResponse;

    private RequestLinePanel requestLinePanel;
    //  RequestBodyPanel
    private RequestBodyPanel requestBodyPanel;
    private RequestSettingsPanel requestSettingsPanel;
    private AuthTabPanel authTabPanel;
    private ScriptPanel scriptPanel;
    private MarkdownEditorPanel descriptionEditor; // Docs tab
    private JTabbedPane reqTabs; // 请求选项卡面板
    // 主面板只保留“编排”和“状态持有”，具体行为拆给各个 helper，避免再次演变成巨型类。
    private final RequestPreparationService requestPreparationService = new RequestPreparationService();
    private final RequestPreparationFeedbackHelper requestPreparationFeedbackHelper = new RequestPreparationFeedbackHelper();
    private RequestExecutionUiHelper requestExecutionUiHelper;
    private RequestStreamUiHelper requestStreamUiHelper;
    private RequestResponseHelper requestResponseHelper;
    private RequestFormDataHelper requestFormDataHelper;
    private RequestTabStateHelper requestTabStateHelper;
    private RequestDirtyStateHelper requestDirtyStateHelper;
    private RequestUrlSyncHelper requestUrlSyncHelper;
    private RequestEditorActionHelper requestEditorActionHelper;
    private RequestSplitLayoutHelper requestSplitLayoutHelper;
    private RequestProtocolDispatchHelper requestProtocolDispatchHelper;
    private RequestSendCoordinator requestSendCoordinator;
    private final SavedResponseHelper savedResponseHelper = new SavedResponseHelper();
    private HttpRequestExecutionHelper httpRequestExecutionHelper;
    private SseRequestExecutionHelper sseRequestExecutionHelper;
    private WebSocketRequestExecutionHelper webSocketRequestExecutionHelper;

    // Tab indicators for showing content status
    private IndicatorTabComponent paramsTabIndicator;
    private IndicatorTabComponent authTabIndicator;
    private IndicatorTabComponent headersTabIndicator;
    private IndicatorTabComponent bodyTabIndicator;
    private IndicatorTabComponent settingsTabIndicator;
    private IndicatorTabComponent scriptsTabIndicator;

    // 当前请求的 SwingWorker，用于支持取消
    private transient volatile SwingWorker<Void, Void> currentWorker;
    // 当前 SSE 事件源, 用于取消 SSE 请求
    private transient volatile EventSource currentEventSource;
    // HTTP 请求自动识别为 SSE 后，用于在取消时保持 SSE 视图
    private transient volatile boolean httpSseStreamOpened;
    // 标记当前 SSE 是否由用户主动取消，避免把预期断开误报为失败
    private final transient AtomicBoolean currentSseCancelled = new AtomicBoolean(false);
    // WebSocket连接对象
    private transient volatile WebSocket currentWebSocket;
    // WebSocket连接ID，用于防止过期连接的回调
    private volatile String currentWebSocketConnectionId;
    JSplitPane splitPane;
    // 数据加载标志，防止加载时触发自动保存和联动更新
    private boolean isLoadingData = false;
    private volatile boolean disposed;
    private ResponsePanel responsePanel;

    // 保存最后一次请求和响应，用于保存响应功能
    private PreparedRequest lastRequest;
    private HttpResponse lastResponse;

    private RequestItemProtocolEnum getEffectiveProtocol() {
        return currentProtocol != null ? currentProtocol : protocol;
    }

    private boolean isBaseHttpProtocol() {
        return protocol != null && protocol.isHttpProtocol();
    }

    private boolean isEffectiveHttpProtocol() {
        return getEffectiveProtocol() != null && getEffectiveProtocol().isHttpProtocol();
    }

    private boolean isEffectiveSseProtocol() {
        return getEffectiveProtocol() != null && getEffectiveProtocol().isSseProtocol();
    }

    private boolean isEffectiveWebSocketProtocol() {
        return getEffectiveProtocol() != null && getEffectiveProtocol().isWebSocketProtocol();
    }

    /**
     * 判断当前面板是否是保存的响应标签页
     */
    public boolean isSavedResponseTab() {
        return panelType == RequestEditSubPanelType.SAVED_RESPONSE;
    }

    /**
     * 普通请求编辑面板构造函数
     */
    public RequestEditSubPanel(String id, RequestItemProtocolEnum protocol) {
        this(id, protocol, false);
    }

    public RequestEditSubPanel(String id, RequestItemProtocolEnum protocol, boolean deferEditorInitialization) {
        this(id, protocol, RequestEditSubPanelType.NORMAL, null, deferEditorInitialization);
    }

    /**
     * 保存的响应面板构造函数
     */
    public RequestEditSubPanel(SavedResponse savedResponse) {
        this(savedResponse.getId(), RequestItemProtocolEnum.HTTP, RequestEditSubPanelType.SAVED_RESPONSE, savedResponse, false);
    }

    /**
     * 完整构造函数
     */
    private RequestEditSubPanel(String id, RequestItemProtocolEnum protocol, RequestEditSubPanelType panelType,
                                SavedResponse savedResponse,
                                boolean deferEditorInitialization) {
        this.id = id;
        this.protocol = protocol;
        this.currentProtocol = protocol;
        this.panelType = panelType;
        this.savedResponse = savedResponse;
        this.deferEditorInitialization = deferEditorInitialization && panelType == RequestEditSubPanelType.NORMAL;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 设置边距为5
        setOpaque(true);
        setBackground(ModernColors.getBackgroundColor());
        if (!this.deferEditorInitialization) {
            initializeEditorUiIfNeeded();
        } else {
            installDeferredPlaceholder();
        }
    }

    public void ensureEditorInitialized() {
        ensureEditorInitialized(false);
    }

    public void ensureEditorInitialized(boolean animatePlaceholderTransition) {
        // 保留这个重载是为了兼容现有调用方。
        // 但这里不再启用占位快照过渡，否则透明 overlay 会影响真实组件的 hover 命中。
        if (editorInitialized) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            initializeEditorUiIfNeeded();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(this::initializeEditorUiIfNeeded);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RequestEditSubPanel on EDT", e);
        }
    }

    public boolean isEditorInitialized() {
        return editorInitialized;
    }

    public RequestLinePanel getRequestLinePanel() {
        ensureEditorInitialized();
        return requestLinePanel;
    }

    public ResponsePanel getResponsePanel() {
        ensureEditorInitialized();
        return responsePanel;
    }

    private void initializeEditorUiIfNeeded() {
        if (editorInitialized) {
            return;
        }
        long initStartNanos = System.nanoTime();
        removeAll();
        // 先集中创建视图组件，再把交互逻辑按职责注入 helper，构造阶段就能看清依赖方向。
        RequestViewComponents components = RequestViewFactory.create(protocol, panelType, this::sendRequest);
        requestLinePanel = components.requestLinePanel;
        methodBox = components.methodBox;
        urlField = components.urlField;
        reqTabs = components.reqTabs;
        descriptionEditor = components.descriptionEditor;
        paramsPanel = components.paramsPanel;
        paramsTabIndicator = components.paramsTabIndicator;
        authTabPanel = components.authTabPanel;
        authTabIndicator = components.authTabIndicator;
        headersPanel = components.headersPanel;
        headersTabIndicator = components.headersTabIndicator;
        requestBodyPanel = components.requestBodyPanel;
        bodyTabIndicator = components.bodyTabIndicator;
        settingsTabIndicator = components.settingsTabIndicator;
        requestSettingsPanel = components.requestSettingsPanel;
        scriptPanel = components.scriptPanel;
        scriptsTabIndicator = components.scriptsTabIndicator;
        responsePanel = components.responsePanel;
        splitPane = components.splitPane;

        RequestUiSetupHelper.bindUrlField(
                urlField,
                requestPreparationFeedbackHelper,
                this::detectAndParseCurl,
                this::parseUrlParamsToParamsPanel,
                this::autoPrependHttpsIfNeeded
        );
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(requestLinePanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
        RequestUiSetupHelper.bindParamsSync(paramsPanel, this::parseParamsPanelToUrl);
        requestUrlSyncHelper = new RequestUrlSyncHelper(urlField, paramsPanel);
        requestFormDataHelper = new RequestFormDataHelper(
                urlField,
                methodBox,
                paramsPanel,
                headersPanel,
                requestBodyPanel,
                requestSettingsPanel,
                authTabPanel,
                scriptPanel,
                descriptionEditor,
                reqTabs
        );
        requestTabStateHelper = new RequestTabStateHelper(
                protocol,
                urlField,
                methodBox,
                descriptionEditor,
                paramsPanel,
                authTabPanel,
                headersPanel,
                requestBodyPanel,
                requestSettingsPanel,
                scriptPanel,
                paramsTabIndicator,
                authTabIndicator,
                headersTabIndicator,
                bodyTabIndicator,
                settingsTabIndicator,
                scriptsTabIndicator
        );
        requestDirtyStateHelper = new RequestDirtyStateHelper(
                this::getCurrentRequestFromModel,
                dirty -> SingletonFactory.getInstance(RequestEditPanel.class).updateTabDirty(this, dirty)
        );
        requestExecutionUiHelper = new RequestExecutionUiHelper(
                responsePanel,
                requestLinePanel,
                requestBodyPanel,
                reqTabs,
                this::sendRequest,
                this::isBaseHttpProtocol,
                this::isEffectiveHttpProtocol,
                this::isEffectiveWebSocketProtocol
        );
        requestStreamUiHelper = new RequestStreamUiHelper(responsePanel, TIME_FORMATTER);
        requestEditorActionHelper = new RequestEditorActionHelper(
                urlField,
                headersPanel,
                requestBodyPanel,
                requestLinePanel,
                requestStreamUiHelper,
                responsePanel,
                this::sendRequest,
                this::isBaseHttpProtocol,
                this::isEffectiveSseProtocol,
                this::isEffectiveWebSocketProtocol,
                () -> currentWebSocket,
                webSocket -> currentWebSocket = webSocket,
                () -> currentEventSource,
                eventSource -> currentEventSource = eventSource,
                () -> currentWorker,
                worker -> currentWorker = worker,
                connectionId -> currentWebSocketConnectionId = connectionId,
                currentSseCancelled,
                () -> httpSseStreamOpened,
                this::getEffectiveProtocol,
                newProtocol -> currentProtocol = newProtocol,
                newProtocol -> SingletonFactory.getInstance(RequestEditPanel.class).updateTabProtocol(this, newProtocol),
                this::initPanelData,
                this::updateTabDirty
        );
        requestResponseHelper = new RequestResponseHelper(
                this,
                responsePanel,
                responsePanel::setTestResults,
                (request, response) -> {
                    lastRequest = request;
                    lastResponse = response;
                }
        );
        httpRequestExecutionHelper = new HttpRequestExecutionHelper(
                responsePanel,
                requestExecutionUiHelper,
                requestStreamUiHelper,
                requestResponseHelper,
                this::convertCurrentRequestToSse,
                opened -> httpSseStreamOpened = opened,
                () -> httpSseStreamOpened,
                () -> currentWorker = null,
                this::isDisposed
        );
        sseRequestExecutionHelper = new SseRequestExecutionHelper(
                responsePanel,
                requestExecutionUiHelper,
                requestStreamUiHelper,
                requestResponseHelper,
                currentSseCancelled,
                eventSource -> currentEventSource = eventSource,
                () -> currentEventSource = null,
                () -> currentWorker = null,
                this::isDisposed
        );
        webSocketRequestExecutionHelper = new WebSocketRequestExecutionHelper(
                responsePanel,
                requestExecutionUiHelper,
                requestStreamUiHelper,
                requestResponseHelper,
                webSocket -> currentWebSocket = webSocket,
                connectionId -> currentWebSocketConnectionId = connectionId,
                () -> currentWebSocketConnectionId,
                () -> currentWorker,
                () -> currentWorker = null,
                this::isDisposed
        );
        requestProtocolDispatchHelper = new RequestProtocolDispatchHelper(
                httpRequestExecutionHelper,
                sseRequestExecutionHelper,
                webSocketRequestExecutionHelper,
                opened -> httpSseStreamOpened = opened,
                worker -> currentWorker = worker,
                MAX_REDIRECT_COUNT
        );
        requestSendCoordinator = new RequestSendCoordinator(
                () -> currentWorker,
                worker -> currentWorker = worker,
                this::cancelCurrentRequest,
                urlField,
                requestPreparationFeedbackHelper,
                requestLinePanel,
                this::sendRequest,
                responsePanel,
                this::prepareRequestForSending,
                requestExecutionUiHelper::updateUIForRequesting,
                requestProtocolDispatchHelper::dispatch
        );

        requestSplitLayoutHelper = new RequestSplitLayoutHelper(
                splitPane,
                responsePanel,
                this::isEffectiveSseProtocol,
                this::isEffectiveWebSocketProtocol
        );
        boolean isVertical = SettingManager.isLayoutVertical();
        double initialRatio = isVertical ? requestSplitLayoutHelper.getDefaultResizeWeight() : 0.5;
        splitPane.setResizeWeight(initialRatio);

        add(splitPane, BorderLayout.CENTER);
        RequestUiSetupHelper.applyInitialProtocolUi(
                protocol,
                reqTabs,
                requestBodyPanel,
                paramsPanel,
                authTabPanel,
                e -> sendWebSocketMessage()
        );
        // 监听表单内容变化，动态更新tab红点
        addDirtyListeners();

        // 初始化tab指示器状态
        SwingUtilities.invokeLater(this::updateTabIndicators);
        RequestUiSetupHelper.bindSaveResponseButton(protocol, panelType, responsePanel, e -> saveResponseDialog());
        RequestUiSetupHelper.bindBodyTypeHeaderSync(requestBodyPanel, headersPanel, () -> isLoadingData);
        editorInitialized = true;

        if (pendingRequestItem != null) {
            populatePanelData(pendingRequestItem, pendingOriginalRequestItem != null ? pendingOriginalRequestItem : pendingRequestItem);
        }
        if (pendingLayoutVertical != null) {
            requestSplitLayoutHelper.updateLayoutOrientation(pendingLayoutVertical);
            pendingLayoutVertical = null;
        }
        revalidate();
        repaint();
        if (deferEditorInitialization) {
            StartupDiagnostics.mark("Initialized deferred editor tab '" + (name != null ? name : id)
                    + "' in " + StartupDiagnostics.formatSince(initStartNanos));
        }
    }

    private void installDeferredPlaceholder() {
        removeAll();
        JComponent placeholder = new RequestEditorPlaceholderPanel();
        attachDeferredActivationListeners(placeholder);
        add(placeholder, BorderLayout.CENTER);
    }

    private void attachDeferredActivationListeners(Component component) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                activateDeferredEditorIfNeeded();
            }
        };
        FocusAdapter focusAdapter = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                activateDeferredEditorIfNeeded();
            }
        };
        attachDeferredActivationListeners(component, mouseAdapter, focusAdapter);
    }

    private void attachDeferredActivationListeners(Component component, MouseAdapter mouseAdapter, FocusAdapter focusAdapter) {
        component.addMouseListener(mouseAdapter);
        component.addFocusListener(focusAdapter);
        if (component instanceof JComponent jComponent) {
            jComponent.setFocusable(true);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachDeferredActivationListeners(child, mouseAdapter, focusAdapter);
            }
        }
    }

    private void activateDeferredEditorIfNeeded() {
        if (!deferEditorInitialization || editorInitialized) {
            return;
        }
        ensureEditorInitialized();
    }

    /**
     * 添加监听器，表单内容变化时在tab标题显示红点
     */
    private void addDirtyListeners() {
        requestTabStateHelper.bindListeners(this::updateTabDirty);
    }

    /**
     * 更新所有tab的内容指示器
     */
    private void updateTabIndicators() {
        requestTabStateHelper.updateTabIndicators();
    }


    /**
     * 设置原始请求数据（脏数据检测）
     */
    public void setOriginalRequestItem(HttpRequestItem item) {
        if (!editorInitialized) {
            pendingOriginalRequestItem = item;
            pendingRequestItem = item;
            return;
        }
        requestDirtyStateHelper.setOriginalRequestItem(item);
        requestSettingsPanel.rebaseline();
    }

    public HttpRequestItem getOriginalRequestItem() {
        if (!editorInitialized) {
            return pendingOriginalRequestItem;
        }
        return requestDirtyStateHelper.getOriginalRequestItem();
    }

    /**
     * 判断当前表单内容是否被修改（与原始请求对比）
     * 注意：比较时排除 response 字段，因为它是历史响应数据，不属于表单编辑内容
     */
    public boolean isModified() {
        if (!editorInitialized) {
            return false;
        }
        return requestDirtyStateHelper.isModified();
    }

    /**
     * 从 tableModel 直接读取数据构建 HttpRequestItem，不调用 stopCellEditing。
     * 专供 isModified() / updateTabDirty() 等后台比较场景使用，
     * 避免在 TableModelListener 回调中打断用户正在进行的单元格编辑。
     */
    private HttpRequestItem getCurrentRequestFromModel() {
        if (!editorInitialized) {
            return pendingRequestItem != null ? pendingRequestItem : pendingOriginalRequestItem;
        }
        return requestFormDataHelper.buildCurrentRequest(id, name, currentProtocol, getOriginalRequestItem(), true);
    }

    /**
     * 检查脏状态并更新tab标题
     */
    private void updateTabDirty() {
        requestDirtyStateHelper.updateTabDirty();
    }

    private void sendRequest(ActionEvent e) {
        requestSendCoordinator.sendRequest();
    }

    private RequestPreparationResult prepareRequestForSending() {
        // 发送前只做“收集表单 + 构建请求 + 执行前置脚本 + 校验”，真正发协议请求交给 dispatch helper。
        ensureEditorInitialized();
        promotePreviewTabToPermanent();
        String settingsValidationError = validateRequestSettings();
        if (settingsValidationError != null) {
            return RequestPreparationResult.error(settingsValidationError);
        }
        HttpRequestItem item = getCurrentRequest();
        boolean useCache = getOriginalRequestItem() != null && !isModified();
        return requestPreparationService.prepare(item, useCache);
    }

    public String validateRequestSettings() {
        ensureEditorInitialized();
        return requestSettingsPanel.validateSettings();
    }

    private void promotePreviewTabToPermanent() {
        SwingUtilities.invokeLater(() -> {
            RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
            editPanel.promotePreviewTabToPermanent();
        });
    }

    // WebSocket消息发送逻辑
    private void sendWebSocketMessage() {
        requestEditorActionHelper.sendWebSocketMessage();
    }

    /**
     * 更新表单内容（用于切换请求或保存后刷新）
     */
    public void initPanelData(HttpRequestItem item) {
        if (!editorInitialized) {
            pendingRequestItem = item;
            pendingOriginalRequestItem = item;
            this.id = item.getId();
            this.name = item.getName();
            this.currentProtocol = item.getProtocol() != null ? item.getProtocol() : protocol;
            return;
        }
        populatePanelData(item, item);
    }

    private void populatePanelData(HttpRequestItem item, HttpRequestItem originalItem) {
        // 设置加载标志，防止加载过程中触发自动保存和URL/Params联动
        isLoadingData = true;

        try {
            this.id = item.getId();
            this.name = item.getName();
            this.currentProtocol = item.getProtocol() != null ? item.getProtocol() : protocol;
            requestFormDataHelper.populate(item);

            // 设置原始数据用于脏检测
            requestDirtyStateHelper.setOriginalRequestItem(originalItem);
            requestSettingsPanel.rebaseline();

            // 根据请求类型智能选择默认Tab
            requestFormDataHelper.selectDefaultTabByRequestType(getEffectiveProtocol(), item);
        } finally {
            // 确保标志一定会被清除，即使发生异常
            isLoadingData = false;
        }
        pendingRequestItem = item;
        pendingOriginalRequestItem = originalItem;
    }

    /**
     * 获取当前表单内容封装为HttpRequestItem
     */
    public HttpRequestItem getCurrentRequest() {
        if (!editorInitialized) {
            return pendingRequestItem != null ? pendingRequestItem : pendingOriginalRequestItem;
        }
        return requestFormDataHelper.buildCurrentRequest(id, name, currentProtocol, getOriginalRequestItem(), false);
    }

    /**
     * 解析url中的参数到paramsPanel，并与现有params合并去重
     */
    private void parseUrlParamsToParamsPanel() {
        requestUrlSyncHelper.syncUrlToParams(isLoadingData);
    }

    /**
     * 从Params面板同步更新到URL栏（类似Postman的双向联动）
     */
    private void parseParamsPanelToUrl() {
        requestUrlSyncHelper.syncParamsToUrl(isLoadingData);
    }

    // 取消当前请求
    private void cancelCurrentRequest() {
        requestEditorActionHelper.cancelCurrentRequest();
    }

    /**
     * 关闭标签页前释放网络资源，避免后台连接在 UI 被移除后继续存活。
     */
    public void disposeResources() {
        disposed = true;
        if (!editorInitialized) {
            pendingRequestItem = null;
            pendingOriginalRequestItem = null;
            return;
        }

        if (currentEventSource != null) {
            currentSseCancelled.set(true);
            currentEventSource.cancel();
            currentEventSource = null;
        }
        if (currentWebSocket != null) {
            currentWebSocket.close(WEBSOCKET_NORMAL_CLOSURE, "Tab closed");
            currentWebSocket = null;
        }
        currentWebSocketConnectionId = null;

        SwingWorker<Void, Void> worker = currentWorker;
        if (worker != null) {
            worker.cancel(true);
            currentWorker = null;
        }
        httpSseStreamOpened = false;
    }

    boolean isDisposed() {
        return disposed;
    }

    private void convertCurrentRequestToSse() {
        requestEditorActionHelper.convertCurrentRequestToSse();
    }


    /**
     * 如果urlField内容没有协议，自动补全 http:// 或 https:// 或 ws:// 或 wss://，根据 protocol 和用户配置判断
     */
    private void autoPrependHttpsIfNeeded() {
        requestEditorActionHelper.autoPrependProtocolIfNeeded();
    }

    /**
     * 显示保存响应对话框
     */
    private void saveResponseDialog() {
        if (lastResponse == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_NO_RESPONSE));
            return;
        }

        // 检查是否是临时请求（未保存的请求）
        if (getOriginalRequestItem() == null || getOriginalRequestItem().isNewRequest()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_REQUEST_NOT_SAVED));
            return;
        }

        String name = savedResponseHelper.promptResponseName(this);

        if (name != null && !name.trim().isEmpty()) {
            savedResponseHelper.saveResponse(name.trim(), lastRequest, lastResponse, getOriginalRequestItem());
        }
    }

    /**
     * 加载保存的响应到面板
     */
    public void loadSavedResponse(SavedResponse savedResponse) {
        if (savedResponse == null) return;
        ensureEditorInitialized();

        try {
            SavedResponse.OriginalRequest originalRequest = savedResponse.getOriginalRequest();
            if (originalRequest != null) {
                isLoadingData = true;
                try {
                    requestFormDataHelper.populateSavedResponseRequest(originalRequest);
                    requestFormDataHelper.selectDefaultTabBySavedResponse(getEffectiveProtocol(), originalRequest);
                } finally {
                    isLoadingData = false;
                }
            }

            savedResponseHelper.displaySavedResponse(responsePanel, requestLinePanel, this::sendRequest, savedResponse);

        } catch (Exception ex) {
            log.error("加载保存的响应失败", ex);
            NotificationUtil.showError(I18nUtil.getMessage("加载响应失败: {0}", ex.getMessage()));
        }
    }

    /**
     * 动态更新布局方向（用于全局布局切换）
     *
     * @param isVertical true=垂直布局，false=水平布局
     */
    public void updateLayoutOrientation(boolean isVertical) {
        if (!editorInitialized) {
            pendingLayoutVertical = isVertical;
            return;
        }
        requestSplitLayoutHelper.updateLayoutOrientation(isVertical);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (editorInitialized && requestSplitLayoutHelper != null) {
            requestSplitLayoutHelper.handleInitialLayout();
        }
    }

    /**
     * 检测并解析 cURL 命令
     */
    private void detectAndParseCurl() {
        ensureEditorInitialized();
        requestEditorActionHelper.detectAndParseCurl(isLoadingData);
    }
}
