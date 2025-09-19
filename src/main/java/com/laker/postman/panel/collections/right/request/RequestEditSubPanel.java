package com.laker.postman.panel.collections.right.request;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.setting.SettingManager;
import com.laker.postman.common.table.map.EasyNameValueTablePanel;
import com.laker.postman.common.table.map.EasyTablePanel;
import com.laker.postman.model.*;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.sub.*;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.http.RedirectHandler;
import com.laker.postman.service.http.sse.SseEventListener;
import com.laker.postman.service.http.sse.SseUiCallback;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.InterruptedIOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.laker.postman.service.http.HttpUtil.*;

/**
 * 单个请求编辑子面板，包含 URL、方法选择、Headers、Body 和响应展示
 */
@Slf4j
public class RequestEditSubPanel extends JPanel {
    public static final String TEXT_EVENT_STREAM = "text/event-stream";
    public static final String ACCEPT = "Accept";
    public static final String USER_AGENT = "User-Agent";
    public static final String EASY_POSTMAN_HTTP_CLIENT = "EasyPostman HTTP Client";
    public static final String VALUE = "*/*";
    private final JTextField urlField;
    private final JComboBox<String> methodBox;
    private final EasyNameValueTablePanel paramsPanel;
    private final EasyNameValueTablePanel headersPanel;
    @Getter
    private String id;
    private String name;
    private final RequestItemProtocolEnum protocol;
    private final RequestLinePanel requestLinePanel;
    //  RequestBodyPanel
    private final RequestBodyPanel requestBodyPanel;
    @Getter
    private HttpRequestItem originalRequestItem;
    private final AuthTabPanel authTabPanel;
    private final ScriptPanel scriptPanel;
    private final JTabbedPane reqTabs; // 请求选项卡面板

    // 当前请求的 SwingWorker，用于支持取消
    private transient SwingWorker<Void, Void> currentWorker;
    // 当前 SSE 事件源, 用于取消 SSE 请求
    private transient EventSource currentEventSource;
    // WebSocket连接对象
    private volatile transient WebSocket currentWebSocket;
    // WebSocket连接ID，用于防止过期连接的回调
    private volatile String currentWebSocketConnectionId;
    JSplitPane splitPane;
    // 双向联动控制标志，防止循环更新
    private boolean isUpdatingFromUrl = false;
    private boolean isUpdatingFromParams = false;
    @Getter
    private final ResponsePanel responsePanel;

    public RequestEditSubPanel(String id, RequestItemProtocolEnum protocol) {
        this.id = id;
        this.protocol = protocol;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 设置边距为5
        // 1. 顶部请求行面板
        requestLinePanel = new RequestLinePanel(this::sendRequest, protocol);
        methodBox = requestLinePanel.getMethodBox();
        urlField = requestLinePanel.getUrlField();
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                parseUrlParamsToParamsPanel();
            }

            public void removeUpdate(DocumentEvent e) {
                parseUrlParamsToParamsPanel();
            }

            public void changedUpdate(DocumentEvent e) {
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

        // 2.1 Params
        paramsPanel = new EasyNameValueTablePanel("Key", "Value");
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_PARAMS), paramsPanel); // 2.1 添加参数选项卡

        // 添加Params面板的监听器，实现从Params到URL的联动
        paramsPanel.addTableModelListener(e -> {
            if (!isUpdatingFromUrl) {
                parseParamsPanelToUrl();
            }
        });

        // 2.2 Auth 面板
        authTabPanel = new AuthTabPanel();
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_AUTHORIZATION), authTabPanel);

        // 2.3 Headers
        headersPanel = new EasyNameValueTablePanel("Key", "Value");
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_HEADERS), headersPanel);

        // 2.4 Body 面板
        requestBodyPanel = new RequestBodyPanel(protocol);
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_REQUEST_BODY), requestBodyPanel);


        // 2.5 脚本Tab
        scriptPanel = new ScriptPanel();
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_SCRIPTS), scriptPanel);

        // 2.6 Cookie 面板
        CookieTablePanel cookiePanel = new CookieTablePanel();
        reqTabs.addTab(I18nUtil.getMessage(MessageKeys.TAB_COOKIES), cookiePanel);

        // 3. 响应面板
        responsePanel = new ResponsePanel();
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, reqTabs, responsePanel);
        splitPane.setDividerSize(2); // 设置分割条的宽度
        splitPane.setResizeWeight(0.5); // 设置分割线位置，表示请求部分占50%
        add(splitPane, BorderLayout.CENTER);

        if (protocol.isWebSocketProtocol()) {
            // WebSocket消息发送按钮事件绑定（只绑定一次）
            requestBodyPanel.setWsSendActionListener(e -> sendWebSocketMessage());
            splitPane.setResizeWeight(0.2); // 设置分割线位置，表示请求部分占30%
            // 切换到WebSocket协议时，默认选中Body Tab
            reqTabs.setSelectedComponent(requestBodyPanel);
            // 隐藏认证tab和cookie tab
            reqTabs.remove(authTabPanel);
            reqTabs.remove(cookiePanel);
            // 初始时禁用发送和定时按钮，只有连接后才可用
            requestBodyPanel.setWebSocketConnected(false);
        }
        // 监听表单内容变化，动态更新tab红点
        addDirtyListeners();
    }

    /**
     * 添加监听器，表单内容变化时在tab标题显示红点
     */
    private void addDirtyListeners() {
        // 监听urlField
        addDocumentListener(urlField.getDocument());
        // 监听methodBox
        methodBox.addActionListener(e -> updateTabDirty());
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
            this.originalRequestItem = JSONUtil.toBean(JSONUtil.parse(item).toString(), HttpRequestItem.class);
        } else {
            this.originalRequestItem = null;
        }
    }

    /**
     * 判断当前表单内容是否被修改（与原始请求对比）
     */
    public boolean isModified() {
        if (originalRequestItem == null) return false;
        HttpRequestItem current = getCurrentRequest();
        String oriJson = JSONUtil.toJsonStr(originalRequestItem);
        String curJson = JSONUtil.toJsonStr(current);
        boolean isModified = !oriJson.equals(curJson);
        if (isModified) {
            log.debug("Request form has been modified,Request Name: {}", current.getName());
            log.debug("oriJson: {}", oriJson);
            log.debug("curJson: {}", curJson);
        }
        return isModified;
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

    private void setResponseBody(HttpResponse resp) {
        responsePanel.getResponseBodyPanel().setBodyText(resp);
    }


    private void sendRequest(ActionEvent e) {
        if (currentWorker != null) {
            cancelCurrentRequest();
            return;
        }

        // 清理上次请求的临时变量
        EnvironmentService.clearTemporaryVariables();

        HttpRequestItem item = getCurrentRequest();

        // 根据协议类型进行URL验证
        String url = item.getUrl();
        RequestItemProtocolEnum protocol = item.getProtocol();
        if (protocol.isWebSocketProtocol()) {
            // WebSocket只允许ws://或wss://协议
            if (!url.toLowerCase().startsWith("ws://") && !url.toLowerCase().startsWith("wss://")) {
                JOptionPane.showMessageDialog(this,
                        "WebSocket requests must use ws:// or wss:// protocol",
                        "Invalid URL Protocol", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // 强制使用全局 followRedirects 设置
        item.setFollowRedirects(SettingManager.isFollowRedirects());
        PreparedRequest req = PreparedRequestBuilder.build(item);
        Map<String, Object> bindings = prepareBindings(req);
        if (!executePrescript(item, bindings)) return;

        // 前置脚本执行完成后，再进行变量替换
        PreparedRequestBuilder.replaceVariablesAfterPreScript(req);

        if (!validateRequest(req, item)) return;
        updateUIForRequesting();

        // 协议分发 - 根据HttpRequestItem的protocol字段分发
        if (protocol.isWebSocketProtocol()) {
            handleWebSocketRequest(item, req, bindings);
        } else if (isSSERequest(req)) {
            handleSseRequest(item, req, bindings);
        } else {
            handleHttpRequest(item, req, bindings);
        }
    }

    // 普通HTTP请求处理
    private void handleHttpRequest(HttpRequestItem item, PreparedRequest req, Map<String, Object> bindings) {
        currentWorker = new SwingWorker<>() {
            String statusText;
            HttpResponse resp;

            @Override
            protected Void doInBackground() {
                try {
                    responsePanel.setResponseTabButtonsEnable(true);
                    resp = RedirectHandler.executeWithRedirects(req, 10);
                    if (resp != null) {
                        statusText = (resp.code > 0 ? String.valueOf(resp.code) : "Unknown Status");
                    }
                } catch (InterruptedIOException ex) {
                    log.warn(ex.getMessage());
                    statusText = ex.getMessage();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    ConsolePanel.appendLog("[Error] " + ex.getMessage(), ConsolePanel.LogType.ERROR);
                    statusText = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                updateUIForResponse(statusText, resp);
                handleResponse(item, bindings, req, resp);
                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                currentWorker = null;

                if (resp != null && resp.isSse) {
                    // 弹窗提示用户是否切换到SSE监听模式
                    SwingUtilities.invokeLater(() -> {
                        int result = JOptionPane.showConfirmDialog(RequestEditSubPanel.this,
                                I18nUtil.getMessage(MessageKeys.SSE_SWITCH_TIP),
                                I18nUtil.getMessage(MessageKeys.SSE_SWITCH_TITLE),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (result == JOptionPane.YES_OPTION) {
                            // 检查 header 区域是否已存在 Accept: text/event-stream（忽略大小写）
                            boolean hasSseAccept = headersPanel.getMap().keySet().stream()
                                    .anyMatch(k -> k != null && k.equalsIgnoreCase(ACCEPT) &&
                                            TEXT_EVENT_STREAM.equalsIgnoreCase(headersPanel.getMap().get(k)));
                            if (!hasSseAccept) {
                                headersPanel.addRow(ACCEPT, TEXT_EVENT_STREAM);
                            }
                            reqTabs.setSelectedComponent(headersPanel);
                            // 定位到headerpanel table的最后一行
                            headersPanel.scrollRectToVisible();
                            JOptionPane.showMessageDialog(RequestEditSubPanel.this,
                                    I18nUtil.getMessage(MessageKeys.SSE_HEADER_ADDED),
                                    I18nUtil.getMessage(MessageKeys.OPERATION_TIP),
                                    JOptionPane.INFORMATION_MESSAGE);
                            sendRequest(null);
                        }
                    });
                }
            }
        };
        currentWorker.execute();
    }

    // SSE请求处理
    private void handleSseRequest(HttpRequestItem item, PreparedRequest req, Map<String, Object> bindings) {
        currentWorker = new SwingWorker<>() {
            HttpResponse resp;
            StringBuilder sseBodyBuilder;
            long startTime;

            @Override
            protected Void doInBackground() {
                try {
                    if (!isSSERequest(req)) {
                        req.headers.remove("accept"); // 如果不是SSE请求，移除Accept头
                        req.headers.remove(ACCEPT);
                        req.headers.put(ACCEPT, TEXT_EVENT_STREAM); // 确保设置为SSE类型
                    }
                    startTime = System.currentTimeMillis();
                    resp = new HttpResponse();
                    sseBodyBuilder = new StringBuilder();
                    SseUiCallback callback = new SseUiCallback() {
                        @Override
                        public void onOpen(HttpResponse r, String headersText) {
                            SwingUtilities.invokeLater(() -> updateUIForResponse(String.valueOf(r.code), r));
                        }

                        @Override
                        public void onEvent(HttpResponse r) {
                            SwingUtilities.invokeLater(() -> {
                                setResponseBody(r);
                                responsePanel.getResponseSizeLabel().setText("ResponseSize: " + getSizeText(r.bodySize));
                            });
                        }

                        @Override
                        public void onClosed(HttpResponse r) {
                            SwingUtilities.invokeLater(() -> {
                                updateUIForResponse(String.valueOf(r.code), r);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                            });
                            currentEventSource = null;
                            currentWorker = null;
                        }

                        @Override
                        public void onFailure(String errorMsg, HttpResponse r) {
                            SwingUtilities.invokeLater(() -> {
                                responsePanel.getStatusCodeLabel().setText(I18nUtil.getMessage(MessageKeys.SSE_FAILED, errorMsg));
                                responsePanel.getStatusCodeLabel().setForeground(Color.RED);
                                updateUIForResponse(I18nUtil.getMessage(MessageKeys.SSE_FAILED, errorMsg), r);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                            });
                            currentEventSource = null;
                            currentWorker = null;
                        }
                    };
                    currentEventSource = HttpSingleRequestExecutor.executeSSE(req, new SseEventListener(callback, resp, sseBodyBuilder, startTime));
                    responsePanel.setResponseTabButtonsEnable(true); // 启用响应区的tab按钮
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    SwingUtilities.invokeLater(() -> {
                        responsePanel.getStatusCodeLabel().setText(I18nUtil.getMessage(MessageKeys.SSE_ERROR, ex.getMessage()));
                        responsePanel.getStatusCodeLabel().setForeground(Color.RED);
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
    private void handleWebSocketRequest(HttpRequestItem item, PreparedRequest req, Map<String, Object> bindings) {
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
                                webSocket.close(1000, "Connection expired");
                                return;
                            }

                            resp.headers = new LinkedHashMap<>();
                            for (String name : response.headers().names()) {
                                resp.headers.put(name, response.headers(name));
                            }
                            resp.code = response.code();
                            resp.protocol = response.protocol().toString();
                            currentWebSocket = webSocket;
                            SwingUtilities.invokeLater(() -> {
                                updateUIForResponse(String.valueOf(resp.code), resp);
                                reqTabs.setSelectedComponent(requestBodyPanel);
                                requestBodyPanel.getWsSendButton().requestFocusInWindow();
                                requestLinePanel.setSendButtonToClose(RequestEditSubPanel.this::sendRequest);
                                // 连接成功后启用发送和定时按钮
                                requestBodyPanel.setWebSocketConnected(true);
                            });
                            appendWebSocketMessage(WebSocketMsgType.CONNECTED, response.message());
                        }

                        @Override
                        public void onMessage(okhttp3.WebSocket webSocket, String text) {
                            // 检查连接ID是否还有效
                            if (!connectionId.equals(currentWebSocketConnectionId)) {
                                log.debug("Ignoring onMessage callback for expired connection ID: {}", connectionId);
                                return;
                            }
                            appendWebSocketMessage(WebSocketMsgType.RECEIVED, text);
                        }

                        @Override
                        public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
                            // 检查连接ID是否还有效
                            if (!connectionId.equals(currentWebSocketConnectionId)) {
                                log.debug("Ignoring onMessage(binary) callback for expired connection ID: {}", connectionId);
                                return;
                            }
                            appendWebSocketMessage(WebSocketMsgType.BINARY, bytes.hex());
                        }

                        @Override
                        public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
                            // 检查连接ID是否还有效
                            if (CharSequenceUtil.isBlank(currentWebSocketConnectionId) || connectionId.equals(currentWebSocketConnectionId)) {
                                log.debug("closing WebSocket: code={}, reason={}", code, reason);
                                appendWebSocketMessage(WebSocketMsgType.CLOSED, code + ", " + reason);
                                handleWebSocketClose();
                            }
                        }

                        @Override
                        public void onClosed(WebSocket webSocket, int code, String reason) {
                            // 检查连接ID是否还有效
                            if (CharSequenceUtil.isBlank(currentWebSocketConnectionId) || connectionId.equals(currentWebSocketConnectionId)) {
                                log.debug("closed WebSocket: code={}, reason={}", code, reason);
                                appendWebSocketMessage(WebSocketMsgType.CLOSED, code + ", " + reason);
                                handleWebSocketClose();
                            }
                        }

                        private void handleWebSocketClose() {
                            closed = true;
                            resp.costMs = System.currentTimeMillis() - startTime;
                            currentWebSocket = null;
                            SwingUtilities.invokeLater(() -> {
                                updateUIForResponse("closed", resp);
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
                            appendWebSocketMessage(WebSocketMsgType.WARNING, t.getMessage());
                            closed = true;
                            resp.costMs = System.currentTimeMillis() - startTime;
                            SwingUtilities.invokeLater(() -> {
                                String statusMsg = response != null ? I18nUtil.getMessage(MessageKeys.WEBSOCKET_FAILED, t.getMessage() + " (" + response.code() + ")")
                                        : I18nUtil.getMessage(MessageKeys.WEBSOCKET_FAILED, t.getMessage());
                                responsePanel.getStatusCodeLabel().setText(statusMsg);
                                responsePanel.getStatusCodeLabel().setForeground(Color.RED);
                                updateUIForResponse(I18nUtil.getMessage(MessageKeys.WEBSOCKET_FAILED, t.getMessage()), resp);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                                // 失败后禁用发送和定时按钮
                                requestBodyPanel.setWebSocketConnected(false);
                            });
                        }
                    });
                    responsePanel.setResponseTabButtonsEnable(true); // 启用响应区的tab按钮
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    SwingUtilities.invokeLater(() -> {
                        responsePanel.getStatusCodeLabel().setText(I18nUtil.getMessage(MessageKeys.WEBSOCKET_ERROR, ex.getMessage()));
                        responsePanel.getStatusCodeLabel().setForeground(Color.RED);
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
            appendWebSocketMessage(WebSocketMsgType.INFO, I18nUtil.getMessage(MessageKeys.WEBSOCKET_NOT_CONNECTED));
            return;
        }

        String msg = requestBodyPanel.getRawBody();
        if (CharSequenceUtil.isNotBlank(msg)) {
            currentWebSocket.send(msg); // 发送消息
            appendWebSocketMessage(WebSocketMsgType.SENT, msg);
        }
    }

    private void appendWebSocketMessage(WebSocketMsgType type, String text) {
        String formattedText = formatWebSocketMessage(type, text);
        SwingUtilities.invokeLater(() -> responsePanel.getResponseBodyPanel().appendBodyText(formattedText));
    }

    /**
     * 格式化WebSocket消息，添加图标前缀和时间戳
     *
     * @param type    消息类型
     * @param message 原始消息
     * @return 格式化后的消息
     */
    private String formatWebSocketMessage(WebSocketMsgType type, String message) {
        if (message == null) return "";
        // 添加时间戳
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return "[" + timestamp + "]" + I18nUtil.getMessage(type.iconKey) + " " + message;
    }

    /**
     * 更新表单内容（用于切换请求或保存后刷新）
     */
    public void updateRequestForm(HttpRequestItem item) {
        this.id = item.getId();
        this.name = item.getName();
        // 拆解URL参数
        String url = item.getUrl();
        urlField.setText(url);
        urlField.setCaretPosition(0); // 设置光标到开头

        // 如果URL中有参数，解析到paramsPanel
        Map<String, String> mergedParams = HttpUtil.getMergedParams(item.getParams(), url);
        // 更新参数面板
        paramsPanel.setMap(mergedParams);
        methodBox.setSelectedItem(item.getMethod());
        // Headers
        headersPanel.setMap(item.getHeaders());
        // 自动补充 User-Agent 和 Accept
        // 判断是否已存在 User-Agent（忽略大小写）
        boolean hasUserAgent = item.getHeaders().keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase(USER_AGENT));
        if (!hasUserAgent) {
            item.getHeaders().put(USER_AGENT, EASY_POSTMAN_HTTP_CLIENT);
            headersPanel.addRow(USER_AGENT, EASY_POSTMAN_HTTP_CLIENT);
        }
        // 判断是否已存在 Accept（忽略大小写）
        boolean hasAccept = item.getHeaders().keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase(ACCEPT));
        if (!hasAccept) {
            item.getHeaders().put(ACCEPT, VALUE);
            headersPanel.addRow(ACCEPT, VALUE);
        }
        // Body
        requestBodyPanel.getBodyArea().setText(item.getBody());
        if (StrUtil.isBlank(item.getBody())) {
            requestBodyPanel.getBodyTypeComboBox().setSelectedItem(RequestBodyPanel.BODY_TYPE_NONE); // 切换到无 Body 模式
            // form-data 字段还原
        } else {
            // 其他模式（如 raw）
            // raw: 如果请求头没有设置 application/json，则补充（忽略大小写）
            ensureContentTypeHeader(item.getHeaders(), "application/json", headersPanel);
        }

        if (MapUtil.isNotEmpty(item.getFormData()) || MapUtil.isNotEmpty(item.getFormFiles())) {
            requestBodyPanel.getBodyTypeComboBox().setSelectedItem(RequestBodyPanel.BODY_TYPE_FORM_DATA); // 切换到 form-data 模式
            EasyTablePanel formDataTablePanel = requestBodyPanel.getFormDataTablePanel();
            formDataTablePanel.clear();
            if (item.getFormData() != null) {
                for (Map.Entry<String, String> entry : item.getFormData().entrySet()) {
                    formDataTablePanel.addRow(entry.getKey(), "Text", entry.getValue());
                }
            }
            if (item.getFormFiles() != null) {
                for (Map.Entry<String, String> entry : item.getFormFiles().entrySet()) {
                    formDataTablePanel.addRow(entry.getKey(), "File", entry.getValue());
                }
            }
            // form-data: 如果请求头没有设置 multipart/form-data，则补充（忽略大小写）
            ensureContentTypeHeader(item.getHeaders(), "multipart/form-data", headersPanel);
        } else if (MapUtil.isNotEmpty(item.getUrlencoded())) {
            // 处理 POST-x-www-form-urlencoded
            requestBodyPanel.getBodyTypeComboBox().setSelectedItem(RequestBodyPanel.BODY_TYPE_FORM_URLENCODED);
            EasyTablePanel urlencodedTablePanel = requestBodyPanel.getFormUrlencodedTablePanel();
            urlencodedTablePanel.clear();
            for (Map.Entry<String, String> entry : item.getUrlencoded().entrySet()) {
                urlencodedTablePanel.addRow(entry.getKey(), entry.getValue());
            }
            // x-www-form-urlencoded: 如果请求头没有设置 application/x-www-form-urlencoded，则补充（忽略大小写）
            ensureContentTypeHeader(item.getHeaders(), "application/x-www-form-urlencoded", headersPanel);
        }

        // 认证Tab
        authTabPanel.setAuthType(item.getAuthType());
        authTabPanel.setUsername(item.getAuthUsername());
        authTabPanel.setPassword(item.getAuthPassword());
        authTabPanel.setToken(item.getAuthToken());

        // 前置/后置脚本
        scriptPanel.setPrescript(item.getPrescript() == null ? "" : item.getPrescript());
        scriptPanel.setPostscript(item.getPostscript() == null ? "" : item.getPostscript());
        // 设置原始数据用于脏检测
        setOriginalRequestItem(item);
    }

    /**
     * 获取当前表单内容封装为HttpRequestItem
     */
    public HttpRequestItem getCurrentRequest() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(this.id); // 保证id不丢失
        item.setName(this.name); // 保证name不丢失
        item.setUrl(urlField.getText().trim());
        item.setMethod((String) methodBox.getSelectedItem());
        item.setProtocol(protocol);
        item.setHeaders(headersPanel.getMap()); // 获取Headers表格内容
        item.setParams(paramsPanel.getMap()); // 获取Params表格内容
        // 统一通过requestBodyPanel获取body相关内容
        item.setBody(requestBodyPanel.getBodyArea().getText().trim());
        String bodyType = requestBodyPanel.getBodyType();
        if (RequestBodyPanel.BODY_TYPE_FORM_DATA.equals(bodyType)) {
            item.setFormData(requestBodyPanel.getFormData());
            item.setFormFiles(requestBodyPanel.getFormFiles());
            item.setBody(""); // form-data模式下，body通常不直接使用
        } else if (RequestBodyPanel.BODY_TYPE_FORM_URLENCODED.equals(bodyType)) {
            item.setBody(""); // x-www-form-urlencoded模式下，body通常不直接使用
            item.setFormData(new LinkedHashMap<>());
            item.setFormFiles(new LinkedHashMap<>());
            item.setUrlencoded(requestBodyPanel.getUrlencoded());
        } else if (RequestBodyPanel.BODY_TYPE_RAW.equals(bodyType)) {
            item.setBody(requestBodyPanel.getRawBody());
            item.setFormData(new LinkedHashMap<>());
            item.setFormFiles(new LinkedHashMap<>());
        }
        // 认证Tab收集
        item.setAuthType(authTabPanel.getAuthType());
        item.setAuthUsername(authTabPanel.getUsername());
        item.setAuthPassword(authTabPanel.getPassword());
        item.setAuthToken(authTabPanel.getToken());
        // 脚本内容
        item.setPrescript(scriptPanel.getPrescript());
        item.setPostscript(scriptPanel.getPostscript());
        item.setFollowRedirects(SettingManager.isFollowRedirects());
        return item;
    }

    /**
     * 解析url中的参数到paramsPanel，并与现有params合并去重
     */
    private void parseUrlParamsToParamsPanel() {
        if (isUpdatingFromParams) {
            return; // 如果正在从Params更新URL，避免循环更新
        }

        isUpdatingFromUrl = true;
        try {
            String url = urlField.getText();
            Map<String, String> urlParams = getParamsMapFromUrl(url);

            // 获取当前Params面板的参数
            Map<String, String> currentParams = paramsPanel.getMap();

            // 如果URL中没有参数，清空Params面板
            if (urlParams == null || urlParams.isEmpty()) {
                if (!currentParams.isEmpty()) {
                    paramsPanel.clear();
                }
                return;
            }

            // 检查URL参数和当前Params参数是否完全一致
            if (!urlParams.equals(currentParams)) {
                // 完全用URL中的参数替换Params面板
                paramsPanel.clear();
                for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                    paramsPanel.addRow(entry.getKey(), entry.getValue());
                }
            }
        } finally {
            isUpdatingFromUrl = false;
        }
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

            // 获取Params面板的所有参数
            Map<String, String> params = paramsPanel.getMap();

            // 使用HttpUtil中的方法构建完整URL
            String newUrl = HttpUtil.buildUrlFromParamsMap(baseUrl, params);

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
        responsePanel.getStatusCodeLabel().setText(I18nUtil.getMessage(MessageKeys.STATUS_CANCELED));
        responsePanel.getStatusCodeLabel().setForeground(new Color(255, 140, 0));
        currentWorker = null;

        // 为WebSocket连接添加取消消息
        if (protocol.isWebSocketProtocol()) {
            appendWebSocketMessage(WebSocketMsgType.WARNING, "User canceled");
        }
    }

    // UI状态：请求中
    private void updateUIForRequesting() {
        responsePanel.setStatus(I18nUtil.getMessage(MessageKeys.STATUS_REQUESTING), new Color(255, 140, 0));
        responsePanel.setResponseTime(0);
        responsePanel.setResponseSize(0);
        requestLinePanel.setSendButtonToCancel(this::sendRequest);
        responsePanel.getNetworkLogPanel().clearLog();
        responsePanel.setResponseTabButtonsEnable(false);
        responsePanel.getResponseBodyPanel().setEnabled(false);
        responsePanel.clearAll();
    }

    // UI状态：响应完成
    private void updateUIForResponse(String statusText, HttpResponse resp) {
        if (resp == null) {
            responsePanel.setStatus(I18nUtil.getMessage(MessageKeys.STATUS_PREFIX, statusText), Color.RED);
            responsePanel.getResponseBodyPanel().setEnabled(true);
            return;
        }
        responsePanel.setResponseHeaders(resp);
        responsePanel.setTiming(resp);
        responsePanel.setResponseBody(resp);
        Color statusColor = getStatusColor(resp.code);
        responsePanel.setStatus(I18nUtil.getMessage(MessageKeys.STATUS_PREFIX, statusText), statusColor);
        responsePanel.setResponseTime(resp.costMs);
        responsePanel.setResponseSize(resp.bodySize);
        responsePanel.getResponseBodyPanel().setEnabled(true);
    }

    private void setTestResults(List<TestResult> testResults) {
        responsePanel.setTestResults(testResults);
    }

    // 处理响应、后置脚本、变量提取、历史
    private void handleResponse(HttpRequestItem item, Map<String, Object> bindings, PreparedRequest req, HttpResponse resp) {
        if (resp == null) {
            log.error("Response is null, cannot handle response.");
            return;
        }
        try {
            HttpUtil.postBindings(bindings, resp);
            executePostscript(item.getPostscript(), bindings);
            Postman pm = (Postman) bindings.get("pm");
            setTestResults(pm.testResults);
            SingletonFactory.getInstance(HistoryPanel.class).addRequestHistory(req, resp);
        } catch (Exception ex) {
            log.error("Error handling response: {}", ex.getMessage(), ex);
            ConsolePanel.appendLog("[Error] " + ex.getMessage(), ConsolePanel.LogType.ERROR);
        }
    }

    /**
     * 如果urlField内容没有协议，自动补全https:// 或 wss://，根据protocol判断
     */
    private void autoPrependHttpsIfNeeded() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) return;
        String lower = url.toLowerCase();
        if (!(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("ws://") || lower.startsWith("wss://"))) {
            if (protocol != null && protocol.isWebSocketProtocol()) {
                url = "wss://" + url;
            } else {
                url = "https://" + url;
            }
            urlField.setText(url);
        }
    }
}