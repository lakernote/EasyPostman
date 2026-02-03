package com.laker.postman.panel.collections.right.request;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.MarkdownEditorPanel;
import com.laker.postman.common.component.table.EasyPostmanFormDataTablePanel;
import com.laker.postman.common.component.table.EasyPostmanFormUrlencodedTablePanel;
import com.laker.postman.common.exception.DownloadCancelledException;
import com.laker.postman.model.*;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.sub.*;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.http.RedirectHandler;
import com.laker.postman.service.http.sse.SseEventListener;
import com.laker.postman.service.http.sse.SseResEventListener;
import com.laker.postman.service.http.sse.SseUiCallback;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSource;
import okio.ByteString;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.InterruptedIOException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static com.laker.postman.service.http.HttpUtil.validateRequest;


/**
 * 单个请求编辑子面板，包含 URL、方法选择、Headers、Body 和响应展示
 */
@Slf4j
public class RequestEditSubPanel extends JPanel {
    // 常量定义
    private static final int MAX_REDIRECT_COUNT = 10;
    private static final int WEBSOCKET_NORMAL_CLOSURE = 1000;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextField urlField;
    private final JComboBox<String> methodBox;
    private final EasyRequestParamsPanel paramsPanel;
    private final EasyRequestHttpHeadersPanel headersPanel;
    @Getter
    private String id;
    private String name;
    private final RequestItemProtocolEnum protocol;

    // 面板类型
    @Getter
    private final RequestEditSubPanelType panelType;

    // 如果是 SAVED_RESPONSE 类型，保存关联的 savedResponse 和 parentRequest
    @Getter
    private final SavedResponse savedResponse;

    @Getter
    private final RequestLinePanel requestLinePanel;
    //  RequestBodyPanel
    private final RequestBodyPanel requestBodyPanel;
    @Getter
    private HttpRequestItem originalRequestItem;
    private final AuthTabPanel authTabPanel;
    private final ScriptPanel scriptPanel;
    private final MarkdownEditorPanel descriptionEditor; // Docs tab
    private final JTabbedPane reqTabs; // 请求选项卡面板

    // 当前请求的 SwingWorker，用于支持取消
    private transient volatile SwingWorker<Void, Void> currentWorker;
    // 当前 SSE 事件源, 用于取消 SSE 请求
    private transient volatile EventSource currentEventSource;
    // WebSocket连接对象
    private transient volatile WebSocket currentWebSocket;
    // WebSocket连接ID，用于防止过期连接的回调
    private volatile String currentWebSocketConnectionId;
    JSplitPane splitPane;
    // 标记是否已经设置过初始分割位置（用于水平布局的 5:5 分割）
    private boolean initialDividerLocationSet = false;
    // 双向联动控制标志，防止循环更新
    private boolean isUpdatingFromUrl = false;
    private boolean isUpdatingFromParams = false;
    // 数据加载标志，防止加载时触发自动保存和联动更新
    private boolean isLoadingData = false;
    @Getter
    private final ResponsePanel responsePanel;

    // 保存最后一次请求和响应，用于保存响应功能
    private PreparedRequest lastRequest;
    private HttpResponse lastResponse;

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
        this(id, protocol, RequestEditSubPanelType.NORMAL, null);
    }

    /**
     * 保存的响应面板构造函数
     */
    public RequestEditSubPanel(SavedResponse savedResponse) {
        this(savedResponse.getId(), RequestItemProtocolEnum.HTTP, RequestEditSubPanelType.SAVED_RESPONSE, savedResponse);
    }

    /**
     * 完整构造函数
     */
    private RequestEditSubPanel(String id, RequestItemProtocolEnum protocol, RequestEditSubPanelType panelType,
                                SavedResponse savedResponse) {
        this.id = id;
        this.protocol = protocol;
        this.panelType = panelType;
        this.savedResponse = savedResponse;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 设置边距为5
        // 1. 顶部请求行面板
        requestLinePanel = new RequestLinePanel(this::sendRequest, protocol);
        methodBox = requestLinePanel.getMethodBox();
        urlField = requestLinePanel.getUrlField();
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                // 优先检测 cURL 命令
                detectAndParseCurl();
                // 然后处理 URL 参数解析
                parseUrlParamsToParamsPanel();
            }

            public void removeUpdate(DocumentEvent e) {
                parseUrlParamsToParamsPanel();
            }

            public void changedUpdate(DocumentEvent e) {
                detectAndParseCurl();
                parseUrlParamsToParamsPanel();
            }
        });
        // 自动补全URL协议
        urlField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                autoPrependHttpsIfNeeded();
            }
        });
        urlField.addActionListener(e -> autoPrependHttpsIfNeeded());
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(requestLinePanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // 创建请求选项卡面板
        reqTabs = new JTabbedPane(); // 2. 创建请求选项卡面板
        reqTabs.setMinimumSize(new Dimension(400, 120));

        // 2.0 Docs Tab - 放在第一个，像 Postman 一样
        descriptionEditor = new MarkdownEditorPanel();

        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.REQUEST_DOCS_TAB_TITLE), descriptionEditor);

        // 2.1 Params
        paramsPanel = new EasyRequestParamsPanel();
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_PARAMS), paramsPanel); // 2.1 添加参数选项卡

        // 添加Params面板的监听器，实现从Params到URL的联动
        paramsPanel.addTableModelListener(e -> {
            if (!isUpdatingFromUrl && !isLoadingData) {
                parseParamsPanelToUrl();
            }
        });

        // 2.2 Auth 面板
        authTabPanel = new AuthTabPanel();
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION), authTabPanel);

        // 2.3 Headers
        headersPanel = new EasyRequestHttpHeadersPanel();
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_HEADERS), headersPanel);

        // 2.4 Body 面板
        requestBodyPanel = new RequestBodyPanel(protocol);
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_BODY), requestBodyPanel);


        // 2.5 脚本Tab
        scriptPanel = new ScriptPanel();
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS), scriptPanel);

        // 3. 响应面板
        // 只有 HTTP 协议且非 SAVED_RESPONSE 类型才启用保存响应按钮
        boolean enableSaveButton = protocol.isHttpProtocol() && panelType != RequestEditSubPanelType.SAVED_RESPONSE;
        responsePanel = new ResponsePanel(protocol, enableSaveButton);

        // 根据保存的设置初始化布局方向
        boolean isVertical = SettingManager.isLayoutVertical();
        int orientation = isVertical ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;

        // 水平布局时，设置 responsePanel 的 minimumSize 与 reqTabs 对称（reqTabs 在第 172 行已设置）
        if (!isVertical) {
            responsePanel.setMinimumSize(new Dimension(400, 120));
        }

        splitPane = new JSplitPane(orientation, reqTabs, responsePanel);
        splitPane.setDividerSize(3); // 设置分割条宽度
        splitPane.setOneTouchExpandable(false);
        splitPane.setContinuousLayout(true); // 连续布局，拖动时更流畅

        // 根据布局方向设置比例
        double initialRatio;
        if (isVertical) {
            // 垂直布局：使用协议默认比例
            initialRatio = getDefaultResizeWeight();
        } else {
            // 水平布局：固定对半分
            initialRatio = 0.5;
        }
        splitPane.setResizeWeight(initialRatio);


        add(splitPane, BorderLayout.CENTER);


        if (protocol.isWebSocketProtocol()) {
            // WebSocket消息发送按钮事件绑定（只绑定一次）
            requestBodyPanel.setWsSendActionListener(e -> sendWebSocketMessage());
            // 切换到WebSocket协议时，默认选中Body Tab
            reqTabs.setSelectedComponent(requestBodyPanel);
            // 隐藏认证tab
            reqTabs.remove(authTabPanel);
            // 初始时禁用发送和定时按钮，只有连接后才可用
            requestBodyPanel.setWebSocketConnected(false);
        } else if (protocol.isSseProtocol()) {
            // SSE协议：默认选中Params Tab
            reqTabs.setSelectedComponent(paramsPanel);
            // 隐藏认证tab
            reqTabs.remove(authTabPanel);
        } else {
            // HTTP协议：默认选中Params Tab（避免默认显示Docs tab）
            reqTabs.setSelectedComponent(paramsPanel);
        }
        // 监听表单内容变化，动态更新tab红点
        addDirtyListeners();

        // 添加保存响应按钮监听器（仅HTTP协议且非保存的响应面板）
        if (protocol.isHttpProtocol() && panelType != RequestEditSubPanelType.SAVED_RESPONSE
                && responsePanel.getSaveResponseButton() != null) {
            responsePanel.getSaveResponseButton().addActionListener(e -> saveResponseDialog());
        }

        // bodyTypeComboBox 变化时，自动设置 Content-Type
        requestBodyPanel.getBodyTypeComboBox().addActionListener(e -> {
            // 如果正在加载数据，不自动修改 Content-Type（避免覆盖从文件解析来的 header）
            if (isLoadingData) {
                return;
            }

            String selectedType = (String) requestBodyPanel.getBodyTypeComboBox().getSelectedItem();
            if (RequestBodyPanel.BODY_TYPE_NONE.equals(selectedType)) {
                headersPanel.removeHeader("Content-Type");
            } else {
                String contentType = null;
                if (RequestBodyPanel.BODY_TYPE_RAW.equals(selectedType)) {
                    contentType = "application/json";
                } else if (RequestBodyPanel.BODY_TYPE_FORM_URLENCODED.equals(selectedType)) {
                    contentType = "application/x-www-form-urlencoded";
                } else if (RequestBodyPanel.BODY_TYPE_FORM_DATA.equals(selectedType)) {
                    contentType = "multipart/form-data";
                }
                if (contentType != null) {
                    headersPanel.setOrUpdateHeader("Content-Type", contentType);
                }
            }
        });
    }

    /**
     * 添加监听器，表单内容变化时在tab标题显示红点
     */
    private void addDirtyListeners() {
        // 监听urlField
        addDocumentListener(urlField.getDocument());
        // 监听methodBox
        methodBox.addActionListener(e -> updateTabDirty());
        // 监听descriptionEditor
        descriptionEditor.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateTabDirty();
            }

            public void removeUpdate(DocumentEvent e) {
                updateTabDirty();
            }

            public void changedUpdate(DocumentEvent e) {
                updateTabDirty();
            }
        });
        // 监听headersPanel
        headersPanel.addTableModelListener(e -> updateTabDirty());
        // 监听paramsPanel
        paramsPanel.addTableModelListener(e -> updateTabDirty());
        // 监听认证面板
        authTabPanel.addDirtyListener(this::updateTabDirty);
        if (protocol.isHttpProtocol()) {
            // 监听bodyArea
            if (requestBodyPanel.getBodyArea() != null) {
                addDocumentListener(requestBodyPanel.getBodyArea().getDocument());
            }
            if (requestBodyPanel.getFormDataTablePanel() != null) {
                requestBodyPanel.getFormDataTablePanel().addTableModelListener(e -> updateTabDirty());

            }
            if (requestBodyPanel.getFormUrlencodedTablePanel() != null) {
                requestBodyPanel.getFormUrlencodedTablePanel().addTableModelListener(e -> updateTabDirty());
            }
        }
        // 监听脚本面板
        scriptPanel.addDirtyListeners(this::updateTabDirty);
    }

    private void addDocumentListener(Document document) {
        document.addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateTabDirty();
            }

            public void removeUpdate(DocumentEvent e) {
                updateTabDirty();
            }

            public void changedUpdate(DocumentEvent e) {
                updateTabDirty();
            }
        });
    }


    /**
     * 设置原始请求数据（脏数据检测）
     */
    public void setOriginalRequestItem(HttpRequestItem item) {
        if (item != null && !item.isNewRequest()) {
            // 深拷贝，避免引用同一对象导致脏检测失效
            this.originalRequestItem = JsonUtil.deepCopy(item, HttpRequestItem.class);
        } else {
            this.originalRequestItem = null;
        }
    }

    /**
     * 判断当前表单内容是否被修改（与原始请求对比）
     * 注意：比较时排除 response 字段，因为它是历史响应数据，不属于表单编辑内容
     */
    public boolean isModified() {
        if (originalRequestItem == null) return false;
        HttpRequestItem current = getCurrentRequest();

        // 使用字段级别比较，排除 response（优化性能，避免JSON序列化）
        boolean isModified = !equalsIgnoringResponse(originalRequestItem, current);

        if (isModified) {
            log.debug("Request form has been modified, Request Name: {}", current.getName());
        }
        return isModified;
    }

    /**
     * 比较两个 HttpRequestItem 是否相等（排除 response 字段）
     * 使用 JSON 序列化比较，忽略 response 字段的变化
     */
    private boolean equalsIgnoringResponse(HttpRequestItem ori, HttpRequestItem cur) {
        if (ori == cur) return true;
        if (ori == null || cur == null) return false;

        try {
            // 临时保存 response
            List<SavedResponse> oriSaved = ori.getResponse();
            List<SavedResponse> curSaved = cur.getResponse();

            try {
                // 将两个对象的 response 都设为 null，使其不参与比较
                ori.setResponse(null);
                cur.setResponse(null);

                // 通过 JSON 序列化进行深度比较
                String oriJson = JsonUtil.toJsonStr(ori);
                String curJson = JsonUtil.toJsonStr(cur);

                return oriJson.equals(curJson);
            } finally {
                // 恢复原始的 response
                ori.setResponse(oriSaved);
                cur.setResponse(curSaved);
            }
        } catch (Exception e) {
            log.error("比较请求时发生异常", e);
            return false;
        }
    }

    /**
     * 检查脏状态并更新tab标题
     */
    private void updateTabDirty() {
        SwingUtilities.invokeLater(() -> {
            if (originalRequestItem == null) return; // 如果没有原始请求数据，则不进行脏检测
            boolean dirty = isModified();
            SingletonFactory.getInstance(RequestEditPanel.class).updateTabDirty(this, dirty);
        });
    }

    private void sendRequest(ActionEvent e) {
        if (currentWorker != null) {
            cancelCurrentRequest();
            return;
        }

        // ===== 立即更新UI，提供即时反馈 =====
        updateUIForRequesting();

        // ===== 在后台线程执行所有耗时操作 =====
        SwingWorker<Void, Void> preparationWorker = new SwingWorker<>() {
            HttpRequestItem item;
            HttpRequestItem effectiveItem;
            PreparedRequest req;
            ScriptExecutionPipeline pipeline;
            String validationError = null;

            @Override
            protected Void doInBackground() {
                try {
                    // 发送请求时，如果当前是预览 tab，则转为固定 tab（模仿 Postman 行为）
                    SwingUtilities.invokeLater(() -> {
                        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
                        editPanel.promotePreviewTabToPermanent();
                    });

                    // 清理上次请求的临时变量
                    EnvironmentService.clearTemporaryVariables();

                    item = getCurrentRequest();

                    // 根据协议类型进行URL验证
                    String url = item.getUrl();
                    RequestItemProtocolEnum protocol = item.getProtocol();
                    if (protocol.isWebSocketProtocol()) {
                        // WebSocket只允许ws://或wss://协议
                        if (!url.toLowerCase().startsWith("ws://") && !url.toLowerCase().startsWith("wss://")) {
                            validationError = "WebSocket requests must use ws:// or wss:// protocol";
                            return null;
                        }
                    }

                    req = PreparedRequestBuilder.build(item, false);
                    effectiveItem = item; // 保持兼容性

                    // 创建脚本执行流水线（使用 req 中合并后的脚本）
                    pipeline = ScriptExecutionPipeline.builder()
                            .request(req)
                            .preScript(req.prescript)
                            .postScript(req.postscript)
                            .build();

                    // 执行前置脚本
                    ScriptExecutionResult preResult = pipeline.executePreScript();
                    if (!preResult.isSuccess()) {
                        // 显示前置脚本执行错误对话框
                        validationError = I18nUtil.getMessage(MessageKeys.SCRIPT_PRESCRIPT_EXECUTION_FAILED,
                                preResult.getErrorMessage());
                        return null;
                    }

                    // 前置脚本执行完成后，再进行变量替换
                    PreparedRequestBuilder.replaceVariablesAfterPreScript(req);

                    // 验证请求
                    if (!validateRequest(req, item)) {
                        validationError = "Request validation failed";
                        return null;
                    }

                } catch (Exception ex) {
                    log.error("Error preparing request: {}", ex.getMessage(), ex);
                    validationError = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                // 如果有验证错误，恢复UI状态并显示错误
                if (validationError != null) {
                    requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                    responsePanel.hideLoadingOverlay();

                    if (validationError.contains("WebSocket")) {
                        JOptionPane.showMessageDialog(RequestEditSubPanel.this,
                                validationError,
                                "Invalid URL Protocol", JOptionPane.WARNING_MESSAGE);
                    } else if (validationError.contains("prescript")) {
                        String errorTitle = I18nUtil.getMessage(MessageKeys.SCRIPT_PRESCRIPT_ERROR_TITLE);
                        JOptionPane.showMessageDialog(RequestEditSubPanel.this,
                                validationError,
                                errorTitle,
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        // 通用错误处理 - 确保用户能看到所有错误
                        JOptionPane.showMessageDialog(RequestEditSubPanel.this,
                                validationError,
                                "Request Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    return;
                }

                // 验证成功，根据协议分发请求
                RequestItemProtocolEnum protocol = item.getProtocol();
                if (protocol.isWebSocketProtocol()) {
                    handleWebSocketRequest(req, pipeline);
                } else if (protocol.isSseProtocol()) {
                    handleSseRequest(req, pipeline);
                } else {
                    handleHttpRequest(req, pipeline);
                }
            }
        };

        preparationWorker.execute();
    }


    // 普通HTTP请求处理
    private void handleHttpRequest(PreparedRequest req, ScriptExecutionPipeline pipeline) {
        currentWorker = new SwingWorker<>() {
            HttpResponse resp;

            @Override
            protected Void doInBackground() {
                try {
                    responsePanel.setResponseTabButtonsEnable(true);
                    responsePanel.switchTabButtonHttpOrSse("http");
                    resp = RedirectHandler.executeWithRedirects(req, MAX_REDIRECT_COUNT, new SseResEventListener() {
                        @Override
                        public void onOpen(HttpResponse response) {
                            SwingUtilities.invokeLater(() -> {
                                responsePanel.switchTabButtonHttpOrSse("sse");
                                updateUIForResponse(response);
                                // 添加连接成功消息
                                if (responsePanel.getSseResponsePanel() != null) {
                                    String timestamp = LocalTime.now().format(TIME_FORMATTER);
                                    responsePanel.getSseResponsePanel().addMessage(MessageType.CONNECTED, timestamp, "Connected to SSE stream", null);
                                }
                            });
                        }

                        @Override
                        public void onEvent(String id, String type, String data) {
                            SwingUtilities.invokeLater(() -> {
                                // 使用 SSEResponsePanel 来显示 SSE 消息
                                if (responsePanel.getSseResponsePanel() != null) {
                                    String timestamp = LocalTime.now().format(TIME_FORMATTER);
                                    List<TestResult> testResults = handleStreamMessage(pipeline, data);
                                    responsePanel.getSseResponsePanel().addMessage(MessageType.RECEIVED, timestamp, data, testResults);
                                }
                            });
                        }

                        @Override
                        public void onRetryChange(long l) {
                            // ignored
                        }
                    });
                } catch (DownloadCancelledException ex) {
                    // 用户主动取消下载，这是正常行为，不作为错误处理
                    log.info("User canceled download for request: {} {}", req.method, req.url);
                    // 不显示错误通知，因为这是用户的主动行为
                } catch (InterruptedIOException ex) {
                    log.warn("Request interrupted: {} {} - {}", req.method, req.url, ex.getMessage());
                } catch (Exception ex) {
                    log.error("Error executing HTTP request: {} {} - {}", req.method, req.url, ex.getMessage(), ex);
                    ConsolePanel.appendLog("[Error] " + ex.getMessage(), ConsolePanel.LogType.ERROR);
                    NotificationUtil.showError(ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                updateUIForResponse(resp);
                if (resp != null && !resp.isSse) {
                    handleResponse(pipeline, req, resp);
                }
                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                currentWorker = null;
            }
        };
        currentWorker.execute();
    }

    // SSE请求处理
    private void handleSseRequest(PreparedRequest req, ScriptExecutionPipeline pipeline) {
        currentWorker = new SwingWorker<>() {
            HttpResponse resp;
            StringBuilder sseBodyBuilder;
            long startTime;

            @Override
            protected Void doInBackground() {
                try {
                    startTime = System.currentTimeMillis();
                    resp = new HttpResponse();
                    sseBodyBuilder = new StringBuilder();
                    SseUiCallback callback = new SseUiCallback() {
                        @Override
                        public void onOpen(HttpResponse r, String headersText) {
                            SwingUtilities.invokeLater(() -> {
                                updateUIForResponse(r);
                                // 添加连接成功消息
                                if (responsePanel.getSseResponsePanel() != null) {
                                    String timestamp = LocalTime.now().format(TIME_FORMATTER);
                                    responsePanel.getSseResponsePanel().addMessage(MessageType.CONNECTED, timestamp, "Connected to SSE stream", null);
                                }
                            });
                        }

                        @Override
                        public void onEvent(String id, String type, String data) {
                            SwingUtilities.invokeLater(() -> {
                                // 使用 SSEResponsePanel 来显示 SSE 消息
                                if (responsePanel.getSseResponsePanel() != null) {
                                    String timestamp = LocalTime.now().format(TIME_FORMATTER);
                                    List<TestResult> testResults = handleStreamMessage(pipeline, data);
                                    responsePanel.getSseResponsePanel().addMessage(MessageType.RECEIVED, timestamp, data, testResults);
                                }
                            });
                        }

                        @Override
                        public void onClosed(HttpResponse r) {
                            SwingUtilities.invokeLater(() -> {
                                updateUIForResponse(r);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                                // 添加连接关闭消息
                                if (responsePanel.getSseResponsePanel() != null) {
                                    String timestamp = LocalTime.now().format(TIME_FORMATTER);
                                    responsePanel.getSseResponsePanel().addMessage(MessageType.CLOSED, timestamp, "SSE stream closed", null);
                                }
                                // 在UI线程内清理资源，确保线程安全
                                currentEventSource = null;
                                currentWorker = null;
                            });
                        }

                        @Override
                        public void onFailure(String errorMsg, HttpResponse r) {
                            SwingUtilities.invokeLater(() -> {
                                // 通过通知显示错误信息
                                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SSE_FAILED, errorMsg));

                                updateUIForResponse(r);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                                // 添加错误消息
                                if (responsePanel.getSseResponsePanel() != null) {
                                    String timestamp = LocalTime.now().format(TIME_FORMATTER);
                                    responsePanel.getSseResponsePanel().addMessage(MessageType.WARNING, timestamp, "Error: " + errorMsg, null);
                                }
                                // 在UI线程内清理资源，确保线程安全
                                currentEventSource = null;
                                currentWorker = null;
                            });
                        }
                    };
                    currentEventSource = HttpSingleRequestExecutor.executeSSE(req, new SseEventListener(callback, resp, sseBodyBuilder, startTime));
                    responsePanel.setResponseTabButtonsEnable(true); // 启用响应区的tab按钮
                } catch (Exception ex) {
                    log.error("Error executing SSE request: {} - {}", req.url, ex.getMessage(), ex);
                    SwingUtilities.invokeLater(() -> {
                        // 清空状态码，通过通知显示错误
                        responsePanel.setStatus(0);
                        NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SSE_ERROR, ex.getMessage()));
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                if (resp != null) {
                    SingletonFactory.getInstance(HistoryPanel.class).addRequestHistory(req, resp);
                }
            }
        };
        currentWorker.execute();
    }

    // WebSocket请求处理
    private void handleWebSocketRequest(PreparedRequest req, ScriptExecutionPipeline pipeline) {
        // 生成新的连接ID，用于识别当前有效连接
        final String connectionId = UUID.randomUUID().toString();
        currentWebSocketConnectionId = connectionId;

        currentWorker = new SwingWorker<>() {
            final HttpResponse resp = new HttpResponse();
            long startTime;
            volatile boolean closed = false;

            @Override
            protected Void doInBackground() {
                try {
                    startTime = System.currentTimeMillis();
                    // 在连接开始时记录连接状态日志
                    log.debug("Starting WebSocket connection with ID: {}", connectionId);

                    HttpSingleRequestExecutor.executeWebSocket(req, new WebSocketListener() {
                        @Override
                        public void onOpen(WebSocket webSocket, Response response) {
                            // 检查连接ID是否还有效，防止过期连接回调
                            if (!connectionId.equals(currentWebSocketConnectionId)) {
                                log.debug("Ignoring onOpen callback for expired connection ID: {}, current ID: {}",
                                        connectionId, currentWebSocketConnectionId);
                                // 关闭过期的连接
                                webSocket.close(WEBSOCKET_NORMAL_CLOSURE, "Connection expired");
                                return;
                            }

                            resp.headers = new LinkedHashMap<>();
                            for (String name : response.headers().names()) {
                                resp.addHeader(name, response.headers(name));
                            }
                            resp.code = response.code();
                            resp.protocol = response.protocol().toString();
                            currentWebSocket = webSocket;
                            SwingUtilities.invokeLater(() -> {
                                updateUIForResponse(resp);
                                reqTabs.setSelectedComponent(requestBodyPanel);
                                requestBodyPanel.getWsSendButton().requestFocusInWindow();
                                requestLinePanel.setSendButtonToClose(RequestEditSubPanel.this::sendRequest);
                                // 连接成功后启用发送和定时按钮
                                requestBodyPanel.setWebSocketConnected(true);
                            });
                            appendWebSocketMessage(MessageType.CONNECTED, response.message());
                        }

                        @Override
                        public void onMessage(okhttp3.WebSocket webSocket, String text) {
                            // 检查连接ID是否还有效
                            if (!connectionId.equals(currentWebSocketConnectionId)) {
                                log.debug("Ignoring onMessage callback for expired connection ID: {}", connectionId);
                                return;
                            }
                            appendWebSocketMessage(MessageType.RECEIVED, text, handleStreamMessage(pipeline, text));
                        }

                        @Override
                        public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
                            // 检查连接ID是否还有效
                            if (!connectionId.equals(currentWebSocketConnectionId)) {
                                log.debug("Ignoring onMessage(binary) callback for expired connection ID: {}", connectionId);
                                return;
                            }
                            appendWebSocketMessage(MessageType.BINARY, bytes.hex());
                        }

                        @Override
                        public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
                            // 检查连接ID是否还有效
                            if (isValidWebSocketConnection(connectionId)) {
                                log.debug("closing WebSocket: code={}, reason={}", code, reason);
                                handleWebSocketClose();
                            }
                        }

                        @Override
                        public void onClosed(WebSocket webSocket, int code, String reason) {
                            // 检查连接ID是否还有效
                            if (isValidWebSocketConnection(connectionId)) {
                                log.debug("closed WebSocket: code={}, reason={}", code, reason);
                                appendWebSocketMessage(MessageType.CLOSED, code + " " + reason);
                                handleWebSocketClose();
                            }
                        }

                        private void handleWebSocketClose() {
                            closed = true;
                            resp.costMs = System.currentTimeMillis() - startTime;
                            currentWebSocket = null;
                            SwingUtilities.invokeLater(() -> {
                                updateUIForResponse(resp);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                                // 断开后禁用发送和定时按钮
                                requestBodyPanel.setWebSocketConnected(false);
                            });
                        }

                        @Override
                        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                            // 检查连接ID是否还有效
                            if (!connectionId.equals(currentWebSocketConnectionId)) {
                                log.debug("Ignoring onFailure callback for expired connection ID: {}", connectionId);
                                return;
                            }
                            log.error("WebSocket error", t);
                            appendWebSocketMessage(MessageType.WARNING, t.getMessage());
                            closed = true;
                            resp.costMs = System.currentTimeMillis() - startTime;
                            SwingUtilities.invokeLater(() -> {
                                // 通过通知显示错误信息
                                String errorMsg = response != null ?
                                        I18nUtil.getMessage(MessageKeys.WEBSOCKET_FAILED, t.getMessage() + " (" + response.code() + ")") :
                                        I18nUtil.getMessage(MessageKeys.WEBSOCKET_FAILED, t.getMessage());
                                NotificationUtil.showError(errorMsg);

                                updateUIForResponse(resp);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                                // 失败后禁用发送和定时按钮
                                requestBodyPanel.setWebSocketConnected(false);
                            });
                        }
                    });
                    responsePanel.setResponseTabButtonsEnable(true); // 启用响应区的tab按钮
                } catch (Exception ex) {
                    log.error("Error executing WebSocket request: {} - {}", req.url, ex.getMessage(), ex);
                    SwingUtilities.invokeLater(() -> {
                        // 清空状态码，通过通知显示错误
                        responsePanel.setStatus(0);
                        NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.WEBSOCKET_ERROR, ex.getMessage()));
                        // 失败后禁用发送和定时按钮
                        requestBodyPanel.setWebSocketConnected(false);
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                // 只有当前有效连接才记录历史
                if (connectionId.equals(currentWebSocketConnectionId)) {
                    SingletonFactory.getInstance(HistoryPanel.class).addRequestHistory(req, resp);
                }
            }
        };
        currentWorker.execute();
    }

    // WebSocket消息发送逻辑
    private void sendWebSocketMessage() {

        if (currentWebSocket == null) {
            appendWebSocketMessage(MessageType.INFO, I18nUtil.getMessage(MessageKeys.WEBSOCKET_NOT_CONNECTED));
            return;
        }

        String msg = requestBodyPanel.getRawBody();
        if (CharSequenceUtil.isNotBlank(msg)) {
            currentWebSocket.send(msg); // 发送消息
            appendWebSocketMessage(MessageType.SENT, msg);
        }
    }

    private void appendWebSocketMessage(MessageType type, String text) {
        appendWebSocketMessage(type, text, null);
    }


    private void appendWebSocketMessage(MessageType type, String text, List<TestResult> testResults) {
        if (responsePanel.getProtocol().isWebSocketProtocol() && responsePanel.getWebSocketResponsePanel() != null) {
            String timestamp = LocalTime.now().format(TIME_FORMATTER);
            responsePanel.getWebSocketResponsePanel().addMessage(type, timestamp, text, testResults);
        }
    }

    /**
     * 检查 WebSocket 连接是否有效
     *
     * @param connectionId 要检查的连接ID
     * @return 如果连接有效返回 true，否则返回 false
     */
    private boolean isValidWebSocketConnection(String connectionId) {
        return CharSequenceUtil.isBlank(currentWebSocketConnectionId) ||
                connectionId.equals(currentWebSocketConnectionId);
    }

    /**
     * 更新表单内容（用于切换请求或保存后刷新）
     */
    public void initPanelData(HttpRequestItem item) {
        // 设置加载标志，防止加载过程中触发自动保存和URL/Params联动
        isLoadingData = true;

        try {
            this.id = item.getId();
            this.name = item.getName();
            // 拆解URL参数
            String url = item.getUrl();
            urlField.setText(url);
            urlField.setCaretPosition(0); // 设置光标到开头

            if (CollUtil.isNotEmpty(item.getParamsList())) {
                paramsPanel.setParamsList(item.getParamsList());
            } else {
                // 没有数据，尝试从 URL 解析参数
                List<HttpParam> urlParams = HttpUtil.getParamsListFromUrl(url);
                if (!urlParams.isEmpty()) {
                    paramsPanel.setParamsList(urlParams);
                    item.setParamsList(paramsPanel.getParamsList());
                } else {
                    paramsPanel.clear();
                }
            }
            methodBox.setSelectedItem(item.getMethod());

            if (CollUtil.isNotEmpty(item.getHeadersList())) {
                // 有数据，使用请求的 headers
                headersPanel.setHeadersList(item.getHeadersList());
            } else {
                // 没有数据，初始化为空列表
                headersPanel.setHeadersList(new ArrayList<>());
            }
            // 获取最新的补充了默认值和排序的 headers 列表
            item.setHeadersList(headersPanel.getHeadersList());
            // Body
            requestBodyPanel.getBodyArea().setText(item.getBody());
            // 这是兼容性代码，防止旧数据bodyType字段为空
            if (CharSequenceUtil.isBlank(item.getBodyType())) {
                item.setBodyType(RequestBodyPanel.BODY_TYPE_NONE);
                // 只有支持 body 的请求方法，才根据 Content-Type 推断 bodyType
                // GET/HEAD/OPTIONS 等方法即使有 Content-Type header 也不推断 bodyType
                String method = item.getMethod();
                boolean supportsBody = "POST".equals(method) || "PUT".equals(method) ||
                        "PATCH".equals(method) || "DELETE".equals(method);

                if (supportsBody) {
                    // 根据请求headers尝试推断bodyType
                    String contentType = HttpUtil.getHeaderIgnoreCase(item, "Content-Type");
                    if (CharSequenceUtil.isNotBlank(contentType)) {
                        if (contentType.contains("application/x-www-form-urlencoded")) {
                            item.setBodyType(RequestBodyPanel.BODY_TYPE_FORM_URLENCODED);
                        } else if (contentType.contains("multipart/form-data")) {
                            item.setBodyType(RequestBodyPanel.BODY_TYPE_FORM_DATA);
                        } else {
                            item.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
                        }
                    }
                }
            }
            requestBodyPanel.getBodyTypeComboBox().setSelectedItem(item.getBodyType());
            // rawTypeComboBox 根据body内容智能设置
            String body = item.getBody();
            if (CharSequenceUtil.isNotBlank(body)) {
                JComboBox<String> rawTypeComboBox = requestBodyPanel.getRawTypeComboBox();
                if (rawTypeComboBox != null) {
                    if (JSONUtil.isTypeJSON(body)) {
                        rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_JSON);
                    } else if (XmlUtil.isXml(body)) {
                        rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_XML);
                    } else {
                        rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_TEXT);
                    }
                }
            }

            if (CollUtil.isNotEmpty(item.getFormDataList())) {
                EasyPostmanFormDataTablePanel formDataTablePanel = requestBodyPanel.getFormDataTablePanel();
                formDataTablePanel.setFormDataList(item.getFormDataList());
            }

            if (CollUtil.isNotEmpty(item.getUrlencodedList())) {
                EasyPostmanFormUrlencodedTablePanel urlencodedTablePanel = requestBodyPanel.getFormUrlencodedTablePanel();
                urlencodedTablePanel.setFormDataList(item.getUrlencodedList());
            }

            // 认证Tab
            authTabPanel.setAuthType(item.getAuthType());
            authTabPanel.setUsername(item.getAuthUsername());
            authTabPanel.setPassword(item.getAuthPassword());
            authTabPanel.setToken(item.getAuthToken());

            // 前置/后置脚本
            scriptPanel.setPrescript(item.getPrescript() == null ? "" : item.getPrescript());
            scriptPanel.setPostscript(item.getPostscript() == null ? "" : item.getPostscript());

            // 文档描述
            descriptionEditor.setText(item.getDescription() == null ? "" : item.getDescription());

            // 设置原始数据用于脏检测
            setOriginalRequestItem(item);

            // 根据请求类型智能选择默认Tab
            selectDefaultTabByRequestType(item);
        } finally {
            // 确保标志一定会被清除，即使发生异常
            isLoadingData = false;
        }
    }

    /**
     * 获取当前表单内容封装为HttpRequestItem
     */
    public HttpRequestItem getCurrentRequest() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(this.id); // 保证id不丢失
        item.setName(this.name); // 保证name不丢失
        item.setDescription(descriptionEditor.getText()); // 保存文档描述
        item.setUrl(urlField.getText().trim());
        item.setMethod((String) methodBox.getSelectedItem());
        item.setProtocol(protocol);
        item.setHeadersList(headersPanel.getHeadersList());
        item.setParamsList(paramsPanel.getParamsList());
        item.setBody(requestBodyPanel.getBodyArea().getText().trim());
        item.setBodyType(Objects.requireNonNull(requestBodyPanel.getBodyTypeComboBox().getSelectedItem()).toString());
        String bodyType = requestBodyPanel.getBodyType();
        if (RequestBodyPanel.BODY_TYPE_FORM_DATA.equals(bodyType)) {
            EasyPostmanFormDataTablePanel formDataTablePanel = requestBodyPanel.getFormDataTablePanel();
            item.setFormDataList(formDataTablePanel.getFormDataList());
            item.setBody(""); // form-data模式下，body通常不直接使用
            item.setUrlencodedList(new ArrayList<>());
        } else if (RequestBodyPanel.BODY_TYPE_FORM_URLENCODED.equals(bodyType)) {
            item.setBody(""); // x-www-form-urlencoded模式下，body通常不直接使用
            item.setFormDataList(new ArrayList<>());
            EasyPostmanFormUrlencodedTablePanel urlencodedTablePanel = requestBodyPanel.getFormUrlencodedTablePanel();
            item.setUrlencodedList(urlencodedTablePanel.getFormDataList());
        } else if (RequestBodyPanel.BODY_TYPE_RAW.equals(bodyType)) {
            item.setBody(requestBodyPanel.getRawBody());
            item.setFormDataList(new ArrayList<>());
            item.setUrlencodedList(new ArrayList<>());
        }
        // 认证Tab收集
        item.setAuthType(authTabPanel.getAuthType());
        item.setAuthUsername(authTabPanel.getUsername());
        item.setAuthPassword(authTabPanel.getPassword());
        item.setAuthToken(authTabPanel.getToken());
        // 脚本内容
        item.setPrescript(scriptPanel.getPrescript());
        item.setPostscript(scriptPanel.getPostscript());

        // 保留 response，避免在保存请求时丢失已保存的响应
        if (originalRequestItem != null && originalRequestItem.getResponse() != null) {
            item.setResponse(originalRequestItem.getResponse());
        }

        return item;
    }

    /**
     * 解析url中的参数到paramsPanel，并与现有params合并去重
     */
    private void parseUrlParamsToParamsPanel() {
        if (isUpdatingFromParams || isLoadingData) {
            return; // 如果正在从Params更新URL或正在加载数据，避免循环更新
        }

        isUpdatingFromUrl = true;
        try {
            String url = urlField.getText();
            List<HttpParam> urlParams = HttpUtil.getParamsListFromUrl(url);

            // 获取当前Params面板的参数
            List<HttpParam> currentParams = paramsPanel.getParamsList();

            // 检查URL参数和当前Params参数中enabled的是否一致
            // 注意：只比较enabled的参数，因为URL中不包含disabled的参数
            List<HttpParam> enabledCurrentParams = currentParams.stream()
                    .filter(HttpParam::isEnabled)
                    .toList();

            if (!paramsListEquals(urlParams, enabledCurrentParams)) {
                // URL中的参数与当前enabled的参数不一致，需要更新
                // 保留当前disabled的参数，添加URL中的新参数
                List<HttpParam> disabledParams = currentParams.stream()
                        .filter(p -> !p.isEnabled())
                        .toList();

                // 合并URL参数和disabled参数
                // 如果URL没有参数，只保留disabled的参数
                List<HttpParam> mergedParams = new ArrayList<>(urlParams);
                mergedParams.addAll(disabledParams);

                paramsPanel.setParamsList(mergedParams);
            }
        } finally {
            isUpdatingFromUrl = false;
        }
    }

    /**
     * 比较两个参数列表是否相等
     */
    private boolean paramsListEquals(List<HttpParam> list1, List<HttpParam> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        for (int i = 0; i < list1.size(); i++) {
            HttpParam p1 = list1.get(i);
            HttpParam p2 = list2.get(i);
            if (!Objects.equals(p1.getKey(), p2.getKey()) ||
                    !Objects.equals(p1.getValue(), p2.getValue()) ||
                    p1.isEnabled() != p2.isEnabled()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从Params面板同步更新到URL栏（类似Postman的双向联动）
     */
    private void parseParamsPanelToUrl() {
        if (isUpdatingFromUrl) {
            return; // 如果正在从URL更新Params，避免循环更新
        }

        isUpdatingFromParams = true;
        try {
            String currentUrl = urlField.getText().trim();
            String baseUrl = HttpUtil.getBaseUrlWithoutParams(currentUrl);

            if (baseUrl == null || baseUrl.isEmpty()) {
                return; // 没有基础URL，无法构建完整URL
            }

            // 获取Params面板的所有参数（包括enabled状态）
            List<HttpParam> params = paramsPanel.getParamsList();

            // 使用HttpUtil中的方法构建完整URL（只包含enabled的参数）
            String newUrl = HttpUtil.buildUrlFromParamsList(baseUrl, params);

            // 只有在URL真正发生变化时才更新
            if (!newUrl.equals(currentUrl)) {
                urlField.setText(newUrl);
                urlField.setCaretPosition(0); // 设置光标到开头
            }
        } finally {
            isUpdatingFromParams = false;
        }
    }

    // 取消当前请求
    private void cancelCurrentRequest() {
        if (currentEventSource != null) {
            currentEventSource.cancel(); // 取消SSE请求
            currentEventSource = null;
        }
        if (currentWebSocket != null) {
            currentWebSocket.close(1000, "User canceled"); // 关闭WebSocket连接
            currentWebSocket = null;
        }
        // 清空WebSocket连接ID，使过期的连接回调失效
        currentWebSocketConnectionId = null;

        currentWorker.cancel(true);
        requestLinePanel.setSendButtonToSend(this::sendRequest);
        currentWorker = null;

        // 隐藏加载遮罩
        responsePanel.hideLoadingOverlay();

        // 为WebSocket连接添加取消消息
        if (protocol.isWebSocketProtocol()) {
            appendWebSocketMessage(MessageType.WARNING, "User canceled");
        }
    }

    // UI状态：请求中
    private void updateUIForRequesting() {
        requestLinePanel.setSendButtonToCancel(this::sendRequest);

        if (protocol.isHttpProtocol()) {
            responsePanel.getNetworkLogPanel().clearLog();
            responsePanel.setResponseTabButtonsEnable(false);
            responsePanel.setResponseBodyEnabled(false);
        }
        responsePanel.clearAll();

        // 显示加载遮罩
        responsePanel.showLoadingOverlay();
    }

    // UI状态：响应完成
    private void updateUIForResponse(HttpResponse resp) {
        // 隐藏加载遮罩
        responsePanel.hideLoadingOverlay();

        if (resp == null) {
            // 响应为空，清空状态码（错误信息已通过异常处理和通知显示）
            responsePanel.setStatus(0);
            if (protocol.isHttpProtocol()) {
                responsePanel.setResponseBodyEnabled(true);
            }
            return;
        }

        // 有响应时，设置响应数据
        responsePanel.setResponseHeaders(resp);
        if (!protocol.isWebSocketProtocol() && !protocol.isSseProtocol()) {
            responsePanel.setTiming(resp);
            responsePanel.setResponseBody(resp);
            responsePanel.setResponseBodyEnabled(true);
        }
        responsePanel.setStatus(resp.code);
        responsePanel.setResponseTime(resp.costMs);
        responsePanel.setResponseSize(resp.bodySize, resp.httpEventInfo);
    }

    private void setTestResults(List<TestResult> testResults) {
        responsePanel.setTestResults(testResults);
    }

    // 处理响应、后置脚本、变量提取、历史
    private void handleResponse(ScriptExecutionPipeline pipeline, PreparedRequest req, HttpResponse resp) {
        if (resp == null) {
            log.error("Response is null, cannot handle response.");
            return;
        }

        // 保存最后一次请求和响应，用于保存响应功能
        this.lastRequest = req;
        this.lastResponse = resp;

        // 更新请求和响应详情面板
        responsePanel.setRequestDetails(req);
        responsePanel.setResponseDetails(resp);

        try {
            // 执行后置脚本（自动清空旧结果、添加响应绑定、收集新结果）
            ScriptExecutionResult postResult = pipeline.executePostScript(resp);
            if (!postResult.isSuccess()) {
                // 显示后置脚本执行错误对话框
                String errorMessage = I18nUtil.getMessage(MessageKeys.SCRIPT_POSTSCRIPT_EXECUTION_FAILED,
                        postResult.getErrorMessage());
                String errorTitle = I18nUtil.getMessage(MessageKeys.SCRIPT_POSTSCRIPT_ERROR_TITLE);
                JOptionPane.showMessageDialog(this,
                        errorMessage,
                        errorTitle,
                        JOptionPane.ERROR_MESSAGE);
            }
            setTestResults(postResult.getTestResults());
        } catch (Exception ex) {
            log.error("Error executing post-script: {}", ex.getMessage(), ex);
            ConsolePanel.appendLog("[Error] Post-script execution failed: " + ex.getMessage(), ConsolePanel.LogType.ERROR);
        }

        // 单独处理历史记录保存，避免历史记录失败影响整个响应处理
        try {
            SingletonFactory.getInstance(HistoryPanel.class).addRequestHistory(req, resp);
        } catch (Exception ex) {
            log.error("Error saving to history: {}", ex.getMessage(), ex);
            // 历史记录失败不应该中断用户流程，只记录日志
            ConsolePanel.appendLog("[Warning] Failed to save request to history: " + ex.getMessage(), ConsolePanel.LogType.WARN);
        }
    }

    /**
     * 如果urlField内容没有协议，自动补全 http:// 或 https:// 或 ws:// 或 wss://，根据 protocol 和用户配置判断
     */
    private void autoPrependHttpsIfNeeded() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) return;
        // 如果是环境变量占位符开头，直接返回
        if (url.startsWith("{{")) return;
        String lower = url.toLowerCase();
        if (!(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("ws://") || lower.startsWith("wss://"))) {
            if (protocol != null && protocol.isWebSocketProtocol()) {
                // WebSocket 协议：根据默认协议设置补全 ws:// 或 wss://
                String defaultProtocol = SettingManager.getDefaultProtocol();
                url = ("https".equals(defaultProtocol) ? "wss://" : "ws://") + url;
            } else {
                // HTTP 协议：根据默认协议设置补全 http:// 或 https://
                url = SettingManager.getDefaultProtocol() + "://" + url;
            }
            urlField.setText(url);
        }
    }

    private List<TestResult> handleStreamMessage(ScriptExecutionPipeline pipeline, String message) {
        HttpResponse resp = new HttpResponse();
        resp.body = message;
        resp.bodySize = message != null ? message.length() : 0;

        // 执行后置脚本（自动清空、添加响应绑定、收集结果）
        ScriptExecutionResult postResult = pipeline.executePostScript(resp);

        // 对于流式消息，如果后置脚本失败，仅记录日志，不显示对话框（避免频繁弹窗）
        if (!postResult.isSuccess()) {
            log.warn("Post-script execution failed for stream message: {}", postResult.getErrorMessage());
        }

        return postResult.getTestResults();
    }

    /**
     * 根据请求类型智能选择默认Tab
     * 优化用户体验，根据请求的特点自动切换到最相关的Tab
     */
    private void selectDefaultTabByRequestType(HttpRequestItem item) {
        if (item == null) {
            return;
        }

        selectDefaultTab(
                item.getMethod(),
                item.getBodyType(),
                item.getBody(),
                CollUtil.isNotEmpty(item.getFormDataList()) || CollUtil.isNotEmpty(item.getUrlencodedList()),
                CollUtil.isNotEmpty(item.getParamsList())
        );
    }

    /**
     * 通用的Tab选择逻辑
     *
     * @param method      HTTP方法
     * @param bodyType    Body类型
     * @param body        Body内容
     * @param hasFormData 是否有form-data或urlencoded数据
     * @param hasParams   是否有参数
     */
    private void selectDefaultTab(String method, String bodyType, String body, boolean hasFormData, boolean hasParams) {
        // WebSocket协议：默认选择Body Tab（用于发送消息）
        if (protocol.isWebSocketProtocol()) {
            reqTabs.setSelectedComponent(requestBodyPanel);
            return;
        }

        // SSE协议：默认选择Params Tab（SSE通常通过URL参数配置）
        if (protocol.isSseProtocol()) {
            reqTabs.setSelectedComponent(paramsPanel);
            return;
        }

        // HTTP协议智能判断
        // 1. 如果有Body内容（非空且非none类型），优先显示Body Tab
        if (CharSequenceUtil.isNotBlank(body) && !RequestBodyPanel.BODY_TYPE_NONE.equals(bodyType)) {
            reqTabs.setSelectedComponent(requestBodyPanel);
            return;
        }

        // 2. 如果有form-data或urlencoded数据，显示Body Tab
        if (hasFormData) {
            reqTabs.setSelectedComponent(requestBodyPanel);
            return;
        }

        // 3. POST/PUT/PATCH请求：智能判断
        // 如果没有任何Body数据，优先显示Params Tab（更符合实际使用场景）
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            // 如果有参数但没有Body数据，显示Params Tab
            if (hasParams) {
                reqTabs.setSelectedComponent(paramsPanel);
                return;
            }

            // 否则显示Body Tab（默认行为，方便用户输入请求体）
            reqTabs.setSelectedComponent(requestBodyPanel);
            return;
        }


        // 4. GET/DELETE/HEAD/OPTIONS等查询类请求：默认显示Params Tab
        if ("GET".equals(method) || "DELETE".equals(method)
                || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            reqTabs.setSelectedComponent(paramsPanel);
            return;
        }

        // 5. 默认情况：显示Params Tab（最常用）
        reqTabs.setSelectedComponent(paramsPanel);
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
        if (originalRequestItem == null || originalRequestItem.isNewRequest()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_REQUEST_NOT_SAVED));
            return;
        }

        // 默认名称：当前时间
        String defaultName = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

        String name = (String) JOptionPane.showInputDialog(
                this,
                I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_DIALOG_MESSAGE),
                I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_DIALOG_TITLE),
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                defaultName
        );

        if (name != null && !name.trim().isEmpty()) {
            saveResponse(name.trim());
        }
    }

    /**
     * 保存响应到树节点
     */
    private void saveResponse(String name) {
        try {
            // 创建 SavedResponse
            SavedResponse savedResponse = SavedResponse.fromRequestAndResponse(
                    name, lastRequest, lastResponse
            );

            // 在树中查找对应的请求节点
            RequestCollectionsLeftPanel leftPanel =
                    SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
            DefaultMutableTreeNode requestNode = findRequestNodeInTree(originalRequestItem);

            if (requestNode != null) {
                // 获取树节点中的 HttpRequestItem 对象（这才是被序列化保存的对象）
                Object[] nodeObj = (Object[]) requestNode.getUserObject();
                HttpRequestItem treeRequestItem = (HttpRequestItem) nodeObj[1];

                // 添加到树节点中的请求对象
                if (treeRequestItem.getResponse() == null) {
                    treeRequestItem.setResponse(new ArrayList<>());
                }
                treeRequestItem.getResponse().add(savedResponse);

                // 同时更新 originalRequestItem（保持一致性）
                if (originalRequestItem.getResponse() == null) {
                    originalRequestItem.setResponse(new ArrayList<>());
                }
                originalRequestItem.getResponse().add(savedResponse);

                // 创建响应节点
                DefaultMutableTreeNode responseNode = new DefaultMutableTreeNode(
                        new Object[]{RequestCollectionsLeftPanel.SAVED_RESPONSE, savedResponse}
                );
                requestNode.add(responseNode);

                // 刷新树
                leftPanel.getTreeModel().reload(requestNode);
                leftPanel.getRequestTree().expandPath(new TreePath(requestNode.getPath()));

                // 保存到文件
                leftPanel.getPersistence().saveRequestGroups();

                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_SUCCESS, name));
            } else {
                log.warn("无法找到请求节点，保存响应失败");
                NotificationUtil.showWarning(I18nUtil.getMessage("无法找到请求节点"));
            }

        } catch (Exception ex) {
            log.error("保存响应失败", ex);
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.RESPONSE_SAVE_ERROR, ex.getMessage()));
        }
    }

    /**
     * 在树中查找请求节点
     */
    private DefaultMutableTreeNode findRequestNodeInTree(HttpRequestItem item) {
        RequestCollectionsLeftPanel leftPanel =
                SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        DefaultMutableTreeNode root = leftPanel.getRootTreeNode();
        return findRequestNodeRecursively(root, item);
    }

    private DefaultMutableTreeNode findRequestNodeRecursively(DefaultMutableTreeNode node, HttpRequestItem item) {
        // 防御性检查
        if (node == null || item == null || item.getId() == null) {
            return null;
        }

        // 检查当前节点
        Object userObj = node.getUserObject();
        if (userObj instanceof Object[] obj && RequestCollectionsLeftPanel.REQUEST.equals(obj[0])) {
            HttpRequestItem nodeItem = (HttpRequestItem) obj[1];
            if (nodeItem != null && nodeItem.getId() != null && nodeItem.getId().equals(item.getId())) {
                return node;
            }
        }

        // 递归检查子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode result = findRequestNodeRecursively(child, item);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * 加载保存的响应到面板
     */
    public void loadSavedResponse(SavedResponse savedResponse) {
        if (savedResponse == null) return;

        try {
            // 1. 回填原始请求数据
            SavedResponse.OriginalRequest originalRequest = savedResponse.getOriginalRequest();
            if (originalRequest != null) {
                isLoadingData = true; // 设置加载标志，防止触发自动保存
                try {
                    // 设置 URL
                    if (originalRequest.getUrl() != null) {
                        urlField.setText(originalRequest.getUrl());
                        urlField.setCaretPosition(0); // 设置光标到开头
                    }

                    // 设置 Method
                    if (originalRequest.getMethod() != null) {
                        methodBox.setSelectedItem(originalRequest.getMethod());
                    }

                    // 设置 Params
                    if (CollUtil.isNotEmpty(originalRequest.getParams())) {
                        paramsPanel.setParamsList(new ArrayList<>(originalRequest.getParams()));
                    } else {
                        paramsPanel.clear();
                    }

                    // 设置 Headers
                    if (CollUtil.isNotEmpty(originalRequest.getHeaders())) {
                        headersPanel.setHeadersList(new ArrayList<>(originalRequest.getHeaders()));
                    } else {
                        headersPanel.setHeadersList(new ArrayList<>());
                    }

                    // 设置 Body Type
                    String bodyType = originalRequest.getBodyType();
                    if (CharSequenceUtil.isBlank(bodyType)) {
                        bodyType = RequestBodyPanel.BODY_TYPE_NONE;
                    }
                    requestBodyPanel.getBodyTypeComboBox().setSelectedItem(bodyType);

                    // 设置 Body 内容
                    String body = originalRequest.getBody();
                    if (body != null) {
                        requestBodyPanel.getBodyArea().setText(body);

                        // 智能设置 Raw Type
                        JComboBox<String> rawTypeComboBox = requestBodyPanel.getRawTypeComboBox();
                        if (rawTypeComboBox != null && CharSequenceUtil.isNotBlank(body)) {
                            if (JSONUtil.isTypeJSON(body)) {
                                rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_JSON);
                            } else if (XmlUtil.isXml(body)) {
                                rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_XML);
                            } else {
                                rawTypeComboBox.setSelectedItem(RequestBodyPanel.RAW_TYPE_TEXT);
                            }
                        }
                    }

                    // 设置 Form Data
                    if (CollUtil.isNotEmpty(originalRequest.getFormDataList())) {
                        EasyPostmanFormDataTablePanel formDataTablePanel = requestBodyPanel.getFormDataTablePanel();
                        formDataTablePanel.setFormDataList(new ArrayList<>(originalRequest.getFormDataList()));
                    }

                    // 设置 URL Encoded
                    if (CollUtil.isNotEmpty(originalRequest.getUrlencodedList())) {
                        EasyPostmanFormUrlencodedTablePanel urlencodedTablePanel = requestBodyPanel.getFormUrlencodedTablePanel();
                        urlencodedTablePanel.setFormDataList(new ArrayList<>(originalRequest.getUrlencodedList()));
                    }

                    // 根据请求类型智能选择默认Tab（参考 initPanelData）
                    selectDefaultTabByRequestTypeForSavedResponse(originalRequest);
                } finally {
                    isLoadingData = false; // 恢复标志
                }
            }

            // 2. 构建 HttpResponse 对象
            HttpResponse response = new HttpResponse();
            response.code = savedResponse.getCode();
            response.body = savedResponse.getBody();

            // 转换 headers
            response.headers = new java.util.LinkedHashMap<>();
            if (savedResponse.getHeaders() != null) {
                for (HttpHeader header : savedResponse.getHeaders()) {
                    response.headers.put(header.getKey(), java.util.List.of(header.getValue()));
                }
            }

            response.costMs = savedResponse.getCostMs();
            response.bodySize = savedResponse.getBodySize();
            response.headersSize = savedResponse.getHeadersSize();

            // 3. 显示响应
            SwingUtilities.invokeLater(() -> {
                // 设置按钮状态（保持发送按钮可用）
                requestLinePanel.setSendButtonToSend(this::sendRequest);

                // 显示响应数据
                responsePanel.setResponseTabButtonsEnable(true);
                responsePanel.setResponseBody(response);
                responsePanel.setResponseHeaders(response);

                responsePanel.setStatus(response.code);
                responsePanel.setResponseTime(response.costMs);
                responsePanel.setResponseSize(response.bodySize, null);
                // 切换到 Response Body tab
                responsePanel.switchToTab(0);
                responsePanel.setResponseBodyEnabled(true);
            });

        } catch (Exception ex) {
            log.error("加载保存的响应失败", ex);
            NotificationUtil.showError(I18nUtil.getMessage("加载响应失败: {0}", ex.getMessage()));
        }
    }

    /**
     * 根据保存的响应的请求类型智能选择默认Tab（用于 loadSavedResponse）
     */
    private void selectDefaultTabByRequestTypeForSavedResponse(SavedResponse.OriginalRequest originalRequest) {
        if (originalRequest == null) {
            return;
        }

        selectDefaultTab(
                originalRequest.getMethod(),
                originalRequest.getBodyType(),
                originalRequest.getBody(),
                CollUtil.isNotEmpty(originalRequest.getFormDataList()) || CollUtil.isNotEmpty(originalRequest.getUrlencodedList()),
                CollUtil.isNotEmpty(originalRequest.getParams())
        );
    }

    /**
     * 获取默认的分割比例
     *
     * @return 根据协议类型返回合适的 resizeWeight
     */
    private double getDefaultResizeWeight() {
        // WebSocket 和 SSE：请求占 20%，响应占 80%（主要看响应流）
        if (protocol.isWebSocketProtocol() || protocol.isSseProtocol()) {
            return 0.2;
        }
        // HTTP：默认对半分
        return 0.4;
    }

    /**
     * 动态更新布局方向（用于全局布局切换）
     *
     * @param isVertical true=垂直布局，false=水平布局
     */
    public void updateLayoutOrientation(boolean isVertical) {
        int targetOrientation = isVertical ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;

        // 如果方向未改变，直接返回
        if (splitPane.getOrientation() == targetOrientation) {
            return;
        }

        // 计算分割比例
        double ratio;
        if (isVertical) {
            ratio = getDefaultResizeWeight();
        } else {
            ratio = 0.5; // 水平布局固定 5:5
        }

        // 更新布局
        splitPane.setOrientation(targetOrientation);
        splitPane.setResizeWeight(ratio);

        // 重置初始化标志，以便 doLayout 重新设置分割位置
        initialDividerLocationSet = false;

        // 立即设置分割位置
        splitPane.revalidate();
        int totalSize = isVertical ? splitPane.getHeight() : splitPane.getWidth();
        if (totalSize > 0) {
            splitPane.setDividerLocation(ratio);
        }

        // 更新ResponsePanel的Tab显示方式
        if (responsePanel != null) {
            responsePanel.updateLayoutOrientation(isVertical);
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        // 第一次布局时，为水平布局设置精确的 5:5 分割位置
        // 垂直布局依赖 resizeWeight 即可，不需要额外设置
        if (!initialDividerLocationSet && splitPane != null &&
                splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT &&
                splitPane.getWidth() > 0) {
            initialDividerLocationSet = true;
            splitPane.setDividerLocation(0.5);
        }
    }

    /**
     * 检测并解析 cURL 命令
     */
    private void detectAndParseCurl() {
        // 如果正在加载数据，不触发 cURL 解析（避免误解析正常URL）
        if (isLoadingData) {
            return;
        }

        String text = urlField.getText();
        if (text != null && text.trim().toLowerCase().startsWith("curl")) {
            SwingUtilities.invokeLater(() -> {
                try {
                    CurlRequest curlRequest = CurlParser.parse(text.trim());
                    if (curlRequest.url != null) {
                        handleCurlParsed(curlRequest);
                    }
                } catch (Exception ex) {
                    // 解析失败时不做处理，用户可能还在输入
                }
            });
        }
    }

    /**
     * 处理curl命令解析结果，自动回填到表单
     */
    private void handleCurlParsed(CurlRequest curlRequest) {
        if (curlRequest == null || curlRequest.url == null) {
            return;
        }

        try {
            // 将 CurlRequest 转换为 HttpRequestItem
            HttpRequestItem item = CurlImportUtil.fromCurlRequest(curlRequest);
            if (item != null) {
                // 使用现有的 initPanelData 方法回填数据
                initPanelData(item);
                // 清空剪贴板内容，避免重复导入
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
                // 显示成功提示
                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.PARSE_CURL_SUCCESS));
            }
        } catch (Exception e) {
            // 静默处理错误，用户可能还在输入
        }
    }
}
