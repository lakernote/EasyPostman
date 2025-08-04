package com.laker.postman.panel.collections.right.request;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
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
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.http.RedirectHandler;
import com.laker.postman.service.http.sse.SseEventListener;
import com.laker.postman.service.http.sse.SseUiCallback;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.TimeDisplayUtil;
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
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.laker.postman.service.http.HttpUtil.*;

/**
 * 单个请求编辑子面板，包含 URL、方法选择、Headers、Body 和响应展示
 */
@Slf4j
public class RequestEditSubPanel extends JPanel {
    private final JTextField urlField;
    private final JComboBox<String> methodBox;
    private final EasyNameValueTablePanel paramsPanel;
    private final EasyNameValueTablePanel headersPanel;
    @Getter
    private String id;
    private String name;
    // 状态展示组件
    private final JLabel statusCodeLabel;
    private final JLabel responseTimeLabel;
    private final JLabel responseSizeLabel;
    private final RequestLinePanel requestLinePanel;
    //  RequestBodyPanel
    private final RequestBodyPanel requestBodyPanel;
    private HttpRequestItem originalRequestItem;
    private final AuthTabPanel authTabPanel;
    private final ScriptPanel scriptPanel;
    private final ResponseHeadersPanel responseHeadersPanel;
    private final ResponseBodyPanel responseBodyPanel;
    @Getter
    private final NetworkLogPanel networkLogPanel; // 网络日志面板
    private final JTabbedPane reqTabs; // 请求选项卡面板

    // 当前请求的 SwingWorker，用于支持取消
    private SwingWorker<Void, Void> currentWorker;
    // 当前 SSE 事件源, 用于取消 SSE 请求
    private EventSource currentEventSource;
    // WebSocket连接对象
    private volatile WebSocket currentWebSocket;
    JSplitPane splitPane;
    private final JEditorPane testsPane;

    private final JButton[] tabButtons;

    public RequestEditSubPanel(String id) {
        this.id = id;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0)); // 设置边距为0
        // 1. 顶部请求行面板
        requestLinePanel = new RequestLinePanel(this::sendRequest);
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
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(requestLinePanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // 创建请求选项卡面板
        reqTabs = new JTabbedPane(); // 2. 创建请求选项卡面板
        reqTabs.setMinimumSize(new Dimension(400, 120));

        // 2.1 Params
        paramsPanel = new EasyNameValueTablePanel("Key", "Value");
        reqTabs.addTab("Params", paramsPanel); // 2.1 添加参数选项卡


        // 2.2 Auth 面板
        authTabPanel = new AuthTabPanel();
        reqTabs.addTab("Authorization", authTabPanel);

        // 2.3 Headers
        headersPanel = new EasyNameValueTablePanel("Key", "Value");
        reqTabs.addTab("Headers", headersPanel);

        // 2.4 Body 面板
        requestBodyPanel = new RequestBodyPanel();
        reqTabs.addTab("Body", requestBodyPanel);


        // 2.5 脚本Tab
        scriptPanel = new ScriptPanel();
        reqTabs.addTab("Scripts", scriptPanel);

        // 2.6 Cookie 面板
        CookieTablePanel cookiePanel = new CookieTablePanel();
        reqTabs.addTab("Cookies", cookiePanel);

        // 3. 响应面板
        JPanel responsePanel = new JPanel(new BorderLayout());
        // 顶部区域：tabBar在左，statusBar在右
        JPanel topResponseBar = new JPanel(new BorderLayout());
        // Tab栏（自定义按钮实现）
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        String[] tabNames = {"Body", "Headers", "Tests", "Network Log"};
        JButton[] tabButtons = new JButton[tabNames.length];
        this.tabButtons = tabButtons;
        for (int i = 0; i < tabNames.length; i++) {
            tabButtons[i] = new TabButton(tabNames[i], i);
            tabBar.add(tabButtons[i]);
        }
        topResponseBar.add(tabBar, BorderLayout.WEST);
        // 状态栏（不可点击）
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 4));
        statusCodeLabel = new JLabel();
        responseTimeLabel = new JLabel();
        responseSizeLabel = new JLabel();
        statusBar.add(statusCodeLabel);
        statusBar.add(responseTimeLabel);
        statusBar.add(responseSizeLabel);
        topResponseBar.add(statusBar, BorderLayout.EAST);
        responsePanel.add(topResponseBar, BorderLayout.NORTH);

        // CardLayout内容区
        JPanel cardPanel = new JPanel(new CardLayout());
        responseBodyPanel = new ResponseBodyPanel();
        responseBodyPanel.setEnabled(false); // 响应体面板初始不可编辑
        responseBodyPanel.setBodyText(null); // 初始无响应内容
        responseHeadersPanel = new ResponseHeadersPanel();
        JPanel testsPanel = new JPanel(new BorderLayout());
        testsPane = new JEditorPane();
        testsPane.setContentType("text/html");
        testsPane.setEditable(false);
        JScrollPane testsScrollPane = new JScrollPane(testsPane);
        testsPanel.add(testsScrollPane, BorderLayout.CENTER);
        networkLogPanel = new NetworkLogPanel();
        cardPanel.add(responseBodyPanel, "Body");
        cardPanel.add(responseHeadersPanel, "Headers");
        cardPanel.add(testsPanel, "Tests");
        cardPanel.add(networkLogPanel, "Network Log");
        responsePanel.add(cardPanel, BorderLayout.CENTER);

        // Tab按钮切换逻辑
        for (int i = 0; i < tabButtons.length; i++) {
            final int idx = i;
            tabButtons[i].addActionListener(e -> {
                CardLayout cl = (CardLayout) cardPanel.getLayout();
                cl.show(cardPanel, tabNames[idx]);
                selectedTabIndex = idx;
                for (JButton btn : tabButtons) {
                    btn.repaint();
                }
            });
        }
        // 默认显示Body
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, "Body");
        // 默认禁用响应区tab按钮
        for (JButton btn : tabButtons) {
            btn.setEnabled(false);
        }

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, reqTabs, responsePanel);
        splitPane.setDividerSize(1);
        splitPane.setResizeWeight(0.5); // 设置分割线位置，表示请求部分占50%
        add(splitPane, BorderLayout.CENTER);

        // WebSocket消息发送按钮事件绑定（只绑定一次）
        requestBodyPanel.getWsSendButton().addActionListener(e -> sendWebSocketMessage());
        requestBodyPanel.getWsSendButton().setEnabled(false);
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
        // 监听bodyArea
        if (requestBodyPanel.getBodyArea() != null) {
            addDocumentListener(requestBodyPanel.getBodyArea().getDocument());
        }
        // 监听formDataTableModel
        requestBodyPanel.getFormDataTablePanel().addTableModelListener(e -> updateTabDirty());
        // 监听formUrlencodedTableModel
        requestBodyPanel.getFormUrlencodedTablePanel().addTableModelListener(e -> updateTabDirty());
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
        if (item != null && item.getName() != null) {
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
            log.info("Request form has been modified,Request Name: {}", current.getName());
            log.info("oriJson: {}", oriJson);
            log.info("curJson: {}", curJson);
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
        responseBodyPanel.setBodyText(resp);
    }


    private void sendRequest(ActionEvent e) {
        if (currentWorker != null) {
            cancelCurrentRequest();
            return;
        }

        // 清理上次请求的临时变量
        EnvironmentService.clearTemporaryVariables();

        HttpRequestItem item = getCurrentRequest();
        // 强制使用全局 followRedirects 设置
        item.setFollowRedirects(SettingManager.isFollowRedirects());
        PreparedRequest req = PreparedRequestBuilder.build(item);
        Map<String, Object> bindings = prepareBindings(req);
        if (!executePrescript(item, bindings)) return;

        // 前置脚本执行完成后，再进行变量替换
        PreparedRequestBuilder.replaceVariablesAfterPreScript(req);

        if (!validateRequest(req, item)) return;
        updateUIForRequesting();
        // 协议分发
        if (isSSERequest(req)) {
            handleSseRequest(item, req, bindings);
            requestBodyPanel.showWebSocketSendPanel(false);
        } else if (isWebSocketRequest(req)) {
            handleWebSocketRequest(item, req, bindings);
        } else {
            handleHttpRequest(item, req, bindings);
            requestBodyPanel.showWebSocketSendPanel(false);
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
                    resp = RedirectHandler.executeWithRedirects(req, 10);
                    if (resp != null) {
                        statusText = (resp.code > 0 ? String.valueOf(resp.code) : "Unknown Status");
                    }
                } catch (InterruptedIOException ex) {
                    log.warn(ex.getMessage());
                    statusText = ex.getMessage();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    statusText = "发生错误: " + ex.getMessage();
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
                                "检测到 SSE 响应，是否切换到 SSE 监听模式？\n同意后将自动在 Header 区域增加 Accept: text/event-stream。",
                                "SSE切换提示",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (result == JOptionPane.YES_OPTION) {
                            // 检查 header 区域是否已存在 Accept: text/event-stream（忽略大小写）
                            boolean hasSseAccept = headersPanel.getMap().keySet().stream()
                                    .anyMatch(k -> k != null && k.equalsIgnoreCase("Accept") &&
                                            "text/event-stream".equalsIgnoreCase(headersPanel.getMap().get(k)));
                            if (!hasSseAccept) {
                                headersPanel.addRow("Accept", "text/event-stream");
                            }
                            reqTabs.setSelectedComponent(headersPanel);
                            // 定位到headerpanel table的最后一行
                            headersPanel.scrollRectToVisible();
                            JOptionPane.showMessageDialog(RequestEditSubPanel.this,
                                    "已自动添加 SSE 头部，即将重新发起请求。",
                                    "操作提示",
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
                        req.headers.remove("Accept");
                        req.headers.put("Accept", "text/event-stream"); // 确保设置为SSE类型
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
                                responseSizeLabel.setText("ResponseSize: " + getSizeText(r.bodySize));
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
                                statusCodeLabel.setText("SSE连接失败: " + errorMsg);
                                statusCodeLabel.setForeground(Color.RED);
                                updateUIForResponse("SSE连接失败", r);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                            });
                            currentEventSource = null;
                            currentWorker = null;
                        }
                    };
                    currentEventSource = HttpSingleRequestExecutor.executeSSE(req, new SseEventListener(callback, resp, sseBodyBuilder, startTime));
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    SwingUtilities.invokeLater(() -> {
                        statusCodeLabel.setText("SSE发生错误: " + ex.getMessage());
                        statusCodeLabel.setForeground(Color.RED);
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
        currentWorker = new SwingWorker<>() {
            WebSocket webSocket;
            final HttpResponse resp = new HttpResponse();
            long startTime;
            volatile boolean closed = false;

            @Override
            protected Void doInBackground() {
                try {
                    startTime = System.currentTimeMillis();
                    webSocket = HttpSingleRequestExecutor.executeWebSocket(req, new WebSocketListener() {
                        @Override
                        public void onOpen(WebSocket webSocket, Response response) {
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
                                requestBodyPanel.getWsSendButton().setEnabled(true);
                                requestBodyPanel.showWebSocketSendPanel(true);
                                requestBodyPanel.getWsSendButton().requestFocusInWindow();
                                requestLinePanel.setSendButtonToClose(RequestEditSubPanel.this::sendRequest);
                                JOptionPane.showMessageDialog(null,
                                        "WebSocket连接已建立，您可以开始发送消息。",
                                        "WebSocket连接成功",
                                        JOptionPane.INFORMATION_MESSAGE);
                            });
                            log.info("WebSocket连接已建立: {}", response.message());
                            appendWebSocketMessage("WebSocket连接已建立: " + response.message());
                        }

                        @Override
                        public void onMessage(okhttp3.WebSocket webSocket, String text) {
                            appendWebSocketMessage("收到消息: " + text);
                        }

                        @Override
                        public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
                            appendWebSocketMessage("收到二进制消息: " + bytes.hex());
                        }


                        @Override
                        public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
                            log.info("closing WebSocket: code={}, reason={}", code, reason);
                            appendWebSocketMessage("WebSocket正在关闭: " + code + ", " + reason);
                            handleWebSocketClose();
                        }

                        @Override
                        public void onClosed(WebSocket webSocket, int code, String reason) {
                            log.info("closed WebSocket: code={}, reason={}", code, reason);
                            appendWebSocketMessage("WebSocket已关闭: " + code + ", " + reason);
                            handleWebSocketClose();
                        }

                        private void handleWebSocketClose() {
                            closed = true;
                            resp.costMs = System.currentTimeMillis() - startTime;
                            currentWebSocket = null;
                            SwingUtilities.invokeLater(() -> {
                                updateUIForResponse("closed", resp);
                                requestBodyPanel.getWsSendButton().setEnabled(false);
                                requestBodyPanel.showWebSocketSendPanel(false);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                            });
                        }

                        @Override
                        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                            appendWebSocketMessage("WebSocket连接失败: " + t.getMessage());
                            if (response != null) {
                                log.error("WebSocket连接失败: {},响应状态: {},响应头: {}", t.getMessage(), response.code(), response.headers(), t);
                            } else {
                                log.error("WebSocket连接失败: {},响应状态: null,响应头: null", t.getMessage(), t);
                            }
                            closed = true;
                            resp.costMs = System.currentTimeMillis() - startTime;
                            SwingUtilities.invokeLater(() -> {
                                String statusMsg = response != null ? "WebSocket连接失败: " + t.getMessage() + " (状态: " + response.code() + ")" : "WebSocket连接失败: " + t.getMessage();
                                statusCodeLabel.setText(statusMsg);
                                statusCodeLabel.setForeground(Color.RED);
                                updateUIForResponse("WebSocket连接失败", resp);
                                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                            });
                        }
                    });
                } catch (Exception ex) {
                    log.error("WebSocket连接异常: {}", ex.getMessage(), ex);
                    SwingUtilities.invokeLater(() -> {
                        statusCodeLabel.setText("WebSocket发生错误: " + ex.getMessage());
                        statusCodeLabel.setForeground(Color.RED);
                        requestBodyPanel.getWsSendButton().setEnabled(false);
                        requestBodyPanel.showWebSocketSendPanel(false);
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

    // WebSocket消息发送逻辑
    private void sendWebSocketMessage() {
        String msg = requestBodyPanel.getRawBody();
        if (currentWebSocket != null && msg != null && !msg.isBlank()) {
            currentWebSocket.send(msg); // 发送消息
            appendWebSocketMessage("发送消息: " + msg);
            requestBodyPanel.getBodyArea().setText(""); // 清空输入框
        }
    }

    private void appendWebSocketMessage(String text) {
        String formattedText = formatWebSocketMessage(text);
        SwingUtilities.invokeLater(() -> responseBodyPanel.appendBodyText(formattedText));
    }

    /**
     * 格式化WebSocket消息，添加图标前缀
     *
     * @param message 原始消息
     * @return 格式化后的消息
     */
    private String formatWebSocketMessage(String message) {
        if (message == null) return "";

        if (message.startsWith("WebSocket连接已建立: ")) {
            return "🟢 " + message; // 连接图标
        } else if (message.startsWith("收到消息: ")) {
            return "📥 " + message; // 接收图标
        } else if (message.startsWith("收到二进制消息: ")) {
            return "📦 " + message; // 包图标
        } else if (message.startsWith("发送消息: ")) {
            return "🚀 " + message; // 发送图标，使用火箭图标代替发件箱图标
        } else if (message.startsWith("WebSocket正在关闭: ") || message.startsWith("WebSocket已关闭: ")) {
            return "🔴 " + message; // 关闭图标
        } else if (message.contains("失败") || message.contains("错误")) {
            return "⚠️ " + message; // 警告图标
        } else {
            return "ℹ️ " + message; // 信息图标（默认）
        }
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
        Map<String, String> mergedParams = getMergedParams(item.getParams(), url);
        // 更新参数面板
        paramsPanel.setMap(mergedParams);
        methodBox.setSelectedItem(item.getMethod());
        // Headers
        headersPanel.setMap(item.getHeaders());
        // 自动补充 User-Agent 和 Accept
        // 判断是否已存在 User-Agent（忽略大小写）
        boolean hasUserAgent = item.getHeaders().keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase("User-Agent"));
        if (!hasUserAgent) {
            item.getHeaders().put("User-Agent", "EasyPostman HTTP Client");
            headersPanel.addRow("User-Agent", "EasyPostman HTTP Client");
        }
        // 判断是否已存在 Accept（忽略大小写）
        boolean hasAccept = item.getHeaders().keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase("Accept"));
        if (!hasAccept) {
            item.getHeaders().put("Accept", "*/*");
            headersPanel.addRow("Accept", "*/*");
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
        String url = urlField.getText();
        Map<String, String> urlParams = getParamsMapFromUrl(url);
        if (urlParams == null) return;
        Map<String, String> merged = new LinkedHashMap<>(paramsPanel.getMap());
        merged.putAll(urlParams);
        // 清空并填充paramsPanel
        paramsPanel.clear();
        for (java.util.Map.Entry<String, String> entry : merged.entrySet()) {
            paramsPanel.addRow(entry.getKey(), entry.getValue());
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
        currentWorker.cancel(true);
        requestLinePanel.setSendButtonToSend(this::sendRequest);
        statusCodeLabel.setText("Status: Canceled");
        statusCodeLabel.setForeground(new Color(255, 140, 0));
        currentWorker = null;
    }


    // UI状态：请求中
    private void updateUIForRequesting() {
        statusCodeLabel.setText("Status: Requesting...");
        statusCodeLabel.setForeground(new Color(255, 140, 0));
        responseTimeLabel.setText("Duration: --");
        responseSizeLabel.setText("ResponseSize: --");
        requestLinePanel.setSendButtonToCancel(this::sendRequest);
        networkLogPanel.clearLog();
        // 禁用响应区tab按钮
        for (JButton btn : tabButtons) {
            btn.setEnabled(false);
        }
        // 只禁用 responseBodyPanel
        responseBodyPanel.setEnabled(false);
        // 清空响应内容
        responseHeadersPanel.setHeaders(new LinkedHashMap<>());
        responseBodyPanel.setBodyText(null);
        // 清空网络日志
        networkLogPanel.clearLog();
        // 清空测试结果
        setTestResults(new ArrayList<>());
    }

    // UI状态：响应完成
    private void updateUIForResponse(String statusText, HttpResponse resp) {
        if (resp == null) {
            statusCodeLabel.setText("Status:" + statusText);
            statusCodeLabel.setForeground(Color.RED);
            // 恢复tab按钮
            for (JButton btn : tabButtons) {
                btn.setEnabled(true);
            }
            // 恢复 responseBodyPanel
            responseBodyPanel.setEnabled(true);
            return;
        }
        responseHeadersPanel.setHeaders(resp.headers);
        setResponseBody(resp);
        Color statusColor = getStatusColor(resp.code);
        statusCodeLabel.setText("Status: " + statusText);
        statusCodeLabel.setForeground(statusColor);
        responseTimeLabel.setText(String.format("Duration: %s", TimeDisplayUtil.formatElapsedTime(resp.costMs)));
        int bytes = resp.bodySize;
        responseSizeLabel.setText("ResponseSize: " + getSizeText(bytes));
        // 恢复tab按钮
        for (JButton btn : tabButtons) {
            btn.setEnabled(true);
        }
        // 恢复 responseBodyPanel
        responseBodyPanel.setEnabled(true);
    }

    // 处理响应、后置脚本、变量提取、历史
    private void handleResponse(HttpRequestItem item, Map<String, Object> bindings, PreparedRequest req, HttpResponse resp) {
        if (resp == null) {
            log.error("响应为空，无法处理后续操作");
            return;
        }
        String bodyText = resp.body;
        try {
            HttpUtil.postBindings(bindings, resp);
            executePostscript(item, bindings, resp, bodyText);
            Postman pm = (Postman) bindings.get("pm");
            setTestResults(pm.testResults);
            SingletonFactory.getInstance(HistoryPanel.class).addRequestHistory(req, resp);
        } catch (Exception ex) {
            log.error("请求处理异常: {}", ex.getMessage(), ex);
        }
    }


    /**
     * 设置测试结果到Tests Tab
     */
    private void setTestResults(java.util.List<TestResult> testResults) {
        String html = HttpHtmlRenderer.renderTestResults(testResults);
        testsPane.setText(html);
        testsPane.setCaretPosition(0);
        // 动态设置Tests按钮文本和颜色
        // Tests按钮在tabButtons中的下标
        int testsTabIndex = 2;
        if (tabButtons != null && tabButtons.length > testsTabIndex) {
            JButton testsBtn = tabButtons[testsTabIndex];
            if (CollectionUtil.isNotEmpty(testResults)) {
                boolean allPassed = testResults.stream().allMatch(r -> r.passed);
                String countText = "(" + testResults.size() + ")";
                String color = allPassed ? "#009900" : "#d32f2f"; // 绿色/红色
                String countHtml = "Tests<span style='color:" + color + ";font-weight:bold;'>" + countText + "</span>";
                testsBtn.setText("<html>" + countHtml + "</html>");
                testsBtn.setForeground(Color.BLACK); // 保持主色为黑色
            } else {
                testsBtn.setText("Tests");
                testsBtn.setForeground(Color.BLACK); // 默认色
            }
        }
    }


    private int selectedTabIndex = 0;

    // 自定义TabButton，支持底部高亮
    private class TabButton extends JButton {
        private final int tabIndex;

        public TabButton(String text, int tabIndex) {
            super(text);
            this.tabIndex = tabIndex;
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selectedTabIndex == tabIndex) {
                g.setColor(new Color(141, 188, 223)); // Google蓝
                g.fillRect(0, getHeight() - 3, getWidth(), 3);
            }
        }
    }
}

