package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.model.HttpRequestItem;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.editor.CollectionTreeEditorGateway;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.editor.request.sub.*;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 单个请求编辑子面板，包含 URL、方法选择、Headers、Body 和响应展示
 */
public class RequestEditSubPanel extends JPanel {
    // 常量定义
    private static final int MAX_REDIRECT_COUNT = 10;

    private JTextField urlField;
    private JComboBox<String> methodBox;
    private EasyRequestParamsPanel paramsPanel;
    private EasyRequestHttpHeadersPanel headersPanel;
    private final RequestEditorDataController dataController;
    private final RequestItemProtocolEnum protocol;
    private final boolean deferEditorInitialization;
    private boolean editorInitialized;
    private Boolean pendingLayoutVertical;
    private final transient RequestEditorDeferredInitializationController deferredInitializationController;

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
    private final RequestPreparationFeedbackHelper requestPreparationFeedbackHelper = new RequestPreparationFeedbackHelper();
    private final RequestEditorSendPreparationController sendPreparationController =
            RequestEditorSendPreparationController.createDefault(
                    this::ensureEditorInitialized,
                    this::promotePreviewTabToPermanent,
                    () -> requestSettingsPanel.validateSettings(),
                    this::getCurrentRequest,
                    this::getOriginalRequestItem,
                    this::isModified
            );
    private RequestEditorBinder requestEditorBinder;
    private RequestEditorDefaultTabSelector requestEditorDefaultTabSelector;
    private RequestEditorTabsVisibilityController requestEditorTabsVisibilityController;
    private RequestTabStateHelper requestTabStateHelper;
    private RequestUrlSyncHelper requestUrlSyncHelper;
    private RequestEditorRuntimeController runtimeController;
    private final SavedResponseHelper savedResponseHelper = new SavedResponseHelper(new CollectionTreeEditorGateway());
    private RequestSavedResponseController savedResponseController;

    // Tab indicators for showing content status
    private IndicatorTabComponent paramsTabIndicator;
    private IndicatorTabComponent authTabIndicator;
    private IndicatorTabComponent headersTabIndicator;
    private IndicatorTabComponent bodyTabIndicator;
    private IndicatorTabComponent settingsTabIndicator;
    private IndicatorTabComponent scriptsTabIndicator;

    JSplitPane splitPane;
    // 数据加载标志，防止加载时触发自动保存和联动更新
    private boolean isLoadingData = false;
    private ResponsePanel responsePanel;

    // 保存最后一次请求和响应，用于保存响应功能
    private PreparedRequest lastRequest;
    private HttpResponse lastResponse;

    private RequestItemProtocolEnum getEffectiveProtocol() {
        return dataController.effectiveProtocol();
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

    private boolean isPerformanceSnapshot() {
        return panelType == RequestEditSubPanelType.PERFORMANCE_SNAPSHOT;
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

    public static RequestEditSubPanel performanceSnapshot(String id,
                                                          RequestItemProtocolEnum protocol,
                                                          boolean deferEditorInitialization) {
        return new RequestEditSubPanel(
                id,
                protocol,
                RequestEditSubPanelType.PERFORMANCE_SNAPSHOT,
                null,
                deferEditorInitialization
        );
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
        this.protocol = protocol;
        this.panelType = panelType;
        this.savedResponse = savedResponse;
        this.deferEditorInitialization = deferEditorInitialization && panelType != RequestEditSubPanelType.SAVED_RESPONSE;
        this.dataController = new RequestEditorDataController(
                id,
                protocol,
                () -> editorInitialized,
                this::isPerformanceSnapshot,
                loading -> isLoadingData = loading,
                this::updateTabIndicators
        );
        this.deferredInitializationController = new RequestEditorDeferredInitializationController(
                this,
                () -> this.deferEditorInitialization && !editorInitialized,
                () -> ensureEditorInitialized(true)
        );
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 设置边距为5
        setOpaque(true);
        setBackground(ModernColors.getBackgroundColor());
        if (!this.deferEditorInitialization) {
            initializeEditorUiIfNeeded();
        } else {
            deferredInitializationController.installPlaceholder();
        }
    }

    public void ensureEditorInitialized() {
        ensureEditorInitialized(false);
    }

    public void ensureEditorInitialized(boolean animatePlaceholderTransition) {
        // 占位页升级为真实编辑器时允许做局部淡出，但过渡层只能是 layeredPane 上的纯绘制快照。
        // 这样既保留平滑感，也不影响底层 JSplitPane 的 hover / resize cursor。
        if (editorInitialized) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            initializeEditorUiIfNeeded(animatePlaceholderTransition);
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> initializeEditorUiIfNeeded(animatePlaceholderTransition));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize RequestEditSubPanel on EDT", e);
        }
    }

    public boolean isEditorInitialized() {
        return editorInitialized;
    }

    public String getId() {
        return dataController.requestId();
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
        initializeEditorUiIfNeeded(false);
    }

    private void initializeEditorUiIfNeeded(boolean animatePlaceholderTransition) {
        if (editorInitialized) {
            return;
        }
        var placeholderSnapshot = animatePlaceholderTransition
                ? deferredInitializationController.capturePlaceholderSnapshot()
                : null;
        removeAll();
        // 先集中创建视图组件，再把交互逻辑按职责注入 helper，构造阶段就能看清依赖方向。
        RequestViewComponents components = RequestViewFactory.create(protocol, panelType, this::sendRequest);
        installViewComponents(components);
        createEditorBindingHelpers();
        bindBasicEditorInteractions();

        if (!isPerformanceSnapshot()) {
            createInteractiveRequestHelpers();
        } else {
            RequestEditorReadOnlyMode.apply(components);
        }

        finishEditorUiInitialization(components);

        dataController.populatePendingRequestIfPresent();
        if (pendingLayoutVertical != null) {
            if (runtimeController != null) {
                runtimeController.updateLayoutOrientation(pendingLayoutVertical);
            }
            pendingLayoutVertical = null;
        }
        revalidate();
        repaint();
        deferredInitializationController.startTransition(placeholderSnapshot);
    }

    private void installViewComponents(RequestViewComponents components) {
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

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(requestLinePanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
    }

    private void bindBasicEditorInteractions() {
        if (!isPerformanceSnapshot()) {
            RequestUiSetupHelper.bindUrlField(
                    urlField,
                    requestPreparationFeedbackHelper,
                    this::detectAndParseCurl,
                    () -> requestUrlSyncHelper.syncUrlToParams(isLoadingData),
                    this::autoPrependHttpsIfNeeded
            );
            RequestUiSetupHelper.bindParamsSync(paramsPanel, () -> requestUrlSyncHelper.syncParamsToUrl(isLoadingData));
        }
    }

    private void createEditorBindingHelpers() {
        requestUrlSyncHelper = new RequestUrlSyncHelper(urlField, paramsPanel);
        requestEditorBinder = new RequestEditorBinder(
                urlField,
                methodBox,
                paramsPanel,
                headersPanel,
                requestBodyPanel,
                requestSettingsPanel,
                authTabPanel,
                scriptPanel,
                descriptionEditor
        );
        requestEditorDefaultTabSelector = new RequestEditorDefaultTabSelector(
                reqTabs,
                requestBodyPanel,
                paramsPanel
        );
        dataController.bindEditor(
                requestEditorBinder,
                requestEditorDefaultTabSelector,
                this::dirtyStateHelper,
                requestSettingsPanel
        );
        requestEditorTabsVisibilityController = new RequestEditorTabsVisibilityController(
                protocol,
                reqTabs,
                descriptionEditor,
                paramsPanel,
                paramsTabIndicator,
                authTabPanel,
                authTabIndicator,
                headersPanel,
                headersTabIndicator,
                requestBodyPanel,
                bodyTabIndicator,
                scriptPanel,
                scriptsTabIndicator,
                requestSettingsPanel,
                settingsTabIndicator,
                this::updateTabIndicators
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
    }

    private void createInteractiveRequestHelpers() {
        runtimeController = RequestEditorRuntimeController.create(new RequestEditorRuntimeController.Config(
                this,
                urlField,
                headersPanel,
                requestBodyPanel,
                requestLinePanel,
                reqTabs,
                responsePanel,
                splitPane,
                requestPreparationFeedbackHelper,
                sendPreparationController::prepareForSending,
                this::sendRequest,
                this::isBaseHttpProtocol,
                this::isEffectiveHttpProtocol,
                this::isEffectiveSseProtocol,
                this::isEffectiveWebSocketProtocol,
                this::getEffectiveProtocol,
                dataController::setCurrentProtocol,
                newProtocol -> UiSingletonFactory.getInstance(RequestEditorPanel.class).updateTabProtocol(this, newProtocol),
                this::initPanelData,
                this::getCurrentRequestFromModel,
                dirty -> UiSingletonFactory.getInstance(RequestEditorPanel.class).updateTabDirty(this, dirty),
                (request, response) -> {
                    lastRequest = request;
                    lastResponse = response;
                },
                this::updateTabDirty,
                MAX_REDIRECT_COUNT
        ));
    }

    private void finishEditorUiInitialization(RequestViewComponents components) {
        add(components.editorContent, BorderLayout.CENTER);
        RequestUiSetupHelper.applyInitialProtocolUi(
                protocol,
                reqTabs,
                requestBodyPanel,
                paramsPanel,
                authTabPanel,
                e -> sendWebSocketMessage()
        );
        if (!isPerformanceSnapshot()) {
            addDirtyListeners();
        }

        SwingUtilities.invokeLater(this::updateTabIndicators);
        RequestUiSetupHelper.bindSaveResponseButton(protocol, panelType, responsePanel, e -> saveResponseDialog());
        if (!isPerformanceSnapshot()) {
            RequestUiSetupHelper.bindBodyTypeHeaderSync(requestBodyPanel, headersPanel, () -> isLoadingData);
        }
        editorInitialized = true;
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
        dataController.setOriginalRequestItem(item);
    }

    public HttpRequestItem getOriginalRequestItem() {
        return dataController.originalRequestItem();
    }

    /**
     * 判断当前表单内容是否被修改（与原始请求对比）
     * 注意：比较时排除 response 字段，因为它是历史响应数据，不属于表单编辑内容
     */
    public boolean isModified() {
        return dataController.isModified();
    }

    /**
     * 从 tableModel 直接读取数据构建 HttpRequestItem，不调用 stopCellEditing。
     * 专供 isModified() / updateTabDirty() 等后台比较场景使用，
     * 避免在 TableModelListener 回调中打断用户正在进行的单元格编辑。
     */
    private HttpRequestItem getCurrentRequestFromModel() {
        return dataController.currentRequestFromModel();
    }

    /**
     * 检查脏状态并更新tab标题
     */
    private void updateTabDirty() {
        if (runtimeController == null) {
            return;
        }
        runtimeController.updateTabDirty();
    }

    private RequestDirtyStateHelper dirtyStateHelper() {
        return runtimeController != null ? runtimeController.dirtyStateHelper() : null;
    }

    private void sendRequest(ActionEvent e) {
        if (runtimeController == null) {
            return;
        }
        runtimeController.sendRequest();
    }

    public String validateRequestSettings() {
        return sendPreparationController.validateRequestSettings();
    }

    private void promotePreviewTabToPermanent() {
        SwingUtilities.invokeLater(() -> {
            RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
            editPanel.promotePreviewTabToPermanent();
        });
    }

    // WebSocket消息发送逻辑
    private void sendWebSocketMessage() {
        if (runtimeController != null) {
            runtimeController.sendWebSocketMessage();
        }
    }

    /**
     * 更新表单内容（用于切换请求或保存后刷新）
     */
    public void initPanelData(HttpRequestItem item) {
        dataController.initPanelData(item);
    }

    /**
     * 获取当前表单内容封装为HttpRequestItem
     */
    public HttpRequestItem getCurrentRequest() {
        return dataController.currentRequest();
    }

    // 取消当前请求
    private void cancelCurrentRequest() {
        if (runtimeController == null) {
            return;
        }
        runtimeController.cancelCurrentRequest();
    }

    /**
     * 关闭标签页前释放网络资源，避免后台连接在 UI 被移除后继续存活。
     */
    public void disposeResources() {
        if (runtimeController != null) {
            runtimeController.disposeOpenConnections();
        }
        if (!editorInitialized) {
            dataController.clearPending();
            return;
        }
    }

    boolean isDisposed() {
        return runtimeController != null && runtimeController.isDisposed();
    }

    private void convertCurrentRequestToSse() {
        if (runtimeController == null) {
            return;
        }
        runtimeController.convertCurrentRequestToSse();
    }


    /**
     * 如果urlField内容没有协议，自动补全 http:// 或 https:// 或 ws:// 或 wss://，根据 protocol 和用户配置判断
     */
    private void autoPrependHttpsIfNeeded() {
        if (runtimeController == null) {
            return;
        }
        runtimeController.autoPrependProtocolIfNeeded();
    }

    /**
     * 显示保存响应对话框
     */
    private void saveResponseDialog() {
        getSavedResponseController().showSaveDialog();
    }

    private RequestSavedResponseController getSavedResponseController() {
        if (savedResponseController == null) {
            savedResponseController = new RequestSavedResponseController(
                    this,
                    savedResponseHelper,
                    () -> lastRequest,
                    () -> lastResponse,
                    this::getOriginalRequestItem,
                    requestEditorBinder,
                    requestEditorDefaultTabSelector,
                    this::getEffectiveProtocol,
                    loading -> isLoadingData = loading,
                    responsePanel,
                    requestLinePanel,
                    this::sendRequest
            );
        }
        return savedResponseController;
    }

    /**
     * 加载保存的响应到面板
     */
    public void loadSavedResponse(SavedResponse savedResponse) {
        if (savedResponse == null) {
            return;
        }
        ensureEditorInitialized();
        getSavedResponseController().loadSavedResponse(savedResponse);
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
        if (runtimeController == null) {
            return;
        }
        runtimeController.updateLayoutOrientation(isVertical);
    }

    public void updateRequestEditorTabsVisibility() {
        if (!editorInitialized || requestEditorTabsVisibilityController == null) {
            return;
        }
        requestEditorTabsVisibilityController.updateVisibility();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (editorInitialized && runtimeController != null) {
            runtimeController.handleInitialLayout();
        }
    }

    /**
     * 检测并解析 cURL 命令
     */
    private void detectAndParseCurl() {
        ensureEditorInitialized();
        if (runtimeController != null) {
            runtimeController.detectAndParseCurl(isLoadingData);
        }
    }
}
