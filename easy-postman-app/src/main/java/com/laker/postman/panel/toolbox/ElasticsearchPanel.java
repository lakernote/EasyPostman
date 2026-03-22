package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.button.*;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import okhttp3.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import tools.jackson.databind.JsonNode;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

/**
 * Elasticsearch 可视化 CRUD + DSL 工具面板
 */
@Slf4j
public class ElasticsearchPanel extends JPanel {

    // ===== 连接 =====
    private JComboBox<String> hostCombo;        // 带历史记录的 host 下拉
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JLabel connectionStatusLabel;
    private CardLayout btnCardLayout;   // 用于在 connectBtn/disconnectBtn 之间切换
    private JPanel btnCard;

    // ===== 索引管理 =====
    private DefaultListModel<String> indexListModel;
    private DefaultListModel<String> indexFilteredModel;
    private JList<String> indexList;
    private JTextField newIndexField;
    private JSpinner shardSpinner;
    private JSpinner replicaSpinner;
    /**
     * indexName -> docCount (从 _cat/indices 解析)
     */
    private final Map<String, Long> indexDocCountMap = new LinkedHashMap<>();

    // ===== DSL 编辑器 =====
    private JComboBox<String> templateCombo;
    private JComboBox<String> methodCombo;
    private JTextField pathField;
    private RSyntaxTextArea dslEditor;
    private RSyntaxTextArea resultArea;
    private SearchableTextArea searchableResultArea;
    private SearchableTextArea searchableDslArea;

    // ===== 结果表格 =====
    private com.laker.postman.common.component.table.EnhancedTablePanel enhancedTable;
    private com.laker.postman.common.component.table.EnhancedTablePanel aggTable;
    private JTabbedPane resultTabs;
    private JLabel respStatusLabel;
    private JLabel hitsInfoLabel;
    private PrimaryButton executeBtn;

    // ===== 请求历史 =====
    private static final int MAX_HISTORY = 20;
    /**
     * 每条记录格式: "METHOD PATH\nBODY"
     */
    private final java.util.Deque<HistoryEntry> requestHistory = new java.util.ArrayDeque<>();
    private DefaultListModel<HistoryEntry> historyListModel;
    private JList<HistoryEntry> historyList;

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
    private static final int MAX_HOST_HISTORY = 5;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int WRITE_TIMEOUT_MS = 30_000;
    // aggregation JSON field names
    private static final String AGG_KEY_AS_STRING = "key_as_string";
    private static final String AGG_KEY = "key";
    private static final String AGG_DOC_COUNT = "doc_count";
    private static final String AGG_VALUE = "value";
    private static final String AGG_BUCKETS = "buckets";

    // ===== 内置 DSL 模板 =====
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

    // ===== 历史记录条目 =====
    private static class HistoryEntry {
        final String method;
        final String path;
        final String body;
        final long time;

        HistoryEntry(String method, String path, String body) {
            this.method = method;
            this.path = path;
            this.body = body;
            this.time = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return method + "  " + path;
        }
    }

    public ElasticsearchPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 顶部连接栏
        add(buildConnectionPanel(), BorderLayout.NORTH);

        // 主内容区：左侧（索引+历史）+ 右侧 DSL
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildDslPanel());
        mainSplit.setDividerLocation(240);
        mainSplit.setDividerSize(3);
        mainSplit.setContinuousLayout(true);
        add(mainSplit, BorderLayout.CENTER);
    }

    // ===== 连接面板 =====
    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        // Host: 带历史记录的可编辑下拉框
        hostCombo = new JComboBox<>();
        hostCombo.setEditable(true);
        hostCombo.setPreferredSize(new Dimension(240, 32));
        hostCombo.addItem("http://localhost:9200");
        JTextField hostEditor = (JTextField) hostCombo.getEditor().getEditorComponent();
        hostEditor.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HOST_PLACEHOLDER));
        hostEditor.addActionListener(e -> doConnect());

        usernameField = new JTextField("", 10);
        usernameField.setPreferredSize(new Dimension(usernameField.getPreferredSize().width, 32));
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_USER_PLACEHOLDER));

        passwordField = new JPasswordField("", 10);
        passwordField.setPreferredSize(new Dimension(passwordField.getPreferredSize().width, 32));
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PASS_PLACEHOLDER));
        passwordField.addActionListener(e -> doConnect());

        connectionStatusLabel = new JLabel("●");
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setFont(connectionStatusLabel.getFont().deriveFont(Font.BOLD, 14f));
        connectionStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_NOT_CONNECTED));

        connectBtn = new PrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CONNECT), "icons/connect.svg");
        connectBtn.addActionListener(e -> doConnect());

        disconnectBtn = new SecondaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DISCONNECT), "icons/ws-close.svg");
        disconnectBtn.addActionListener(e -> doDisconnect());

        // 用 CardLayout 将 connectBtn / disconnectBtn 叠放在同一格，切换时不留空白
        btnCardLayout = new CardLayout();
        btnCard = new JPanel(btnCardLayout);
        btnCard.setOpaque(false);
        btnCard.add(connectBtn, "connect");
        btnCard.add(disconnectBtn, "disconnect");
        btnCardLayout.show(btnCard, "connect");


        // 使用 MigLayout 实现连接配置表单（insets 留出空间避免 FlatLaf focus 高亮被裁剪）
        JPanel form = new JPanel(new MigLayout(
                "insets 4 0 4 0, fillx",
                "[][grow,fill]8[][grow,fill]8[][grow,fill]8[]8[]",
                "[]"
        ));

        form.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HOST)));
        form.add(hostCombo);
        form.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_USER)));
        form.add(usernameField);
        form.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PASS)));
        form.add(passwordField);
        form.add(btnCard);
        form.add(connectionStatusLabel);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    // ===== 左侧面板：索引管理 + 历史记录（JTabbedPane）=====
    private JPanel buildLeftPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setPreferredSize(new Dimension(240, 0));

        JTabbedPane leftTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_MANAGEMENT), buildIndexPanel());
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY), buildHistoryPanel());

        wrapper.add(leftTabs, BorderLayout.CENTER);
        return wrapper;
    }

    // ===== 索引管理面板 =====
    private JPanel buildIndexPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder());

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

        // 索引列表
        indexListModel = new DefaultListModel<>();
        indexFilteredModel = new DefaultListModel<>();
        indexList = new JList<>(indexFilteredModel);
        indexList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        indexList.setCellRenderer(new IndexCellRenderer());
        indexList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                List<String> selected = indexList.getSelectedValuesList();
                if (selected.size() == 1) {
                    pathField.setText("/" + selected.get(0) + "/_search");
                }
            }
        });
        indexList.addMouseListener(buildIndexListMouseListener());
        JScrollPane listScroll = new JScrollPane(indexList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());

        // 实时过滤
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

        // 创建索引面板
        JPanel createPanel = new JPanel(new MigLayout(
                "insets 6, fillx",
                "[][grow,fill]",
                "[]2[]2[]2[]"
        ));
        createPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor(SEPARATOR_FG)));

        newIndexField = new JTextField();
        newIndexField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_NAME_PLACEHOLDER));
        shardSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        replicaSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
        PrimaryButton createBtn = new PrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CREATE), "icons/plus.svg");
        createBtn.addActionListener(e -> createIndex());

        createPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_NAME)));
        createPanel.add(newIndexField, "wrap");
        createPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_SHARDS)));
        createPanel.add(shardSpinner, "wrap");
        createPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_REPLICAS)));
        createPanel.add(replicaSpinner, "wrap");
        createPanel.add(createBtn, "span 2, growx, wrap");

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.add(titleBar, BorderLayout.NORTH);
        topArea.add(searchBox, BorderLayout.CENTER);

        panel.add(topArea, BorderLayout.NORTH);
        panel.add(listScroll, BorderLayout.CENTER);
        panel.add(createPanel, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * 索引列表 Cell Renderer：索引名 + 右侧文档数
     */
    private class IndexCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (value instanceof String name) {
                Long count = indexDocCountMap.get(name);
                if (count != null) {
                    lbl.setText("<html><b>" + name + "</b>"
                            + "&nbsp;<font color='" + (isSelected ? "#cce0ff" : "#888888") + "'>"
                            + count + "</font></html>");
                } else {
                    lbl.setText(name);
                }
            }
            return lbl;
        }
    }

    // ===== 请求历史面板 =====
    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        // 标题栏 + 清空按钮
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(4, 8, 4, 4)));
        JLabel titleLbl = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));
        ClearButton clearHistBtn = new ClearButton();
        clearHistBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY_CLEAR));
        clearHistBtn.addActionListener(e -> {
            requestHistory.clear();
            historyListModel.clear();
        });
        titleBar.add(titleLbl, BorderLayout.CENTER);
        titleBar.add(clearHistBtn, BorderLayout.EAST);

        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryCellRenderer());
        historyList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int idx = historyList.locationToIndex(e.getPoint());
                    if (idx >= 0) applyHistory(historyListModel.get(idx));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(historyList);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JLabel tipLbl = new JLabel("<html><center><small>" +
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY_EMPTY) + "</small></center></html>");
        tipLbl.setHorizontalAlignment(SwingConstants.CENTER);
        tipLbl.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        tipLbl.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        panel.add(titleBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(tipLbl, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * 历史记录 Cell Renderer
     */
    private static class HistoryCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (value instanceof HistoryEntry h) {
                String color = switch (h.method) {
                    case "POST" -> "#e8a000";
                    case "PUT" -> "#5cacee";
                    case HTTP_DELETE -> "#e04040";
                    default -> "#28a745";
                };
                String path = h.path.length() > 32 ? h.path.substring(0, 30) + "…" : h.path;
                lbl.setText("<html><b><font color='" + color + "'>" + h.method + "</font></b> " + path + "</html>");
                lbl.setToolTipText(h.method + " " + h.path);
            }
            return lbl;
        }
    }

    private void applyHistory(HistoryEntry entry) {
        if (entry == null) return;
        methodCombo.setSelectedItem(entry.method);
        pathField.setText(entry.path);
        dslEditor.setText(entry.body);
        dslEditor.setCaretPosition(0);
    }

    private void addToHistory(String method, String path, String body) {
        // 去重：相同 method+path+body 不重复加
        for (HistoryEntry e : requestHistory) {
            if (e.method.equals(method) && e.path.equals(path) && e.body.equals(body)) {
                // 移到顶部
                requestHistory.remove(e);
                requestHistory.addFirst(e);
                rebuildHistoryListModel();
                return;
            }
        }
        requestHistory.addFirst(new HistoryEntry(method, path, body));
        while (requestHistory.size() > MAX_HISTORY) requestHistory.removeLast();
        rebuildHistoryListModel();
    }

    private void rebuildHistoryListModel() {
        historyListModel.clear();
        for (HistoryEntry e : requestHistory) historyListModel.addElement(e);
    }

    private java.awt.event.MouseAdapter buildIndexListMouseListener() {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                    int idx = indexList.locationToIndex(evt.getPoint());
                    if (idx >= 0) {
                        indexList.setSelectedIndex(idx);
                        executeIndexQuickQuery(indexList.getSelectedValue());
                    }
                }
            }

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
        if (idx >= 0 && !indexList.isSelectedIndex(idx)) indexList.setSelectedIndex(idx);
        List<String> selectedIndices = new ArrayList<>(indexList.getSelectedValuesList());
        String primarySelected = indexList.getSelectedValue();

        JPopupMenu menu = new JPopupMenu();

        // 复制索引名
        JMenuItem copyNameItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_COPY_NAME));
        copyNameItem.setEnabled(primarySelected != null);
        copyNameItem.addActionListener(e -> {
            if (primarySelected != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(primarySelected), null);
                NotificationUtil.showSuccess(primarySelected);
            }
        });
        menu.add(copyNameItem);

        menu.addSeparator();

        // 查看别名
        JMenuItem aliasItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_VIEW_ALIASES));
        aliasItem.setEnabled(primarySelected != null);
        aliasItem.addActionListener(e -> {
            if (primarySelected != null) {
                pathField.setText("/" + primarySelected + "/_alias");
                methodCombo.setSelectedItem("GET");
                dslEditor.setText("");
                executeRequest();
            }
        });
        menu.add(aliasItem);

        menu.add(buildViewMappingItem(selectedIndices.size() == 1 ? primarySelected : null));
        menu.add(buildViewSettingsItem(selectedIndices.size() == 1 ? primarySelected : null));

        menu.addSeparator();

        JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE));
        deleteItem.setEnabled(!selectedIndices.isEmpty());
        deleteItem.addActionListener(e -> deleteIndices(selectedIndices));
        menu.add(deleteItem);

        JMenuItem clearItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR));
        clearItem.setEnabled(!selectedIndices.isEmpty());
        clearItem.addActionListener(e -> clearIndices(selectedIndices));
        menu.add(clearItem);

        menu.show(indexList, evt.getX(), evt.getY());
    }

    private void executeIndexQuickQuery(String indexName) {
        if (!connected || indexName == null || indexName.isBlank()) return;
        methodCombo.setSelectedItem("GET");
        pathField.setText("/" + indexName + "/_search");
        String body = dslEditor.getText().trim();
        if (body.isEmpty()) {
            dslEditor.setText("{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 60\n}");
            dslEditor.setCaretPosition(0);
        }
        executeRequest();
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
     * 解析 _cat/indices JSON，填充索引列表 Model + 文档数 Map
     */
    private void parseAndFillIndexList(String json) {
        try {
            indexListModel.clear();
            indexDocCountMap.clear();
            JsonNode arr = JsonUtil.readTree(json);
            if (!arr.isArray()) return;
            for (JsonNode node : arr) {
                JsonNode idx = node.get("index");
                if (idx != null && !idx.isNull()) {
                    String name = idx.toString().replace("\"", "");
                    indexListModel.addElement(name);
                    // 解析文档数
                    JsonNode docsCount = node.get("docs.count");
                    if (docsCount != null && !docsCount.isNull()) {
                        try {
                            indexDocCountMap.put(name, Long.parseLong(docsCount.toString().replace("\"", "")));
                        } catch (NumberFormatException ignored) { /* skip */ }
                    }
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

        // ---- 工具栏（MigLayout）----
        JPanel toolBar = new JPanel(new MigLayout("insets 4, fillx", "[][][][]8[][]push", "[]"));
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
            clearAggTable();
            respStatusLabel.setText("");
            hitsInfoLabel.setText("");
        });

        toolBar.add(templateCombo);
        toolBar.add(loadTplBtn);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL), "growy, gap 2 2");
        toolBar.add(formatBtn);
        toolBar.add(copyBtn);
        toolBar.add(clearBtn);

        // ---- 请求行（MigLayout）----
        JPanel requestRow = new JPanel(new MigLayout("insets 4 2 4 2, fillx", "[90!][grow,fill][]", "[]"));
        methodCombo = new JComboBox<>(new String[]{"GET", "POST", "PUT", HTTP_DELETE, "HEAD"});
        methodCombo.setPreferredSize(new Dimension(90, 32));
        // Method 颜色渲染
        methodCombo.setRenderer(new MethodComboRenderer());

        pathField = new JTextField(CLUSTER_HEALTH_PATH);
        pathField.setPreferredSize(new Dimension(400, 32));
        pathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PATH_PLACEHOLDER));
        // 路径框回车执行
        pathField.addActionListener(e -> executeRequest());

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

        // DSL 编辑器 - 参考 RequestBodyPanel，可编辑，用 SearchableTextArea 包装（启用搜索替换）
        JPanel editorPanel = new JPanel(new BorderLayout());
        // DSL 编辑器顶部标题 + 工具按钮（MigLayout）
        JPanel dslHeaderBar = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push", "[]"));
        dslHeaderBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));
        JLabel dslLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DSL_TITLE));
        dslLabel.setFont(dslLabel.getFont().deriveFont(Font.BOLD, 11f));
        dslHeaderBar.add(dslLabel);

        dslEditor = createJsonEditor(true);
        dslEditor.setText("{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 60\n}");
        // 参考 RequestBodyPanel：可编辑编辑器使用 SearchableTextArea(area) 包装（启用搜索替换）
        searchableDslArea = new SearchableTextArea(dslEditor);
        editorPanel.add(dslHeaderBar, BorderLayout.NORTH);
        editorPanel.add(searchableDslArea, BorderLayout.CENTER);

        // 结果区（Tab: 表格 + 聚合 + 原始 JSON）
        JPanel resultPanel = new JPanel(new BorderLayout());

        // 响应标题栏（MigLayout）：标题 + hits 统计 + 状态
        JPanel respHeader = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push[][]", "[]"));
        respHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));
        JLabel respLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_RESPONSE_TITLE));
        respLabel.setFont(respLabel.getFont().deriveFont(Font.BOLD, 11f));

        hitsInfoLabel = new JLabel("");
        hitsInfoLabel.setFont(hitsInfoLabel.getFont().deriveFont(Font.PLAIN, 11f));
        hitsInfoLabel.setForeground(new Color(80, 130, 200));
        respStatusLabel = new JLabel("");
        respStatusLabel.setFont(respStatusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        respStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        respHeader.add(respLabel);
        respHeader.add(hitsInfoLabel);
        respHeader.add(respStatusLabel);

        resultTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        enhancedTable = new com.laker.postman.common.component.table.EnhancedTablePanel(new String[]{});
        aggTable = new com.laker.postman.common.component.table.EnhancedTablePanel(new String[]{});
        // 参考 ResponseBodyPanel：不可编辑编辑器使用 SearchableTextArea(area, false) 包装（仅搜索）
        resultArea = createJsonEditor(false);
        resultArea.setLineWrap(false);
        resultArea.setHighlightCurrentLine(false);
        searchableResultArea = new SearchableTextArea(resultArea, false);

        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TAB_TABLE), enhancedTable);
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TAB_AGG), aggTable);
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_TAB_RAW), searchableResultArea);

        resultPanel.add(respHeader, BorderLayout.NORTH);
        resultPanel.add(resultTabs, BorderLayout.CENTER);

        editorSplit.setTopComponent(editorPanel);
        editorSplit.setBottomComponent(resultPanel);

        panel.add(editorSplit, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Method 下拉颜色渲染
     */
    private static class MethodComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            if (!isSelected && value instanceof String method) {
                Color c = switch (method) {
                    case "POST" -> new Color(220, 140, 20);
                    case "PUT" -> new Color(80, 160, 230);
                    case HTTP_DELETE -> new Color(210, 50, 50);
                    case "HEAD" -> new Color(130, 100, 200);
                    default -> new Color(40, 167, 69); // GET
                };
                lbl.setForeground(c);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            }
            return lbl;
        }
    }

    private void registerCtrlEnterShortcut(JButton btn) {
        SwingUtilities.invokeLater(() -> {
            if (dslEditor != null) {
                dslEditor.getInputMap().put(
                        KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER,
                                java.awt.event.InputEvent.CTRL_DOWN_MASK),
                        "executeRequest");
                dslEditor.getActionMap().put("executeRequest", new AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        btn.doClick();
                    }
                });
            }
        });
    }

    // ===== 核心功能方法 =====

    private void deleteIndices(List<String> indices) {
        List<String> targets = normalizeIndexTargets(indices);
        if (!connected || targets.isEmpty()) return;

        String confirm = targets.size() == 1
                ? MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_CONFIRM), targets.get(0))
                : MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_BATCH_CONFIRM), targets.size());
        int opt = JOptionPane.showConfirmDialog(this, confirm,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;

        runIndexMutation(targets, true,
                MessageKeys.TOOLBOX_ES_INDEX_DELETE_SUCCESS,
                MessageKeys.TOOLBOX_ES_INDEX_DELETE_BATCH_SUCCESS,
                MessageKeys.TOOLBOX_ES_INDEX_DELETE_FAILED);
    }

    private void clearIndices(List<String> indices) {
        List<String> targets = normalizeIndexTargets(indices);
        if (!connected || targets.isEmpty()) return;

        String confirm = targets.size() == 1
                ? MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_CONFIRM), targets.get(0))
                : MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_BATCH_CONFIRM), targets.size());
        int opt = JOptionPane.showConfirmDialog(this, confirm,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CLEAR_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;

        runIndexMutation(targets, false,
                MessageKeys.TOOLBOX_ES_INDEX_CLEAR_SUCCESS,
                MessageKeys.TOOLBOX_ES_INDEX_CLEAR_BATCH_SUCCESS,
                MessageKeys.TOOLBOX_ES_INDEX_CLEAR_FAILED);
    }

    private List<String> normalizeIndexTargets(List<String> indices) {
        if (indices == null || indices.isEmpty()) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String idx : indices) {
            if (idx != null) {
                String t = idx.trim();
                if (!t.isBlank()) set.add(t);
            }
        }
        return new ArrayList<>(set);
    }

    private void runIndexMutation(List<String> targets, boolean deleteIndex,
                                  String singleSuccessKey, String batchSuccessKey, String failedKey) {
        List<String> finalTargets = new ArrayList<>(targets);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String lastResp = "";
                for (String indexName : finalTargets) {
                    if (deleteIndex) {
                        lastResp = doDelete("/" + indexName, "");
                    } else {
                        lastResp = doPost("/" + indexName + "/_delete_by_query",
                                "{\n  \"query\": {\n    \"match_all\": {}\n  }\n}");
                    }
                }
                return lastResp;
            }

            @Override
            protected void done() {
                try {
                    String resp = get();
                    resultArea.setText(JsonUtil.toJsonPrettyStr(resp));
                    resultArea.setCaretPosition(0);
                    loadIndices();
                    if (finalTargets.size() == 1) {
                        NotificationUtil.showSuccess(
                                MessageFormat.format(I18nUtil.getMessage(singleSuccessKey), finalTargets.get(0)));
                    } else {
                        NotificationUtil.showSuccess(
                                MessageFormat.format(I18nUtil.getMessage(batchSuccessKey), finalTargets.size()));
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    NotificationUtil.showError(MessageFormat.format(
                            I18nUtil.getMessage(failedKey), ex.getMessage()));
                }
            }
        }.execute();
    }

    private void doConnect() {
        String url = getCurrentHost();
        if (url.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_HOST_REQUIRED));
            return;
        }
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        baseUrl = url;
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();
        authHeader = !user.isEmpty() ? "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes()) : null;

        connectBtn.setEnabled(false);
        final String finalUrl = baseUrl;
        new SwingWorker<String, Void>() {
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
                    btnCardLayout.show(btnCard, "disconnect");
                    resultArea.setText(JsonUtil.toJsonPrettyStr(resp));
                    resultArea.setCaretPosition(0);
                    addHostHistory(finalUrl);
                    loadIndices();
                    NotificationUtil.showSuccess(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CONNECT_SUCCESS), finalUrl));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    connected = false;
                    connectionStatusLabel.setForeground(Color.RED);
                    connectionStatusLabel.setToolTipText(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_NOT_CONNECTED));
                    NotificationUtil.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_CONNECT_FAILED), ex.getMessage()));
                }
            }
        }.execute();
    }

    private void doDisconnect() {
        connected = false;
        baseUrl = "http://localhost:9200";
        authHeader = null;
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_NOT_CONNECTED));
        btnCardLayout.show(btnCard, "connect");
        indexListModel.clear();
        indexFilteredModel.clear();
        indexDocCountMap.clear();
        NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DISCONNECT_SUCCESS));
    }

    /**
     * 获取当前选中/输入的 host
     */
    private String getCurrentHost() {
        Object selected = hostCombo.getEditor().getItem();
        return selected == null ? "" : selected.toString().trim();
    }

    /**
     * 将 host 加入历史下拉
     */
    private void addHostHistory(String host) {
        // 先移除已有同名
        for (int i = 0; i < hostCombo.getItemCount(); i++) {
            if (host.equals(hostCombo.getItemAt(i))) {
                hostCombo.removeItemAt(i);
                break;
            }
        }
        hostCombo.insertItemAt(host, 0);
        while (hostCombo.getItemCount() > MAX_HOST_HISTORY) {
            hostCombo.removeItemAt(hostCombo.getItemCount() - 1);
        }
        hostCombo.setSelectedItem(host);
    }

    private void loadIndices() {
        if (!connected) return;
        String previousSelected = indexList == null ? null : indexList.getSelectedValue();
        String currentPath = pathField == null ? "" : pathField.getText().trim();
        boolean shouldAutoApplyTemplate = currentPath.isBlank() || CLUSTER_HEALTH_PATH.equals(currentPath);
        new SwingWorker<String, Void>() {
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
                        int restoreIndex = -1;
                        if (previousSelected != null && !previousSelected.isBlank()) {
                            for (int i = 0; i < indexFilteredModel.size(); i++) {
                                if (previousSelected.equals(indexFilteredModel.get(i))) {
                                    restoreIndex = i;
                                    break;
                                }
                            }
                        }
                        indexList.setSelectedIndex(restoreIndex >= 0 ? restoreIndex : 0);
                        if (shouldAutoApplyTemplate) {
                            applyTemplate(0);
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    log.warn("Failed to load indices: {}", ex.getMessage());
                }
            }
        }.execute();
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
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return doPut("/" + name, body);
            }

            @Override
            protected void done() {
                try {
                    resultArea.setText(JsonUtil.toJsonPrettyStr(get()));
                    resultArea.setCaretPosition(0);
                    newIndexField.setText("");
                    loadIndices();
                    NotificationUtil.showSuccess(
                            MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CREATE_SUCCESS), name));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    NotificationUtil.showError(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_CREATE_FAILED), ex.getMessage()));
                }
            }
        }.execute();
    }

    private void executeRequest() {
        if (!connected) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_NOT_CONNECTED));
            return;
        }
        String selectedMethod = (String) methodCombo.getSelectedItem();
        final String method = (selectedMethod == null) ? "GET" : selectedMethod;
        String path = normalizePath(pathField.getText().trim());
        if (path.isEmpty()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_ERR_PATH_REQUIRED));
            return;
        }
        pathField.setText(path);
        String body = dslEditor.getText().trim();

        // 加入历史
        addToHistory(method, path, body);

        clearTable();
        clearAggTable();
        respStatusLabel.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_REQUESTING));
        respStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        hitsInfoLabel.setText("");
        executeBtn.setEnabled(false);
        long start = System.currentTimeMillis();

        new SwingWorker<ResponseWrapper, Void>() {
            @Override
            protected ResponseWrapper doInBackground() throws Exception {
                return switch (method) {
                    case "POST" -> doPostWithCode(path, body);
                    case "PUT" -> doPutWithCode(path, body);
                    case HTTP_DELETE -> doDeleteWithCode(path, body);
                    case "HEAD" -> doHeadWithCode(path);
                    default -> doGetWithCode(path, body);
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
                    respStatusLabel.setText("");
                } catch (Exception ex) {
                    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    resultArea.setText("Error: " + msg);
                    respStatusLabel.setText(MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_ERROR), elapsed));
                    respStatusLabel.setForeground(UIManager.getColor("Actions.Red"));
                }
            }
        }.execute();
    }

    /**
     * 包含 HTTP 状态码的响应包装
     */
    private record ResponseWrapper(int code, String body) {
    }

    private void handleRequestResult(ResponseWrapper rw, long elapsed) {
        String resp = rw.body();
        String formatted = JsonUtil.isTypeJSON(resp) ? JsonUtil.toJsonPrettyStr(resp) : resp;
        resultArea.setText(formatted);
        resultArea.setCaretPosition(0);

        // 状态码颜色
        int code = rw.code();
        Color statusColor;
        if (code >= 200 && code < 300) statusColor = new Color(0, 160, 0);
        else if (code >= 400 && code < 500) statusColor = new Color(210, 130, 0);
        else if (code >= 500) statusColor = new Color(200, 50, 50);
        else statusColor = UIManager.getColor(LABEL_DISABLED_FG);

        String codeStr = code > 0 ? code + " · " : "";
        String statusKey = code >= 200 && code < 300
                ? MessageKeys.TOOLBOX_ES_STATUS_OK
                : MessageKeys.TOOLBOX_ES_STATUS_ERROR;
        respStatusLabel.setText(codeStr + MessageFormat.format(
                I18nUtil.getMessage(statusKey), elapsed));
        respStatusLabel.setForeground(statusColor);

        // 填表格 + 聚合
        if (JsonUtil.isTypeJSON(resp)) {
            populateTable(resp);
            populateAggTable(resp);
        }

        // totalHits 统计
        Object totalHits = enhancedTable.getClientProperty(CLIENT_PROP_TOTAL_HITS);
        int tableRows = enhancedTable.getTable().getRowCount();
        if (totalHits instanceof Long th) {
            hitsInfoLabel.setText(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_STATUS_HITS), tableRows, th));
        } else {
            hitsInfoLabel.setText("");
        }

        // 自动选中最佳 Tab
        boolean hasAgg = aggTable.getTable().getRowCount() > 0;
        boolean hasHits = enhancedTable.getTable().getRowCount() > 0;
        if (hasAgg) resultTabs.setSelectedIndex(1);
        else if (hasHits) resultTabs.setSelectedIndex(0);
        else resultTabs.setSelectedIndex(2);
    }

    // ===== 表格填充 =====

    private void populateTable(String json) {
        try {
            JsonNode root = JsonUtil.readTree(json);
            if (root.has("hits")) {
                populateHitsTable(root);
            } else if (json.trim().startsWith("[")) {
                populateArrayTable(json);
            } else {
                List<Object[]> rows = new ArrayList<>();
                flattenJson("", root, rows);
                if (!rows.isEmpty()) rebuildEnhancedTable(new String[]{"Key", "Value"}, rows);
                else enhancedTable.clearData();
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
        enhancedTable.putClientProperty(CLIENT_PROP_TOTAL_HITS, totalHits > rows.size() ? totalHits : null);
    }

    /**
     * 解析 aggregations 节点并以 "name | type | buckets/value" 平铺展示
     */
    private void populateAggTable(String json) {
        try {
            JsonNode root = JsonUtil.readTree(json);
            JsonNode aggs = root.get("aggregations");
            if (aggs == null || !aggs.isObject()) {
                aggTable.clearData();
                return;
            }

            List<Object[]> rows = new ArrayList<>();
            flattenAggregations("", aggs, rows);
            if (rows.isEmpty()) {
                aggTable.clearData();
                return;
            }
            rebuildAggTable(new String[]{"Aggregation", "Key", "Value"}, rows);
        } catch (Exception e) {
            log.debug("populateAggTable error: {}", e.getMessage());
            aggTable.clearData();
        }
    }

    private void flattenAggregations(String prefix, JsonNode node, List<Object[]> rows) {
        for (java.util.Map.Entry<String, JsonNode> entry : node.properties()) {
            String aggName = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonNode aggNode = entry.getValue();
            // buckets 类型（terms、range、date_histogram 等）
            JsonNode buckets = aggNode.get(AGG_BUCKETS);
            if (buckets != null && buckets.isArray()) {
                for (JsonNode bucket : buckets) {
                    JsonNode keyNode = bucket.get(AGG_KEY_AS_STRING) != null
                            ? bucket.get(AGG_KEY_AS_STRING) : bucket.get(AGG_KEY);
                    String key = nodeText(keyNode);
                    String docCount = nodeText(bucket.get(AGG_DOC_COUNT));
                    rows.add(new Object[]{aggName, key, docCount});
                    // 嵌套 sub-aggregations
                    for (java.util.Map.Entry<String, JsonNode> sub : bucket.properties()) {
                        String sk = sub.getKey();
                        if (!sk.startsWith("_") && !sk.equals(AGG_DOC_COUNT)
                                && !sk.equals(AGG_KEY) && !sk.equals(AGG_KEY_AS_STRING)) {
                            flattenAggregations(aggName + "[" + key + "]." + sk, sub.getValue(), rows);
                        }
                    }
                }
            } else if (aggNode.has(AGG_VALUE)) {
                // metric 类型（sum、avg、max、min 等）
                rows.add(new Object[]{aggName, AGG_VALUE, nodeText(aggNode.get(AGG_VALUE))});
            } else if (aggNode.has(AGG_DOC_COUNT)) {
                // filter 类型
                rows.add(new Object[]{aggName, AGG_DOC_COUNT, nodeText(aggNode.get(AGG_DOC_COUNT))});
            }
        }
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
            if (src != null) src.properties().forEach(e -> srcCols.add(e.getKey()));
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
        for (JsonNode hit : hitsArr) rows.add(buildHitRow(hit, colNames));
        return rows;
    }

    private static Object[] buildHitRow(JsonNode hit, List<String> colNames) {
        JsonNode src = hit.get("_source");
        Object[] row = new Object[colNames.size()];
        row[0] = nodeText(hit.get("_index"));
        row[1] = nodeText(hit.get("_id"));
        row[2] = nodeText(hit.get("_score"));
        if (src != null) {
            for (int c = 3; c < colNames.size(); c++) row[c] = nodeText(src.get(colNames.get(c)));
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

    private void rebuildEnhancedTable(String[] cols, List<Object[]> rows) {
        enhancedTable.resetAndSetData(cols, rows);
    }

    private void rebuildAggTable(String[] cols, List<Object[]> rows) {
        aggTable.resetAndSetData(cols, rows);
    }

    private void clearTable() {
        enhancedTable.clearData();
    }

    private void clearAggTable() {
        aggTable.clearData();
    }

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
        if (txt != null && !txt.isEmpty())
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(txt), null);
    }

    // ===== HTTP 方法封装（返回 ResponseWrapper 含状态码）=====

    private ResponseWrapper doGetWithCode(String path, String body) throws IOException {
        if (body != null && !body.isEmpty()) {
            RequestBody rb = RequestBody.create(body, MediaType.get(JSON_UTF8));
            return executeHttpWithCode(buildRequest("POST", path, rb));
        }
        return executeHttpWithCode(buildRequest("GET", path, null));
    }

    private ResponseWrapper doPostWithCode(String path, String body) throws IOException {
        RequestBody rb = body.isEmpty()
                ? RequestBody.create(new byte[0], null)
                : RequestBody.create(body, MediaType.get(JSON_UTF8));
        return executeHttpWithCode(buildRequest("POST", path, rb));
    }

    private ResponseWrapper doPutWithCode(String path, String body) throws IOException {
        RequestBody rb = body.isEmpty()
                ? RequestBody.create(new byte[0], MediaType.get(JSON_MIME))
                : RequestBody.create(body, MediaType.get(JSON_UTF8));
        return executeHttpWithCode(buildRequest("PUT", path, rb));
    }

    private ResponseWrapper doDeleteWithCode(String path, String body) throws IOException {
        RequestBody rb = (body != null && !body.isEmpty())
                ? RequestBody.create(body, MediaType.get(JSON_UTF8)) : null;
        Request.Builder builder = new Request.Builder().url(baseUrl + normalizePath(path));
        if (authHeader != null) builder.header(HEADER_AUTHORIZATION, authHeader);
        builder.header("Content-Type", JSON_MIME);
        if (rb != null) builder.delete(rb);
        else builder.delete();
        return executeHttpWithCode(builder.build());
    }

    private ResponseWrapper doHeadWithCode(String path) throws IOException {
        Request.Builder builder = new Request.Builder().url(baseUrl + normalizePath(path)).head();
        if (authHeader != null) builder.header(HEADER_AUTHORIZATION, authHeader);
        return executeHttpWithCode(builder.build());
    }

    // ===== 原始 String HTTP 方法（供内部非 executeRequest 调用）=====
    private String doGet(String path, String body) throws IOException {
        return doGetWithCode(path, body).body();
    }

    private String doPost(String path, String body) throws IOException {
        return doPostWithCode(path, body).body();
    }

    private String doPut(String path, String body) throws IOException {
        return doPutWithCode(path, body).body();
    }

    private String doDelete(String path, String body) throws IOException {
        return doDeleteWithCode(path, body).body();
    }

    private Request buildRequest(String method, String path, RequestBody body) {
        Request.Builder builder = new Request.Builder().url(baseUrl + normalizePath(path));
        if (authHeader != null) builder.header(HEADER_AUTHORIZATION, authHeader);
        builder.header("Content-Type", JSON_MIME);
        builder.method(method, body);
        return builder.build();
    }

    private ResponseWrapper executeHttpWithCode(Request req) throws IOException {
        try (Response resp = getHttpClient().newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful() && body.isEmpty()) {
                throw new IOException("HTTP " + resp.code() + " " + resp.message());
            }
            return new ResponseWrapper(resp.code(), body);
        }
    }

    private OkHttpClient getHttpClient() {
        return OkHttpClientManager.getClientForUrl(
                baseUrl,
                true,
                CONNECT_TIMEOUT_MS,
                READ_TIMEOUT_MS,
                WRITE_TIMEOUT_MS
        );
    }

    private String normalizePath(String path) {
        if (path == null) return "";
        String trimmed = path.trim();
        if (trimmed.isBlank()) return "";
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    // ===== 工具方法 =====

    private RSyntaxTextArea createJsonEditor(boolean editable) {
        RSyntaxTextArea area = new RSyntaxTextArea(10, 60);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setEditable(editable);
        EditorThemeUtil.loadTheme(area);
        updateEditorFont(area);
        return area;
    }

    private void updateEditorFont(RSyntaxTextArea editor) {
        if (editor != null) {
            editor.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        }
    }
}
