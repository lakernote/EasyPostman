package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.button.*;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import tools.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Elasticsearch 可视化 CRUD + DSL 工具面板
 */
@Slf4j
public class ElasticsearchPanel extends JPanel {

    // ===== 连接 =====
    private JTextField hostField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectBtn;
    private JLabel connectionStatusLabel;

    // ===== 索引管理 =====
    private DefaultListModel<String> indexListModel;
    private DefaultListModel<String> indexFilteredModel;
    private JList<String> indexList;
    private JTextField newIndexField;
    private JSpinner shardSpinner;
    private JSpinner replicaSpinner;

    // ===== DSL 编辑器 =====
    private JComboBox<String> templateCombo;
    private JComboBox<String> methodCombo;
    private JTextField pathField;
    private RSyntaxTextArea dslEditor;
    private RSyntaxTextArea resultArea;

    // ===== 结果表格 =====
    private com.laker.postman.common.component.table.EnhancedTablePanel enhancedTable;
    private JTabbedPane resultTabs;
    private JLabel respStatusLabel;   // 响应行：状态码 + 耗时
    private PrimaryButton executeBtn; // 保存引用以控制 loading 状态

    // ===== HTTP 客户端 =====
    private transient OkHttpClient httpClient;
    private String baseUrl = "http://localhost:9200";
    private String authHeader = null;
    private boolean connected = false;

    // ===== 常量 =====
    private static final String SEPARATOR_FG = "Separator.foreground";
    private static final String SEARCH_PATH = "/{index}/_search";
    private static final String HTTP_DELETE = "DELETE";
    private static final String CLUSTER_HEALTH_PATH = "/_cluster/health";
    private static final String JSON_UTF8 = "application/json; charset=utf-8";
    private static final String JSON_MIME = "application/json";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String CLIENT_PROP_TOTAL_HITS = "es.totalHits";
    private static final String LABEL_DISABLED_FG = "Label.disabledForeground";

    // ===== 内置 DSL 模板（名称走 i18n，DSL body 保持英文原样）=====
    private static final String[][] DSL_TEMPLATES = {
            {"toolbox.es.tpl.match_all", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 60\n}"},
            {"toolbox.es.tpl.match", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"match\": {\n      \"field\": \"value\"\n    }\n  }\n}"},
            {"toolbox.es.tpl.term", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"term\": {\n      \"field.keyword\": \"exact_value\"\n    }\n  }\n}"},
            {"toolbox.es.tpl.range", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"range\": {\n      \"timestamp\": {\n        \"gte\": \"2024-01-01\",\n        \"lte\": \"2024-12-31\"\n      }\n    }\n  }\n}"},
            {"toolbox.es.tpl.bool", "GET", SEARCH_PATH,
                    "{\n  \"query\": {\n    \"bool\": {\n      \"must\": [\n        { \"match\": { \"title\": \"elasticsearch\" } }\n      ],\n      \"filter\": [\n        { \"term\": { \"status\": \"active\" } }\n      ],\n      \"must_not\": [],\n      \"should\": []\n    }\n  }\n}"},
            {"toolbox.es.tpl.agg", "GET", SEARCH_PATH,
                    "{\n  \"size\": 0,\n  \"aggs\": {\n    \"group_by_status\": {\n      \"terms\": {\n        \"field\": \"status.keyword\",\n        \"size\": 10\n      }\n    }\n  }\n}"},
            {"toolbox.es.tpl.index_doc", "POST", "/{index}/_doc",
                    "{\n  \"field1\": \"value1\",\n  \"field2\": \"value2\",\n  \"timestamp\": \"2024-01-01T00:00:00Z\"\n}"},
            {"toolbox.es.tpl.update_doc", "POST", "/{index}/_update/{id}",
                    "{\n  \"doc\": {\n    \"field\": \"new_value\"\n  }\n}"},
            {"toolbox.es.tpl.delete_doc", HTTP_DELETE, "/{index}/_doc/{id}", ""},
            {"toolbox.es.tpl.get_doc", "GET", "/{index}/_doc/{id}", ""},
            {"toolbox.es.tpl.mapping", "GET", "/{index}/_mapping", ""},
            {"toolbox.es.tpl.settings", "GET", "/{index}/_settings", ""},
            {"toolbox.es.tpl.health", "GET", CLUSTER_HEALTH_PATH, ""},
            {"toolbox.es.tpl.cluster_stats", "GET", "/_cluster/stats", ""},
            {"toolbox.es.tpl.list_indices", "GET", "/_cat/indices?v&format=json", ""},
            {"toolbox.es.tpl.create_index", "PUT", "/{index}",
                    "{\n  \"settings\": {\n    \"number_of_shards\": 1,\n    \"number_of_replicas\": 1\n  },\n  \"mappings\": {\n    \"properties\": {\n      \"title\": { \"type\": \"text\" },\n      \"status\": { \"type\": \"keyword\" },\n      \"timestamp\": { \"type\": \"date\" }\n    }\n  }\n}"},
            {"toolbox.es.tpl.reindex", "POST", "/_reindex",
                    "{\n  \"source\": {\n    \"index\": \"source_index\"\n  },\n  \"dest\": {\n    \"index\": \"dest_index\"\n  }\n}"},
            {"toolbox.es.tpl.delete_by_query", "POST", "/{index}/_delete_by_query",
                    "{\n  \"query\": {\n    \"match\": {\n      \"field\": \"value\"\n    }\n  }\n}"},
            {"toolbox.es.tpl.update_by_query", "POST", "/{index}/_update_by_query",
                    "{\n  \"script\": {\n    \"source\": \"ctx._source.status = 'updated'\"\n  },\n  \"query\": {\n    \"match_all\": {}\n  }\n}"},
    };

    public ElasticsearchPanel() {
        initHttpClient();
        initUI();
    }

    private void initHttpClient() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 顶部连接栏
        add(buildConnectionPanel(), BorderLayout.NORTH);

        // 主内容区：左侧索引树 + 右侧 DSL 编辑器
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildIndexPanel(), buildDslPanel());
        mainSplit.setDividerLocation(230);
        mainSplit.setDividerSize(5);
        mainSplit.setContinuousLayout(true);
        add(mainSplit, BorderLayout.CENTER);
    }

    // ===== 连接面板 =====
    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        hostField = new JTextField("http://localhost:9200", 26);
        hostField.setPreferredSize(new Dimension(hostField.getPreferredSize().width, 32));
        hostField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HOST_PLACEHOLDER));
        // 回车直接触发连接
        hostField.addActionListener(e -> doConnect());

        usernameField = new JTextField("", 10);
        usernameField.setPreferredSize(new Dimension(usernameField.getPreferredSize().width, 32));
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_USER_PLACEHOLDER));

        passwordField = new JPasswordField("", 10);
        passwordField.setPreferredSize(new Dimension(passwordField.getPreferredSize().width, 32));
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PASS_PLACEHOLDER));
        // 密码框回车也触发连接
        passwordField.addActionListener(e -> doConnect());

        connectionStatusLabel = new JLabel("●");
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setFont(connectionStatusLabel.getFont().deriveFont(Font.BOLD, 14f));
        connectionStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_NOT_CONNECTED));

        connectBtn = new PrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CONNECT), "icons/connect.svg");
        connectBtn.addActionListener(e -> doConnect());

        row.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HOST)));
        row.add(hostField);
        row.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_USER)));
        row.add(usernameField);
        row.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PASS)));
        row.add(passwordField);
        row.add(connectBtn);
        row.add(connectionStatusLabel);

        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    // ===== 索引管理面板 =====
    private JPanel buildIndexPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setPreferredSize(new Dimension(220, 0));

        // 顶部标题栏 + 刷新按钮
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(4, 8, 4, 4)));
        JLabel titleLbl = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_MANAGEMENT));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));
        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.addActionListener(e -> loadIndices());
        titleBar.add(titleLbl, BorderLayout.CENTER);
        titleBar.add(refreshBtn, BorderLayout.EAST);

        // 搜索框
        com.laker.postman.common.component.SearchTextField indexSearchField =
                new com.laker.postman.common.component.SearchTextField();
        indexSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
        searchBox.add(indexSearchField, BorderLayout.CENTER);

        // 索引列表（使用过滤 model）
        indexListModel = new DefaultListModel<>();
        indexFilteredModel = new DefaultListModel<>();
        indexList = new JList<>(indexFilteredModel);
        indexList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        indexList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = indexList.getSelectedValue();
                if (selected != null) pathField.setText("/" + selected + "/_search");
            }
        });
        indexList.addMouseListener(buildIndexListMouseListener());
        JScrollPane listScroll = new JScrollPane(indexList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());

        // 搜索监听：实时过滤
        indexSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterIndices(indexSearchField.getText());
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterIndices(indexSearchField.getText());
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterIndices(indexSearchField.getText());
            }
        });

        // 操作按钮行已移除（删除按钮去掉）

        // 创建索引面板
        JPanel createPanel = new JPanel(new GridBagLayout());
        createPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        newIndexField = new JTextField();
        newIndexField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_NAME_PLACEHOLDER));
        shardSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        replicaSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
        PrimaryButton createBtn = new PrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CREATE), "icons/plus.svg");
        createBtn.addActionListener(e -> createIndex());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        createPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_NAME)), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        createPanel.add(newIndexField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        createPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_SHARDS)), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        createPanel.add(shardSpinner, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        createPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_REPLICAS)), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        createPanel.add(replicaSpinner, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        createPanel.add(createBtn, gbc);

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.add(titleBar, BorderLayout.NORTH);
        topArea.add(searchBox, BorderLayout.CENTER);

        panel.add(topArea, BorderLayout.NORTH);
        panel.add(listScroll, BorderLayout.CENTER);
        panel.add(createPanel, BorderLayout.SOUTH);
        return panel;
    }

    private java.awt.event.MouseAdapter buildIndexListMouseListener() {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) maybeShowIndexPopup(evt);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) maybeShowIndexPopup(evt);
            }
        };
    }

    private void maybeShowIndexPopup(java.awt.event.MouseEvent evt) {
        int idx = indexList.locationToIndex(evt.getPoint());
        if (idx >= 0) indexList.setSelectedIndex(idx);
        String sel = indexList.getSelectedValue();

        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE));
        deleteItem.setEnabled(sel != null);
        deleteItem.addActionListener(e -> deleteIndex(sel));
        menu.add(deleteItem);

        JMenuItem clearItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR));
        clearItem.setEnabled(sel != null);
        clearItem.addActionListener(e -> clearIndex(sel));
        menu.add(clearItem);

        menu.addSeparator();
        menu.add(buildViewMappingItem(sel));
        menu.add(buildViewSettingsItem(sel));
        menu.show(indexList, evt.getX(), evt.getY());
    }

    private JMenuItem buildViewMappingItem(String sel) {
        JMenuItem item = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_VIEW_MAPPING));
        item.setEnabled(sel != null);
        item.addActionListener(e -> {
            if (sel != null) {
                pathField.setText("/" + sel + "/_mapping");
                methodCombo.setSelectedItem("GET");
                dslEditor.setText("");
                executeRequest();
            }
        });
        return item;
    }

    private JMenuItem buildViewSettingsItem(String sel) {
        JMenuItem item = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_VIEW_SETTINGS));
        item.setEnabled(sel != null);
        item.addActionListener(e -> {
            if (sel != null) {
                pathField.setText("/" + sel + "/_settings");
                methodCombo.setSelectedItem("GET");
                dslEditor.setText("");
                executeRequest();
            }
        });
        return item;
    }

    /**
     * 根据关键词过滤索引列表
     */
    private void filterIndices(String kw) {
        String lower = kw == null ? "" : kw.trim().toLowerCase();
        indexFilteredModel.clear();
        for (int i = 0; i < indexListModel.size(); i++) {
            String name = indexListModel.get(i);
            if (lower.isEmpty() || name.toLowerCase().contains(lower)) {
                indexFilteredModel.addElement(name);
            }
        }
    }

    /**
     * 解析 _cat/indices JSON 并填充索引列表 Model
     */
    private void parseAndFillIndexList(String json) {
        try {
            indexListModel.clear();
            JsonNode arr = JsonUtil.readTree(json);
            if (!arr.isArray()) return;
            for (JsonNode node : arr) {
                JsonNode idx = node.get("index");
                if (idx != null && !idx.isNull()) {
                    indexListModel.addElement(idx.toString().replace("\"", ""));
                }
            }
        } catch (Exception e) {
            log.warn("parseAndFillIndexList error: {}", e.getMessage());
        }
    }

    // ===== DSL 编辑 + 结果面板 =====
    private JPanel buildDslPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        // ---- 工具栏：所有按钮一行 ----
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                UIManager.getColor(SEPARATOR_FG)));

        templateCombo = new JComboBox<>();
        for (String[] t : DSL_TEMPLATES) templateCombo.addItem(I18nUtil.getMessage(t[0]));
        templateCombo.setPreferredSize(new Dimension(180, 28));
        SecondaryButton loadTplBtn = new SecondaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_LOAD_TEMPLATE), "icons/load.svg");
        loadTplBtn.addActionListener(e -> applyTemplate(templateCombo.getSelectedIndex()));
        FormatButton formatBtn = new FormatButton();
        formatBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_FORMAT_JSON));
        formatBtn.addActionListener(e -> formatDsl());
        CopyButton copyBtn = new CopyButton();
        copyBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_COPY_RESULT));
        copyBtn.addActionListener(e -> {
            copyResult();
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_RESULT_COPIED));
        });
        ClearButton clearBtn = new ClearButton();
        clearBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CLEAR));
        clearBtn.addActionListener(e -> {
            dslEditor.setText("");
            resultArea.setText("");
            clearTable();
            respStatusLabel.setText("");
        });

        toolBar.add(templateCombo);
        toolBar.add(loadTplBtn);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL));
        toolBar.add(formatBtn);
        toolBar.add(copyBtn);
        toolBar.add(clearBtn);

        // ---- 请求行：Method + Path + Execute ----
        JPanel requestRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        requestRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        methodCombo = new JComboBox<>(new String[]{"GET", "POST", "PUT", HTTP_DELETE, "HEAD"});
        methodCombo.setPreferredSize(new Dimension(90, 32));
        pathField = new JTextField(CLUSTER_HEALTH_PATH);
        pathField.setPreferredSize(new Dimension(400, 32));
        pathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PATH_PLACEHOLDER));
        executeBtn = new PrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_EXECUTE), "icons/send.svg");
        executeBtn.addActionListener(e -> executeRequest());
        registerCtrlEnterShortcut(executeBtn);
        requestRow.add(methodCombo);
        requestRow.add(pathField);
        requestRow.add(executeBtn);

        JPanel topArea = new JPanel(new BorderLayout(0, 4));
        topArea.add(toolBar, BorderLayout.NORTH);
        topArea.add(requestRow, BorderLayout.CENTER);
        panel.add(topArea, BorderLayout.NORTH);

        // ---- 主分割：DSL 编辑器(上) + 结果(下) ----
        JSplitPane editorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        editorSplit.setDividerSize(5);
        editorSplit.setContinuousLayout(true);
        editorSplit.setResizeWeight(0.4);
        editorSplit.setBorder(BorderFactory.createEmptyBorder());

        // DSL 编辑器
        JPanel editorPanel = new JPanel(new BorderLayout());
        JLabel dslLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DSL_TITLE));
        dslLabel.setFont(dslLabel.getFont().deriveFont(Font.BOLD, 11f));
        dslLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        dslEditor = createJsonEditor(true);
        dslEditor.setText("{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 60\n}");
        RTextScrollPane editorScroll = new RTextScrollPane(dslEditor);
        editorScroll.setLineNumbersEnabled(true);
        editorScroll.setBorder(BorderFactory.createEmptyBorder());
        editorPanel.add(dslLabel, BorderLayout.NORTH);
        editorPanel.add(editorScroll, BorderLayout.CENTER);

        // 结果区（Tab: 表格 + 原始 JSON）
        JPanel resultPanel = new JPanel(new BorderLayout());

        // 响应标题栏：左侧标签 + 右侧状态码/耗时
        JPanel respHeader = new JPanel(new BorderLayout());
        respHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        JLabel respLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_RESPONSE_TITLE));
        respLabel.setFont(respLabel.getFont().deriveFont(Font.BOLD, 11f));
        respStatusLabel = new JLabel("");
        respStatusLabel.setFont(respStatusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        respStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        respHeader.add(respLabel, BorderLayout.WEST);
        respHeader.add(respStatusLabel, BorderLayout.EAST);

        resultTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        enhancedTable = new com.laker.postman.common.component.table.EnhancedTablePanel(new String[]{});
        resultArea = createJsonEditor(false);
        RTextScrollPane resultScroll = new RTextScrollPane(resultArea);
        resultScroll.setLineNumbersEnabled(true);
        resultScroll.setBorder(BorderFactory.createEmptyBorder());

        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TAB_TABLE), enhancedTable);
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TAB_RAW), resultScroll);

        resultPanel.add(respHeader, BorderLayout.NORTH);
        resultPanel.add(resultTabs, BorderLayout.CENTER);

        editorSplit.setTopComponent(editorPanel);
        editorSplit.setBottomComponent(resultPanel);

        panel.add(editorSplit, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 注册 Ctrl+Enter 快捷键
     */
    private void registerCtrlEnterShortcut(JButton executeBtn) {
        SwingUtilities.invokeLater(() -> {
            if (dslEditor != null) {
                dslEditor.getInputMap().put(
                        KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER,
                                java.awt.event.InputEvent.CTRL_DOWN_MASK),
                        "executeRequest");
                dslEditor.getActionMap().put("executeRequest", new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        executeBtn.doClick();
                    }
                });
            }
        });
    }

    // ===== 核心功能方法 =====

    /**
     * 删除索引（右键菜单触发）
     */
    private void deleteIndex(String indexName) {
        if (!connected || indexName == null) return;
        int opt = JOptionPane.showConfirmDialog(this,
                MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_CONFIRM), indexName),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return doDelete("/" + indexName, "");
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    resultArea.setText(JsonUtil.toJsonPrettyStr(resp));
                    resultArea.setCaretPosition(0);
                    loadIndices();
                    NotificationUtil.showSuccess(
                            MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_SUCCESS), indexName));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("deleteIndex interrupted", ex);
                } catch (Exception ex) {
                    NotificationUtil.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_FAILED),
                            ex.getMessage()));
                }
            }
        }.execute();
    }

    /**
     * 清空索引数据（delete_by_query match_all，保留索引结构）
     */
    private void clearIndex(String indexName) {
        if (!connected || indexName == null) return;
        int opt = JOptionPane.showConfirmDialog(this,
                MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_CONFIRM), indexName),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        String body = "{\n  \"query\": {\n    \"match_all\": {}\n  }\n}";
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return doPost("/" + indexName + "/_delete_by_query", body);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    resultArea.setText(JsonUtil.toJsonPrettyStr(resp));
                    resultArea.setCaretPosition(0);
                    NotificationUtil.showSuccess(
                            MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_SUCCESS), indexName));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("clearIndex interrupted", ex);
                } catch (Exception ex) {
                    NotificationUtil.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_FAILED),
                            ex.getMessage()));
                }
            }
        }.execute();
    }

    private void doConnect() {
        String url = hostField.getText().trim();
        if (url.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_HOST_REQUIRED));
            return;
        }
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        baseUrl = url;
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();
        if (!user.isEmpty()) {
            String cred = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
            authHeader = "Basic " + cred;
        } else {
            authHeader = null;
        }
        connectBtn.setEnabled(false);
        final String finalUrl = baseUrl;
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return doGet(CLUSTER_HEALTH_PATH, "");
            }

            @Override
            protected void done() {
                connectBtn.setEnabled(true);
                try {
                    String resp = get();
                    connected = true;
                    connectionStatusLabel.setForeground(new Color(0, 180, 0));
                    connectionStatusLabel.setToolTipText(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_CONNECTED), finalUrl));
                    resultArea.setText(JsonUtil.toJsonPrettyStr(resp));
                    resultArea.setCaretPosition(0);
                    loadIndices();
                    NotificationUtil.showSuccess(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CONNECT_SUCCESS), finalUrl));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("doConnect interrupted", ex);
                } catch (Exception ex) {
                    connected = false;
                    connectionStatusLabel.setForeground(Color.RED);
                    connectionStatusLabel.setToolTipText(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_NOT_CONNECTED));
                    NotificationUtil.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_CONNECT_FAILED),
                            ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    private void loadIndices() {
        if (!connected) return;
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return doGet("/_cat/indices?v&format=json&s=index", "");
            }

            @Override
            protected void done() {
                try {
                    parseAndFillIndexList(get());
                    filterIndices("");
                    if (indexListModel.isEmpty()) {
                        NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_LIST_EMPTY));
                    } else {
                        // 自动选中第一个索引并加载第一个模版
                        indexList.setSelectedIndex(0);
                        applyTemplate(0);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("loadIndices interrupted", ex);
                } catch (Exception ex) {
                    log.warn("Failed to load indices: {}", ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void applyTemplate(int idx) {
        if (idx < 0 || idx >= DSL_TEMPLATES.length) return;
        String selectedIndex = indexList.getSelectedValue();
        String path = DSL_TEMPLATES[idx][2];
        if (selectedIndex != null && !selectedIndex.isBlank()) {
            path = path.replace("{index}", selectedIndex);
        }
        methodCombo.setSelectedItem(DSL_TEMPLATES[idx][1]);
        pathField.setText(path);
        dslEditor.setText(DSL_TEMPLATES[idx][3]);
        dslEditor.setCaretPosition(0);
    }

    private void createIndex() {
        if (!connected) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_NOT_CONNECTED));
            return;
        }
        String name = newIndexField.getText().trim();
        if (name.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_NAME_REQUIRED));
            return;
        }
        int shards = (int) shardSpinner.getValue();
        int replicas = (int) replicaSpinner.getValue();
        String body = "{\n  \"settings\": {\n    \"number_of_shards\": " + shards
                + ",\n    \"number_of_replicas\": " + replicas + "\n  }\n}";
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return doPut("/" + name, body);
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    resultArea.setText(JsonUtil.toJsonPrettyStr(resp));
                    resultArea.setCaretPosition(0);
                    newIndexField.setText("");
                    loadIndices();
                    NotificationUtil.showSuccess(
                            MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CREATE_SUCCESS), name));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("createIndex interrupted", ex);
                } catch (Exception ex) {
                    NotificationUtil.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CREATE_FAILED),
                            ex.getMessage()));
                }
            }
        };
        worker.execute();
    }


    private void executeRequest() {
        if (!connected) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_NOT_CONNECTED));
            return;
        }
        String selectedMethod = (String) methodCombo.getSelectedItem();
        final String method = (selectedMethod == null) ? "GET" : selectedMethod;
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_PATH_REQUIRED));
            return;
        }
        String body = dslEditor.getText().trim();
        clearTable();
        respStatusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_REQUESTING));
        respStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        executeBtn.setEnabled(false);
        long start = System.currentTimeMillis();
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return switch (method) {
                    case "POST" -> doPost(path, body);
                    case "PUT" -> doPut(path, body);
                    case HTTP_DELETE -> doDelete(path, body);
                    case "HEAD" -> doHead(path);
                    default -> doGet(path, body);
                };
            }

            @Override
            protected void done() {
                long elapsed = System.currentTimeMillis() - start;
                executeBtn.setEnabled(true);
                try {
                    handleRequestResult(get(), elapsed);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("executeRequest interrupted", ex);
                    respStatusLabel.setText("");
                } catch (Exception ex) {
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    resultArea.setText("Error: " + msg);
                    respStatusLabel.setText(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_ERROR), elapsed));
                    respStatusLabel.setForeground(UIManager.getColor("Actions.Red"));
                }
            }
        };
        worker.execute();
    }

    private void handleRequestResult(String resp, long elapsed) {
        String formatted = JsonUtil.isTypeJSON(resp) ? JsonUtil.toJsonPrettyStr(resp) : resp;
        resultArea.setText(formatted);
        resultArea.setCaretPosition(0);
        if (JsonUtil.isTypeJSON(resp)) {
            populateTable(resp);
        }
        resultTabs.setSelectedIndex(enhancedTable.getTable().getRowCount() > 0 ? 0 : 1);
        // 显示耗时
        respStatusLabel.setText(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_OK), elapsed));
        respStatusLabel.setForeground(new Color(0, 160, 0));
        // totalHits 日志
        Object totalHits = enhancedTable.getClientProperty(CLIENT_PROP_TOTAL_HITS);
        if (totalHits instanceof Long th) {
            log.debug("ES totalHits={}, returned={}, elapsed={}ms",
                    th, enhancedTable.getTotalRowCount(), elapsed);
        }
    }

    private void populateTable(String json) {
        try {
            JsonNode root = JsonUtil.readTree(json);
            if (root.has("hits")) {
                populateHitsTable(root);
            } else if (json.trim().startsWith("[")) {
                populateArrayTable(json);
            } else {
                // 扁平 key-value 展示（_cluster/health、_stats 等）
                List<Object[]> rows = new ArrayList<>();
                flattenJson("", root, rows);
                if (!rows.isEmpty()) {
                    rebuildEnhancedTable(new String[]{"Key", "Value"}, rows);
                } else {
                    enhancedTable.clearData();
                }
            }
        } catch (Exception e) {
            log.debug("populateTable error (non-fatal): {}", e.getMessage());
            enhancedTable.clearData();
        }
    }

    private void populateHitsTable(JsonNode root) {
        JsonNode hits = root.get("hits");
        JsonNode hitsArr = hits.get("hits");
        if (hitsArr == null || !hitsArr.isArray() || hitsArr.isEmpty()) {
            enhancedTable.clearData();
            return;
        }
        long totalHits = parseTotalHits(hits.get("total"));
        List<String> colNames = buildHitColumns(hitsArr);
        List<Object[]> rows = buildHitRows(hitsArr, colNames);
        rebuildEnhancedTable(colNames.toArray(new String[0]), rows);
        enhancedTable.putClientProperty(CLIENT_PROP_TOTAL_HITS,
                totalHits > rows.size() ? totalHits : null);
    }

    private static long parseTotalHits(JsonNode totalNode) {
        if (totalNode == null) return -1;
        if (totalNode.isNumber()) return totalNode.longValue();
        if (totalNode.isObject() && totalNode.has("value")) return totalNode.get("value").longValue();
        return -1;
    }

    private static List<String> buildHitColumns(JsonNode hitsArr) {
        LinkedHashSet<String> srcCols = new LinkedHashSet<>();
        for (JsonNode hit : hitsArr) {
            JsonNode src = hit.get("_source");
            if (src != null) {
                src.properties().forEach(e -> srcCols.add(e.getKey()));
            }
        }
        List<String> colNames = new ArrayList<>();
        colNames.add("_index");
        colNames.add("_id");
        colNames.add("_score");
        colNames.addAll(srcCols);
        return colNames;
    }

    private static List<Object[]> buildHitRows(JsonNode hitsArr, List<String> colNames) {
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode hit : hitsArr) {
            rows.add(buildHitRow(hit, colNames));
        }
        return rows;
    }

    private static Object[] buildHitRow(JsonNode hit, List<String> colNames) {
        JsonNode src = hit.get("_source");
        Object[] row = new Object[colNames.size()];
        row[0] = nodeText(hit.get("_index"));
        row[1] = nodeText(hit.get("_id"));
        row[2] = nodeText(hit.get("_score"));
        if (src != null) {
            for (int c = 3; c < colNames.size(); c++) {
                row[c] = nodeText(src.get(colNames.get(c)));
            }
        }
        return row;
    }

    private void populateArrayTable(String json) {
        try {
            JsonNode arr = JsonUtil.readTree(json);
            if (!arr.isArray() || arr.isEmpty()) return;
            JsonNode first = arr.get(0);
            List<String> colNames = new ArrayList<>();
            for (java.util.Map.Entry<String, JsonNode> e : first.properties()) colNames.add(e.getKey());

            List<Object[]> rows = new ArrayList<>();
            for (JsonNode obj : arr) {
                Object[] row = new Object[colNames.size()];
                for (int c = 0; c < colNames.size(); c++) row[c] = nodeText(obj.get(colNames.get(c)));
                rows.add(row);
            }
            rebuildEnhancedTable(colNames.toArray(new String[0]), rows);
        } catch (Exception e) {
            log.debug("populateArrayTable error: {}", e.getMessage());
        }
    }

    private void flattenJson(String prefix, JsonNode obj, List<Object[]> rows) {
        for (java.util.Map.Entry<String, JsonNode> entry : obj.properties()) {
            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonNode v = entry.getValue();
            if (v.isObject()) flattenJson(fullKey, v, rows);
            else rows.add(new Object[]{fullKey, nodeText(v)});
        }
    }

    /**
     * 重置 EnhancedTablePanel 的列结构并填入数据（复用同一实例，不重建组件）
     */
    private void rebuildEnhancedTable(String[] cols, List<Object[]> rows) {
        enhancedTable.resetAndSetData(cols, rows);
    }

    private void clearTable() {
        enhancedTable.clearData();
    }

    /**
     * 安全地将 JsonNode 转为字符串，null 返回空串
     */
    private static String nodeText(JsonNode node) {
        if (node == null || node.isNull()) return "";
        return node.isValueNode() ? node.toString().replace("\"", "") : node.toString();
    }


    private void formatDsl() {
        String txt = dslEditor.getText().trim();
        if (txt.isEmpty()) return;
        if (JsonUtil.isTypeJSON(txt)) {
            dslEditor.setText(JsonUtil.toJsonPrettyStr(txt));
            dslEditor.setCaretPosition(0);
        }
    }

    private void copyResult() {
        String txt = resultArea.getText();
        if (txt != null && !txt.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(txt), null);
        }
    }

    // ===== HTTP 方法封装 =====

    /**
     * GET 请求。
     * ES 的 DSL 查询通过 body 传递（如 _search、_count 等）。
     * OkHttp 不允许 GET 带 body，因此有 body 时改用 POST 发送——ES 完全支持 POST _search。
     * 无 body 时发普通 GET（适用于 _cluster/health、_cat/indices 等）。
     */
    private String doGet(String path, String body) throws IOException {
        if (body != null && !body.isEmpty()) {
            RequestBody rb = RequestBody.create(body, MediaType.get(JSON_UTF8));
            return executeHttp(buildRequest("POST", path, rb));
        }
        return executeHttp(buildRequest("GET", path, null));
    }

    private String doPost(String path, String body) throws IOException {
        RequestBody rb = body.isEmpty()
                ? RequestBody.create(new byte[0], null)
                : RequestBody.create(body, MediaType.get(JSON_UTF8));
        return executeHttp(buildRequest("POST", path, rb));
    }

    private String doPut(String path, String body) throws IOException {
        RequestBody rb = body.isEmpty()
                ? RequestBody.create(new byte[0], MediaType.get(JSON_MIME))
                : RequestBody.create(body, MediaType.get(JSON_UTF8));
        return executeHttp(buildRequest("PUT", path, rb));
    }

    private String doDelete(String path, String body) throws IOException {
        RequestBody rb = (body != null && !body.isEmpty())
                ? RequestBody.create(body, MediaType.get(JSON_UTF8))
                : null;
        Request.Builder builder = new Request.Builder().url(baseUrl + path);
        if (authHeader != null) builder.header(HEADER_AUTHORIZATION, authHeader);
        builder.header("Content-Type", JSON_MIME);
        if (rb != null) builder.delete(rb);
        else builder.delete();
        return executeHttp(builder.build());
    }

    private String doHead(String path) throws IOException {
        Request.Builder builder = new Request.Builder().url(baseUrl + path).head();
        if (authHeader != null) builder.header(HEADER_AUTHORIZATION, authHeader);
        return executeHttp(builder.build());
    }

    private Request buildRequest(String method, String path, RequestBody body) {
        Request.Builder builder = new Request.Builder().url(baseUrl + path);
        if (authHeader != null) builder.header(HEADER_AUTHORIZATION, authHeader);
        builder.header("Content-Type", JSON_MIME);
        builder.method(method, body);
        return builder.build();
    }

    private String executeHttp(Request req) throws IOException {
        try (Response resp = httpClient.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful() && body.isEmpty()) {
                throw new IOException("HTTP " + resp.code() + " " + resp.message());
            }
            return body;
        }
    }

    // ===== 工具方法 =====

    private RSyntaxTextArea createJsonEditor(boolean editable) {
        RSyntaxTextArea area = new RSyntaxTextArea(10, 60);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setEditable(editable);
        EditorThemeUtil.loadTheme(area);
        return area;
    }
}
