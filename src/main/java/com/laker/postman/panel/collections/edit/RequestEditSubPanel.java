package com.laker.postman.panel.collections.edit;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.table.map.EasyNameValueTablePanel;
import com.laker.postman.common.table.map.EasyTablePanel;
import com.laker.postman.model.*;
import com.laker.postman.panel.SidebarTabPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.util.HttpRequestExecutor;
import com.laker.postman.util.HttpUtil;
import com.laker.postman.util.JsScriptExecutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个请求编辑子面板，包含 URL、方法选择、Headers、Body 和响应展示
 */
@Slf4j
public class RequestEditSubPanel extends JPanel {
    private final JTextField urlField;
    private final JComboBox<String> methodBox;
    private final EasyNameValueTablePanel paramsPanel;
    private final EasyNameValueTablePanel headersPanel;
    // 变量提取面板
    private final ExtractorPanel extractorPanel;
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
    private String rawResponseBodyText = null; // 保存原始响应体内容
    private HttpRequestItem originalRequestItem;
    private final AuthTabPanel authTabPanel;
    private final ScriptPanel scriptPanel;
    private final ResponseHeadersPanel responseHeadersPanel;
    private final ResponseBodyPanel responseBodyPanel;
    private final JTextArea redirectChainArea; // 重定向链文本区域

    // 当前请求的 SwingWorker，用于支持取消
    private SwingWorker<Void, Void> currentWorker;

    /**
     * 设置原始请求数据（脏数据检测）
     */
    public void setOriginalRequestItem(HttpRequestItem item) {
        if (item != null) {
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
        if (!isModified) {
            // all EasyNameValueTablePanel and EasyTablePanel updateTableBorder
            paramsPanel.updateTableBorder(false);
            headersPanel.updateTableBorder(false);
            requestBodyPanel.getFormDataTablePanel().updateTableBorder(false);
            requestBodyPanel.getFormUrlencodedTablePanel().updateTableBorder(false);
            extractorPanel.getExtractorTablePabel().updateTableBorder(false);
        }
        return isModified;
    }

    public RequestEditSubPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

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
        JTabbedPane reqTabs = new JTabbedPane(); // 2. 创建请求选项卡面板
        reqTabs.setMinimumSize(new Dimension(400, 120));

        // 2.1 Params
        paramsPanel = new EasyNameValueTablePanel("Key", "Value");
        reqTabs.addTab("Params", paramsPanel); // 2.1 添加参数选项卡
        // 2.2 Headers
        headersPanel = new EasyNameValueTablePanel("Key", "Value");
        reqTabs.addTab("Headers", headersPanel); // 2.2 添加 Headers 选项卡

        // 2.3 Body 面板
        requestBodyPanel = new RequestBodyPanel();
        reqTabs.addTab("Body", requestBodyPanel); // 2.3 添加 Body 选项卡

        // 2.4 Auth 面板
        authTabPanel = new AuthTabPanel();
        reqTabs.addTab("Authorization", authTabPanel); // 2.4 添加 认证 选项卡

        // 新增：脚本Tab，抽离为 ScriptPanel
        scriptPanel = new ScriptPanel();
        reqTabs.addTab("Scripts", scriptPanel);

        // 2.5 Cookie 面板
        CookieTablePanel cookiePanel = new CookieTablePanel(urlField);
        reqTabs.addTab("Cookies", cookiePanel);

        // 3. 响应面板
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("Response"));

        // 状态栏面板
        ResponseStatusPanel responseStatusPanel = new ResponseStatusPanel();
        statusCodeLabel = responseStatusPanel.getStatusCodeLabel();
        responseTimeLabel = responseStatusPanel.getResponseTimeLabel();
        responseSizeLabel = responseStatusPanel.getResponseSizeLabel();
        responsePanel.add(responseStatusPanel, BorderLayout.NORTH);

        // 响应Tabs
        JTabbedPane responseTabs = new JTabbedPane();
        // 响应头面板
        responseHeadersPanel = new ResponseHeadersPanel();
        responseTabs.addTab("Headers", responseHeadersPanel);
        // Response body panel
        responseBodyPanel = new ResponseBodyPanel();
        responseTabs.addTab("Body", responseBodyPanel);
        responseBodyPanel.getFormatButton().addActionListener(e -> formatResponseBody());
        // 重定向链Tab
        this.redirectChainArea = new JTextArea();
        redirectChainArea.setEditable(false);
        JScrollPane redirectScroll = new JScrollPane(redirectChainArea);
        responseTabs.addTab("Redirects", redirectScroll);
        // Variable extraction
        extractorPanel = new ExtractorPanel();
        extractorPanel.setRulesSupplier(() -> getCurrentRequest().getExtractorRules());
        extractorPanel.setEnvSupplier(EnvironmentService::getActiveEnvironment);
        extractorPanel.setRefreshEnvPanel(this::refreshEnvironmentPanel);
        responseTabs.addTab("Extractor", extractorPanel);
        responsePanel.add(responseTabs, BorderLayout.CENTER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, reqTabs, responsePanel);
        splitPane.setDividerSize(1);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

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
        // 监听extractorPanelTable
        extractorPanel.getExtractorTablePabel().addTableModelListener(e -> updateTabDirty());
        extractorPanel.autoExtractCheckBox.addActionListener(e -> updateTabDirty());
        // 监听脚本面板
        scriptPanel.addDirtyListeners(this::updateTabDirty);

        requestLinePanel.getFollowRedirectsCheckBox().addActionListener(e -> updateTabDirty());
    }

    private void addDocumentListener(Document document) {
        document.addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateTabDirty();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateTabDirty();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateTabDirty();
            }
        });
    }

    /**
     * 检查脏状态并更新tab标题
     */
    private void updateTabDirty() {
        SwingUtilities.invokeLater(() -> {
            boolean dirty = isModified();
            SingletonFactory.getInstance(RequestEditPanel.class).updateTabDirty(this, dirty);
            updateTablesBorder(dirty);
        });
    }

    public void updateTablesBorder(boolean isModified) {
        requestBodyPanel.getFormUrlencodedTablePanel().updateTableBorder(isModified);
        requestBodyPanel.getFormDataTablePanel().updateTableBorder(isModified);
        paramsPanel.updateTableBorder(isModified);
        headersPanel.updateTableBorder(isModified);
        extractorPanel.getExtractorTablePabel().updateTableBorder(isModified);
    }

    private void formatResponseBody() {
        String text = rawResponseBodyText != null ? rawResponseBodyText : responseBodyPanel.getResponseBodyPane().getText();
        if (JSONUtil.isTypeJSON(text)) {
            try {
                String prettyJson = JSONUtil.formatJsonStr(text);
                responseBodyPanel.getResponseBodyPane().setContentType("text/html");
                responseBodyPanel.getResponseBodyPane().setText("<pre>" + highlightJson(prettyJson) + "</pre>");
            } catch (Exception e) {
                responseBodyPanel.getResponseBodyPane().setContentType("text/plain");
                responseBodyPanel.getResponseBodyPane().setText(text);
            }
        } else {
            JOptionPane.showMessageDialog(this, "响应体不是有效的 JSON，无法格式化");
        }
        responseBodyPanel.getResponseBodyPane().setCaretPosition(0);
    }

    private void setResponseBody(HttpResponse resp) {
        rawResponseBodyText = resp.body;
        responseBodyPanel.setBodyText(resp);
        if (extractorPanel != null) {
            extractorPanel.setRawResponseBodyText(resp.body);
        }
    }

    private String highlightJson(String json) {
        String s = json.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        s = s.replaceAll("(\"[^\"]+\")\\s*:", "<span style='color:#1565c0;'>$1</span>:"); // 保持兼容，Java正则里\\"
        s = s.replaceAll(":\\s*(\".*?\")", ": <span style='color:#43a047;'>$1</span>");
        s = s.replaceAll(":\\s*([\\d.eE+-]+)", ": <span style='color:#8e24aa;'>$1</span>");
        return s;
    }

    // sendRequest方法替换为调用executeWithRedirects
    private void sendRequest(ActionEvent e) {
        // 如果当前有请求正在进行，则视为取消操作
        if (currentWorker != null) {
            currentWorker.cancel(true);
            requestLinePanel.setSendButtonToSend(this::sendRequest);
            statusCodeLabel.setText("Status: Canceled");
            statusCodeLabel.setForeground(new Color(255, 140, 0));
            return;
        }
        HttpRequestItem item = getCurrentRequest();
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        Postman postman = new Postman(activeEnv);
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("request", item);
        bindings.put("env", activeEnv);
        bindings.put("postman", postman);
        bindings.put("pm", postman);
        // prescript 执行
        String prescript = item.getPrescript();
        if (prescript != null && !prescript.isBlank()) {
            try {
                bindings.put("request", item);
                JsScriptExecutor.executeScript(
                        prescript,
                        bindings,
                        output -> {
                            if (!output.isBlank()) {
                                SidebarTabPanel.appendConsoleLog("[PreScript Console]\n" + output);
                            }
                        }
                );
            } catch (Exception ex) {
                log.error("前置脚本执行异常: {}", ex.getMessage(), ex);
                SidebarTabPanel.appendConsoleLog("[PreScript Error] " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "前置脚本执行异常：" + ex.getMessage(), "脚本错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        PreparedRequest req = HttpRequestExecutor.buildPreparedRequest(item);
        if (req.url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入有效的 URL");
            return;
        }
        if (req.method == null || req.method.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择请求方法");
            return;
        }
        if (req.body != null && "GET".equalsIgnoreCase(req.method) && item.getBody() != null && !item.getBody().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "GET 请求通常不包含请求体，是否继续发送？",
                    "确认",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        statusCodeLabel.setText("Status: Requesting...");
        statusCodeLabel.setForeground(new Color(255, 140, 0));
        responseTimeLabel.setText("Duration: --");
        responseSizeLabel.setText("ResponseSize: --");
        long startTime = System.currentTimeMillis();
        // 切换按钮为 Cancel
        requestLinePanel.setSendButtonToCancel(this::sendRequest);
        currentWorker = new SwingWorker<>() {
            String requestHeadersText;
            String statusText;
            String headersText;
            String bodyText;
            int statusCode = 0;
            long responseTime;
            String redirectChainText = "";
            List<HttpRequestExecutor.RedirectInfo> redirectInfos;
            HttpResponse resp;

            @Override
            protected Void doInBackground() {
                try {
                    StringBuilder reqHeadersBuilder = new StringBuilder();
                    req.headers.forEach((key, value) -> {
                        if (key != null) {
                            reqHeadersBuilder.append(key).append(": ").append(String.join(", ", value)).append("\n");
                        }
                    });
                    requestHeadersText = reqHeadersBuilder.toString();
                    HttpRequestExecutor.ResponseWithRedirects respWithRedirects = HttpRequestExecutor.executeWithRedirects(req, 10);
                    resp = respWithRedirects.finalResponse;
                    redirectInfos = respWithRedirects.redirects;
                    StringBuilder chainBuilder = getRedirctChainStringBuilder();
                    redirectChainText = chainBuilder.toString();
                    statusText = (resp.code > 0 ? String.valueOf(resp.code) : "Unknown Status");
                    statusCode = resp.code;
                    StringBuilder headersBuilder = new StringBuilder();
                    resp.headers.forEach((key, value) -> {
                        if (key != null) {
                            headersBuilder.append(key).append(": ").append(String.join(", ", value)).append("\n");
                        }
                    });
                    headersText = headersBuilder.toString();
                    bodyText = resp.body;
                    responseTime = System.currentTimeMillis() - startTime;
                    return null;
                } catch (InterruptedIOException ignore) {
                    log.info("{} 请求被取消", req.url);
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    statusText = "发生错误: " + ex.getMessage();
                    headersText = "";
                    bodyText = ex.getMessage();
                    responseTime = System.currentTimeMillis() - startTime;
                }
                return null;
            }

            private StringBuilder getRedirctChainStringBuilder() {
                // 构建重定向链文本
                StringBuilder chainBuilder = new StringBuilder();
                if (CollUtil.size(redirectInfos) < 2) { // 如果没有重定向或只有一个请求
                    chainBuilder.append("No redirects\n");
                    return chainBuilder;
                }
                for (int i = 0; i < redirectInfos.size(); i++) {
                    HttpRequestExecutor.RedirectInfo info = redirectInfos.get(i);
                    chainBuilder.append("[").append(i + 1).append("] ")
                            .append(info.url).append("\n");
                    if (info.location != null) {
                        chainBuilder.append("  Location: ").append(info.location).append("\n");
                    }
                    if (info.headers != null) {
                        for (Map.Entry<String, List<String>> entry : info.headers.entrySet()) {
                            if (entry.getKey() != null) {
                                chainBuilder.append("  ").append(entry.getKey()).append(": ")
                                        .append(String.join(", ", entry.getValue())).append("\n");
                            }
                        }
                    }
                    chainBuilder.append("\n");
                }
                return chainBuilder;
            }

            @Override
            protected void done() {
                responseHeadersPanel.setHeadersText(headersText);
                if (resp != null) {
                    setResponseBody(resp);
                } else {
                    responseBodyPanel.setBodyText(null);
                    if (redirectChainArea != null) {
                        redirectChainArea.setText(redirectChainText);
                    }
                }
                responseHeadersPanel.getResponseHeadersArea().setCaretPosition(0);
                if (redirectChainArea != null) {
                    redirectChainArea.setText(redirectChainText);
                }
                Color statusColor;
                if (statusCode >= 200 && statusCode < 300) {
                    statusColor = new Color(0, 150, 0);
                } else if (statusCode >= 400 && statusCode < 500) {
                    statusColor = new Color(230, 130, 0);
                } else if (statusCode >= 500) {
                    statusColor = new Color(200, 0, 0);
                } else if (statusCode >= 300) {
                    statusColor = new Color(0, 120, 200);
                } else {
                    statusColor = Color.DARK_GRAY;
                }
                statusCodeLabel.setText("Status: " + statusText);
                statusCodeLabel.setForeground(statusColor);
                responseTimeLabel.setText(String.format("Duration: %d ms", responseTime));
                String sizeText;
                int bytes = resp.bodySize;
                if (bytes < 1024) {
                    sizeText = String.format("ResponseSize: %d B", bytes);
                } else if (bytes < 1024 * 1024) {
                    sizeText = String.format("ResponseSize: %.1f KB", bytes / 1024.0);
                } else {
                    sizeText = String.format("ResponseSize: %.1f MB", bytes / (1024.0 * 1024.0));
                }
                responseSizeLabel.setText(sizeText);

                // postscript 执行
                String postscript = item.getPostscript();
                if (postscript != null && !postscript.isBlank() && resp != null) {
                    try {
                        bindings.put("responseBody", bodyText);
                        bindings.put("responseHeaders", headersText);
                        bindings.put("status", statusText);
                        bindings.put("statusCode", statusCode);
                        JsScriptExecutor.executeScript(
                                postscript,
                                bindings,
                                output -> {
                                    if (!output.isBlank()) {
                                        SidebarTabPanel.appendConsoleLog("[PostScript Console]\n" + output);
                                    }
                                }
                        );
                        Environment activeEnv = EnvironmentService.getActiveEnvironment();
                        if (activeEnv != null) {
                            EnvironmentService.saveEnvironment(activeEnv);
                            refreshEnvironmentPanel();
                        }
                    } catch (Exception ex) {
                        log.error("后置脚本执行异常: {}", ex.getMessage(), ex);
                        SidebarTabPanel.appendConsoleLog("[PostScript Error] " + ex.getMessage());
                        JOptionPane.showMessageDialog(RequestEditSubPanel.this, "后置脚本执行异常：" + ex.getMessage(), "脚本错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
                if (bodyText != null) {
                    autoExecuteExtractorRules(bodyText);
                }
                // 保存到历史
                if (resp != null) {
                    SingletonFactory.getInstance(HistoryPanel.class).addRequestHistory(req, resp);
                }
                // 恢复按钮为 Send
                requestLinePanel.setSendButtonToSend(RequestEditSubPanel.this::sendRequest);
                currentWorker = null;
            }
        };
        currentWorker.execute();
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
        if (!item.getHeaders().containsKey("User-Agent")) {
            item.getHeaders().put("User-Agent", "EasyPostman HTTP Client");
            headersPanel.addRow("User-Agent", "EasyPostman HTTP Client");
        }
        if (!item.getHeaders().containsKey("Accept")) {
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
        // 变量提取规则
        loadExtractorRulesFromRequest(item);

        // 认证Tab
        authTabPanel.setAuthType(item.getAuthType());
        authTabPanel.setUsername(item.getAuthUsername());
        authTabPanel.setPassword(item.getAuthPassword());
        authTabPanel.setToken(item.getAuthToken());

        // 前置/后置脚本
        scriptPanel.setPrescript(item.getPrescript() == null ? "" : item.getPrescript());
        scriptPanel.setPostscript(item.getPostscript() == null ? "" : item.getPostscript());
        // 自动重定向复选框
        requestLinePanel.getFollowRedirectsCheckBox().setSelected(item.isFollowRedirects);

        // 设置原始数据用于脏检测
        setOriginalRequestItem(item);
    }

    @NotNull
    private static Map<String, String> getMergedParams(Map<String, String> params, String url) {
        Map<String, String> urlParams = new LinkedHashMap<>();
        if (url != null && url.contains("?")) {
            int idx = url.indexOf('?');
            String paramStr = url.substring(idx + 1);
            // 拆解参数并urldecode
            int last = 0;
            while (last < paramStr.length()) {
                int amp = paramStr.indexOf('&', last);
                String pair = (amp == -1) ? paramStr.substring(last) : paramStr.substring(last, amp);
                int eqIdx = pair.indexOf('=');
                if (eqIdx > 0) {
                    String k = pair.substring(0, eqIdx);
                    String v = pair.substring(eqIdx + 1);
                    urlParams.put(k, v);
                } else if (!pair.isEmpty()) {
                    urlParams.put(pair, "");
                }
                if (amp == -1) break;
                last = amp + 1;
            }
        }
        // 合并 params，item.getParams() 优先生效
        Map<String, String> mergedParams = new LinkedHashMap<>(urlParams);
        if (params != null) {
            mergedParams.putAll(params);
        }
        return mergedParams;
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
        // 提取规则处理
        if (extractorPanel != null) {
            item.setExtractorRules(extractorPanel.getExtractorRules());
            item.setAutoExtractVariables(extractorPanel.isAutoExtract());
        }
        // 认证Tab收集
        item.setAuthType(authTabPanel.getAuthType());
        item.setAuthUsername(authTabPanel.getUsername());
        item.setAuthPassword(authTabPanel.getPassword());
        item.setAuthToken(authTabPanel.getToken());
        // 脚本内容
        item.setPrescript(scriptPanel.getPrescript());
        item.setPostscript(scriptPanel.getPostscript());
        // 自动重定向
        item.setIsFollowRedirects(requestLinePanel.getFollowRedirectsCheckBox().isSelected());
        return item;
    }

    /**
     * 从请求对象加载提取规则到表格中
     */
    private void loadExtractorRulesFromRequest(HttpRequestItem item) {
        if (extractorPanel != null) {
            extractorPanel.loadExtractorRules(item.getExtractorRules(), item.isAutoExtractVariables());
        }
    }

    /**
     * 自动执行所有变量提取规则
     */
    private void autoExecuteExtractorRules(String responseText) {
        if (extractorPanel != null) {
            extractorPanel.setRawResponseBodyText(responseText);
            extractorPanel.autoExecuteExtractorRules();
        }
    }

    /**
     * 刷新环境面板
     * 通过查找并调用环境面板的刷新方法来更新环境变量显示
     */
    private void refreshEnvironmentPanel() {
        SingletonFactory.getInstance(EnvironmentPanel.class).refreshUI();
    }


    /**
     * 解析url中的参数到paramsPanel，并与现有params合并去重
     */
    private void parseUrlParamsToParamsPanel() {
        String url = urlField.getText();
        if (url == null) return;
        int idx = url.indexOf('?');
        if (idx < 0 || idx == url.length() - 1) return;
        String paramStr = url.substring(idx + 1);
        if (!paramStr.contains("=")) return; // 没有=号，不解析为参数
        Map<String, String> urlParams = new LinkedHashMap<>();
        int last = 0;
        while (last < paramStr.length()) {
            int amp = paramStr.indexOf('&', last);
            String pair = (amp == -1) ? paramStr.substring(last) : paramStr.substring(last, amp);
            int eqIdx = pair.indexOf('=');
            String k, v;
            if (eqIdx >= 0) {
                k = pair.substring(0, eqIdx);
                v = pair.substring(eqIdx + 1);
            } else {
                // 没有=号的pair不处理
                last = (amp == -1) ? paramStr.length() : amp + 1;
                continue;
            }
            if (StrUtil.isNotBlank(k) && StrUtil.isNotBlank(v)) {
                urlParams.put(k, v);
            }
            if (amp == -1) break;
            last = amp + 1;
        }
        if (urlParams.isEmpty()) {
            return;
        }
        Map<String, String> merged = new LinkedHashMap<>(paramsPanel.getMap());
        merged.putAll(urlParams);
        // 清空并填充paramsPanel
        paramsPanel.clear();
        for (java.util.Map.Entry<String, String> entry : merged.entrySet()) {
            paramsPanel.addRow(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 如果 headers 中不包含指定 Content-Type，则补充，并同步更新 headersPanel
     */
    private void ensureContentTypeHeader(Map<String, String> headers, String contentType, EasyNameValueTablePanel headersPanel) {
        if (!HttpUtil.containsContentType(headers, contentType)) {
            headers.put("Content-Type", contentType);
            headersPanel.addRow("Content-Type", contentType);
        }
    }
}