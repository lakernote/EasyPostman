package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.RequestMethodUiMetadata;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.ToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSidebarHeader;
import com.laker.postman.common.component.ToolWindowSidebarToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.common.component.button.CopyButton;
import com.laker.postman.common.component.button.FormatButton;
import com.laker.postman.common.component.button.PrimaryButton;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.SecondaryButton;
import com.laker.postman.common.component.connection.ConnectionToolbarUi;
import com.laker.postman.common.component.table.EnhancedTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.http.runtime.okhttp.OkHttpClientManager;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import tools.jackson.databind.JsonNode;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Elasticsearch 可视化 CRUD + DSL 工具面板
 */
@Slf4j
public class ElasticsearchPanel extends JPanel {

    // ===== 连接 =====
    private JComboBox<ElasticsearchConnectionProfile> profileCombo;
    private JButton newProfileBtn;
    private JButton saveProfileBtn;
    private JButton saveAsProfileBtn;
    private JButton deleteProfileBtn;
    private JTextField hostField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<AuthMode> authModeCombo;
    private JPanel connectionPanel;
    private JPanel authRow;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private CardLayout btnCardLayout;   // 用于在 connectBtn/disconnectBtn 之间切换
    private JPanel btnCard;
    private final ElasticsearchConnectionProfileStore connectionProfileStore =
            new ElasticsearchConnectionProfileStore();
    private boolean loadingConnectionProfiles;

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
    private EnhancedTablePanel enhancedTable;
    private EnhancedTablePanel aggTable;
    private JTabbedPane resultTabs;
    private JLabel respStatusLabel;
    private JLabel hitsInfoLabel;
    private PrimaryButton executeBtn;

    // ===== 请求历史 =====
    private static final int MAX_HISTORY = 20;
    /**
     * 每条记录格式: "METHOD PATH\nBODY"
     */
    private final Deque<HistoryEntry> requestHistory = new ArrayDeque<>();
    private DefaultListModel<HistoryEntry> historyListModel;
    private JList<HistoryEntry> historyList;

    private String baseUrl = "http://localhost:9200";
    private String authHeader = null;
    private boolean connected = false;

    // ===== 常量 =====
    private static final String SEARCH_PATH = "/{index}/_search";
    private static final String HTTP_DELETE = "DELETE";
    private static final String CLUSTER_HEALTH_PATH = "/_cluster/health";
    private static final String JSON_UTF8 = "application/json; charset=utf-8";
    private static final String JSON_MIME = "application/json";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String CLIENT_PROP_TOTAL_HITS = "es.totalHits";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int WRITE_TIMEOUT_MS = 30_000;
    // aggregation JSON field names
    private static final String AGG_KEY_AS_STRING = "key_as_string";
    private static final String AGG_KEY = "key";
    private static final String AGG_DOC_COUNT = "doc_count";
    private static final String AGG_VALUE = "value";
    private static final String AGG_BUCKETS = "buckets";
    private static final String CONNECT_CARD = "connect";
    private static final String DISCONNECT_CARD = "disconnect";
    private static final int HOST_FIELD_WIDTH = 280;
    private static final int AUTH_MODE_WIDTH = 100;
    private static final int AUTH_FIELD_WIDTH = HOST_FIELD_WIDTH;
    private static final int CONNECTION_BUTTON_WIDTH = 78;

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
    @RequiredArgsConstructor
    private static class HistoryEntry {
        final String method;
        final String path;
        final String body;

        @Override
        public String toString() {
            return method + "  " + path;
        }
    }

    @RequiredArgsConstructor
    private enum AuthMode {
        NONE(MessageKeys.TOOLBOX_ES_AUTH_NONE),
        BASIC(MessageKeys.TOOLBOX_ES_AUTH_BASIC);

        private final String messageKey;

        String displayName() {
            return I18nUtil.getMessage(messageKey);
        }
    }

    public ElasticsearchPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ToolWindowSurfaceStyle.applyCard(this);

        // 顶部连接栏
        add(buildConnectionPanel(), BorderLayout.NORTH);

        // 主内容区：左侧（索引+历史）+ 右侧 DSL
        JSplitPane mainSplit = ToolWindowChrome.createHorizontalInnerSplitPane(
                buildLeftPanel(),
                buildDslPanel(),
                240
        );
        mainSplit.setDividerLocation(240);
        add(mainSplit, BorderLayout.CENTER);
        ToolWindowSurfaceStyle.applyPanelTreeCard(this);
    }

    // ===== 连接面板 =====
    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        connectionPanel = panel;
        ToolWindowSurfaceStyle.applySectionHeader(panel, 3, 6, 3, 6);

        profileCombo = new JComboBox<>();
        profileCombo.setEditable(false);
        ConnectionToolbarUi.compactControl(profileCombo);
        profileCombo.setPrototypeDisplayValue(ElasticsearchConnectionProfile.builder()
                .name("Default Elasticsearch")
                .build());
        profileCombo.setRenderer(ConnectionToolbarUi.displayRenderer(ElasticsearchConnectionProfile::getName));
        profileCombo.addActionListener(e -> applySelectedConnectionProfile());

        newProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NEW),
                "icons/plus.svg", e -> createNewConnectionProfile());
        saveProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVE),
                "icons/save.svg", e -> saveCurrentConnectionProfile(true));
        saveAsProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVE_AS),
                "icons/duplicate.svg", e -> saveCurrentConnectionProfileAs());
        deleteProfileBtn = ConnectionToolbarUi.iconButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DELETE),
                "icons/delete.svg", e -> deleteSelectedConnectionProfile());

        hostField = new JTextField();
        ConnectionToolbarUi.compactControl(hostField);
        hostField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HOST_PLACEHOLDER));
        hostField.addActionListener(e -> doConnect());

        authModeCombo = ConnectionToolbarUi.comboBox(AuthMode.values(), AuthMode::displayName);
        authModeCombo.addActionListener(e -> setAuthOptionsVisible(getSelectedAuthMode() == AuthMode.BASIC));

        usernameField = new JTextField();
        ConnectionToolbarUi.compactControl(usernameField);
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_USER_PLACEHOLDER));

        passwordField = new JPasswordField();
        ConnectionToolbarUi.compactControl(passwordField);
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PASS_PLACEHOLDER));
        passwordField.addActionListener(e -> doConnect());

        connectBtn = new SecondaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_CONNECT), "icons/connect.svg");
        ConnectionToolbarUi.compactButton(connectBtn, CONNECTION_BUTTON_WIDTH);
        connectBtn.addActionListener(e -> doConnect());

        disconnectBtn = new SecondaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DISCONNECT), "icons/ws-close.svg");
        ConnectionToolbarUi.compactButton(disconnectBtn, CONNECTION_BUTTON_WIDTH);
        disconnectBtn.addActionListener(e -> doDisconnect());

        // 用 CardLayout 将 connectBtn / disconnectBtn 叠放在同一格，切换时不留空白
        btnCardLayout = new CardLayout();
        btnCard = new JPanel(btnCardLayout);
        btnCard.setOpaque(false);
        btnCard.add(connectBtn, CONNECT_CARD);
        btnCard.add(disconnectBtn, DISCONNECT_CARD);
        btnCardLayout.show(btnCard, CONNECT_CARD);

        JPanel form = new JPanel(new MigLayout(
                "insets 0, fillx, gapy 2, novisualpadding, hidemode 3",
                "[grow,fill]",
                "[][]"
        ));

        JPanel mainRow = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gapx 0",
                ConnectionToolbarUi.profileActionColumns()
                        + ConnectionToolbarUi.connectionFieldColumns(HOST_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.connectionFieldColumns(AUTH_MODE_WIDTH)
                        + "6[" + CONNECTION_BUTTON_WIDTH + "!]push",
                "[]"
        ));
        mainRow.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE)));
        mainRow.add(profileCombo);
        mainRow.add(newProfileBtn);
        mainRow.add(saveProfileBtn);
        mainRow.add(saveAsProfileBtn);
        mainRow.add(deleteProfileBtn);
        mainRow.add(ConnectionToolbarUi.verticalSeparator(),
                "w 1!, h " + ConnectionToolbarUi.VERTICAL_SEPARATOR_HEIGHT + "!");
        mainRow.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HOST)));
        mainRow.add(hostField);
        mainRow.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_AUTH)));
        mainRow.add(authModeCombo);
        mainRow.add(btnCard, "h " + ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT + "!");

        authRow = new JPanel(new MigLayout(
                "insets 2 0 2 0, fillx, novisualpadding, gapx 0",
                ConnectionToolbarUi.profileActionColumns()
                        + ConnectionToolbarUi.connectionFieldColumns(AUTH_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.connectionFieldColumns(AUTH_FIELD_WIDTH) + "push",
                "[]"
        ));
        authRow.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_USER)), "skip 7");
        authRow.add(usernameField);
        authRow.add(ConnectionToolbarUi.label(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PASS)));
        authRow.add(passwordField);

        form.add(mainRow, "growx, wrap");
        form.add(authRow, "growx");
        panel.add(form, BorderLayout.CENTER);
        setAuthOptionsVisible(false);
        ConnectionToolbarUi.registerSaveShortcut(form, () -> saveCurrentConnectionProfile(true));
        loadSavedConnectionProfiles(null);

        return panel;
    }

    private void loadSavedConnectionProfiles(String preferredProfileId) {
        loadingConnectionProfiles = true;
        profileCombo.removeAllItems();
        List<ElasticsearchConnectionProfile> profiles = connectionProfileStore.loadProfiles();
        ElasticsearchConnectionProfile activeProfile = connectionProfileStore.loadActiveProfile()
                .orElse(ElasticsearchConnectionProfileStore.defaultProfile());
        String selectedProfileId = preferredProfileId == null || preferredProfileId.isBlank()
                ? activeProfile.getId()
                : preferredProfileId;
        ElasticsearchConnectionProfile selectedProfile = null;
        for (ElasticsearchConnectionProfile profile : profiles) {
            profileCombo.addItem(profile);
            if (profile.getId().equals(selectedProfileId)) {
                selectedProfile = profile;
            }
        }
        if (selectedProfile == null && profileCombo.getItemCount() > 0) {
            selectedProfile = profileCombo.getItemAt(0);
        }
        if (selectedProfile != null) {
            profileCombo.setSelectedItem(selectedProfile);
        }
        loadingConnectionProfiles = false;
        applyConnectionProfile(selectedProfile);
        updateProfileActionState();
    }

    private void applySelectedConnectionProfile() {
        if (loadingConnectionProfiles) {
            return;
        }
        ElasticsearchConnectionProfile profile = getSelectedConnectionProfile();
        applyConnectionProfile(profile);
        if (profile != null) {
            connectionProfileStore.saveProfiles(connectionProfileStore.loadProfiles(), profile.getId());
        }
        updateProfileActionState();
    }

    private void applyConnectionProfile(ElasticsearchConnectionProfile profile) {
        if (profile == null) {
            return;
        }
        baseUrl = ElasticsearchConnectionProfileStore.normalizeBaseUrl(profile.getBaseUrl());
        applyHostHistory(profile);
        authModeCombo.setSelectedItem(profile.isAuthEnabled() ? AuthMode.BASIC : AuthMode.NONE);
        usernameField.setText(profile.getUsername() == null ? "" : profile.getUsername());
        passwordField.setText(profile.getPassword() == null ? "" : profile.getPassword());
        setAuthOptionsVisible(profile.isAuthEnabled());
    }

    private void createNewConnectionProfile() {
        String initialName = uniqueProfileName(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NEW_DEFAULT));
        String name = promptProfileName(initialName, null);
        if (name == null) {
            return;
        }
        ElasticsearchConnectionProfile profile = buildProfile(UUID.randomUUID().toString(), name);
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        NotificationUtil.showSuccess(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVED), profile.getName()));
    }

    private void saveCurrentConnectionProfile(boolean notify) {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        if (selectedProfile == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NOT_SELECTED));
            return;
        }
        ElasticsearchConnectionProfile profile = buildProfile(selectedProfile.getId(), selectedProfile.getName());
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        if (notify) {
            NotificationUtil.showSuccess(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVED), profile.getName()));
        }
    }

    private void saveConnectionProfile(String finalUrl, String user, String pass, boolean notify) {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        if (selectedProfile == null) {
            return;
        }
        ElasticsearchConnectionProfile profile = ElasticsearchConnectionProfile.builder()
                .id(selectedProfile.getId())
                .name(selectedProfile.getName())
                .baseUrl(ElasticsearchConnectionProfileStore.normalizeBaseUrl(finalUrl))
                .authEnabled(getSelectedAuthMode() == AuthMode.BASIC)
                .username(user == null ? "" : user.trim())
                .password(pass == null ? "" : pass)
                .hostHistory(currentHostHistoryWith(finalUrl))
                .build();
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        if (notify) {
            NotificationUtil.showSuccess(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVED), profile.getName()));
        }
    }

    private void saveCurrentConnectionProfileAs() {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        String initialName = selectedProfile == null
                ? connectionProfileNameSuggestion()
                : uniqueProfileName(selectedProfile.getName());
        String name = promptProfileName(initialName, null);
        if (name == null) {
            return;
        }
        ElasticsearchConnectionProfile profile = buildProfile(UUID.randomUUID().toString(), name);
        connectionProfileStore.upsertProfile(profile);
        loadSavedConnectionProfiles(profile.getId());
        NotificationUtil.showSuccess(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVED), profile.getName()));
    }

    private void deleteSelectedConnectionProfile() {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        if (selectedProfile == null) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NOT_SELECTED));
            return;
        }
        if (isDefaultProfile(selectedProfile)) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DEFAULT_NOT_DELETABLE));
            return;
        }
        int result = JOptionPane.showConfirmDialog(this,
                MessageFormat.format(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DELETE_CONFIRM),
                        selectedProfile.getName()),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        String deletedName = selectedProfile.getName();
        connectionProfileStore.deleteProfile(selectedProfile.getId());
        loadSavedConnectionProfiles(ElasticsearchConnectionProfileStore.DEFAULT_PROFILE_ID);
        NotificationUtil.showInfo(MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_DELETED), deletedName));
    }

    private ElasticsearchConnectionProfile buildProfile(String profileId, String profileName) {
        String url = ElasticsearchConnectionProfileStore.normalizeBaseUrl(getCurrentHost());
        return ElasticsearchConnectionProfile.builder()
                .id(profileId)
                .name(profileName)
                .baseUrl(url)
                .authEnabled(getSelectedAuthMode() == AuthMode.BASIC)
                .username(usernameField.getText().trim())
                .password(new String(passwordField.getPassword()))
                .hostHistory(currentHostHistoryWith(url))
                .build();
    }

    private String promptProfileName(String initialValue, String existingProfileId) {
        Object value = JOptionPane.showInputDialog(this,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVE_AS_PROMPT),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_SAVE_AS_TITLE),
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                initialValue);
        if (value == null) {
            return null;
        }
        String name = value.toString().trim();
        if (name.isBlank()) {
            NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NAME_REQUIRED));
            return null;
        }
        if (profileNameExists(name, existingProfileId)) {
            NotificationUtil.showWarning(MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PROFILE_NAME_EXISTS), name));
            return null;
        }
        return name;
    }

    private boolean profileNameExists(String name, String ignoredProfileId) {
        String normalizedName = name == null ? "" : name.trim();
        for (ElasticsearchConnectionProfile profile : connectionProfileStore.loadProfiles()) {
            boolean sameProfile = ignoredProfileId != null && ignoredProfileId.equals(profile.getId());
            if (!sameProfile && profile.getName().equalsIgnoreCase(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    private String uniqueProfileName(String baseName) {
        String normalizedBaseName = baseName == null || baseName.isBlank()
                ? connectionProfileNameSuggestion()
                : baseName.trim();
        if (!profileNameExists(normalizedBaseName, null)) {
            return normalizedBaseName;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = normalizedBaseName + " " + i;
            if (!profileNameExists(candidate, null)) {
                return candidate;
            }
        }
        return normalizedBaseName + " " + System.currentTimeMillis();
    }

    private String connectionProfileNameSuggestion() {
        return ElasticsearchConnectionProfileStore.normalizeBaseUrl(getCurrentHost());
    }

    private ElasticsearchConnectionProfile getSelectedConnectionProfile() {
        Object selected = profileCombo.getSelectedItem();
        return selected instanceof ElasticsearchConnectionProfile profile ? profile : null;
    }

    private boolean isDefaultProfile(ElasticsearchConnectionProfile profile) {
        return profile != null && ElasticsearchConnectionProfileStore.DEFAULT_PROFILE_ID.equals(profile.getId());
    }

    private void updateProfileActionState() {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        boolean hasProfile = selectedProfile != null;
        saveProfileBtn.setEnabled(hasProfile);
        saveAsProfileBtn.setEnabled(hasProfile);
        deleteProfileBtn.setEnabled(hasProfile && !isDefaultProfile(selectedProfile));
    }

    private void setAuthOptionsVisible(boolean visible) {
        if (authRow == null) {
            return;
        }
        authRow.setVisible(visible);
        ConnectionToolbarUi.lockConnectionPanelHeight(connectionPanel, visible);
        authRow.revalidate();
        if (authRow.getParent() != null) {
            authRow.getParent().revalidate();
            authRow.getParent().repaint();
        }
    }

    private AuthMode getSelectedAuthMode() {
        Object selected = authModeCombo == null ? null : authModeCombo.getSelectedItem();
        return selected instanceof AuthMode authMode ? authMode : AuthMode.NONE;
    }

    private void applyHostHistory(ElasticsearchConnectionProfile profile) {
        hostField.setText(ElasticsearchConnectionProfileStore.normalizeBaseUrl(profile.getBaseUrl()));
    }

    private List<String> currentHostHistoryWith(String activeHost) {
        ElasticsearchConnectionProfile selectedProfile = getSelectedConnectionProfile();
        List<String> existingHistory = selectedProfile == null ? List.of() : selectedProfile.getHostHistory();
        return ElasticsearchConnectionProfileStore.normalizeHostHistory(existingHistory, activeHost);
    }

    // ===== 左侧面板：索引管理 + 历史记录（JTabbedPane）=====
    private JPanel buildLeftPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(240, 0));

        JTabbedPane leftTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(leftTabs);
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_MANAGEMENT), buildIndexPanel());
        leftTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY), buildHistoryPanel());

        wrapper.add(leftTabs, BorderLayout.CENTER);
        return wrapper;
    }

    // ===== 索引管理面板 =====
    private JPanel buildIndexPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createEmptyBorder());

        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.addActionListener(e -> loadIndices());
        ToolWindowSidebarHeader titleBar = new ToolWindowSidebarHeader(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_INDEX_MANAGEMENT), refreshBtn);

        // 搜索框
        SearchTextField indexSearchField = new SearchTextField();
        indexSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
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
        ToolWindowSurfaceStyle.applyListScrollPaneCard(listScroll, indexList);

        // 实时过滤
        indexSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterIndices(indexSearchField.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                filterIndices(indexSearchField.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                filterIndices(indexSearchField.getText());
            }
        });

        // 创建索引面板
        JPanel createPanel = new JPanel(new MigLayout(
                "insets 6, fillx",
                "[][grow,fill]",
                "[]2[]2[]2[]"
        ));
        ToolWindowSurfaceStyle.applySectionHeader(createPanel);

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
        topArea.add(new ToolWindowSidebarToolbar(null, indexSearchField), BorderLayout.CENTER);

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
                    String countColor = ModernColors.toHtmlColor(isSelected
                            ? lbl.getForeground()
                            : ModernColors.getTextHint());
                    lbl.setText("<html><b>" + name + "</b>"
                            + "&nbsp;<font color='" + countColor + "'>"
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

        ClearButton clearHistBtn = new ClearButton();
        clearHistBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY_CLEAR));
        clearHistBtn.addActionListener(e -> {
            requestHistory.clear();
            historyListModel.clear();
        });
        ToolWindowSidebarHeader titleBar = new ToolWindowSidebarHeader(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY), clearHistBtn);

        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryCellRenderer());
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int idx = historyList.locationToIndex(e.getPoint());
                    if (idx >= 0) applyHistory(historyListModel.get(idx));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(historyList);
        ToolWindowSurfaceStyle.applyListScrollPaneCard(scroll, historyList);

        JLabel tipLbl = new JLabel("<html><center><small>" +
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_HISTORY_EMPTY) + "</small></center></html>");
        tipLbl.setHorizontalAlignment(SwingConstants.CENTER);
        tipLbl.setForeground(ModernColors.getTextSecondary());
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
                String color = RequestMethodUiMetadata.methodColorHex(h.method);
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

    private MouseAdapter buildIndexListMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                    int idx = indexList.locationToIndex(evt.getPoint());
                    if (idx >= 0) {
                        indexList.setSelectedIndex(idx);
                        executeIndexQuickQuery(indexList.getSelectedValue());
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent evt) {
                if (evt.isPopupTrigger()) maybeShowIndexPopup(evt);
            }

            @Override
            public void mouseReleased(MouseEvent evt) {
                if (evt.isPopupTrigger()) maybeShowIndexPopup(evt);
            }
        };
    }

    private void maybeShowIndexPopup(MouseEvent evt) {
        int idx = indexList.locationToIndex(evt.getPoint());
        if (idx >= 0 && !indexList.isSelectedIndex(idx)) indexList.setSelectedIndex(idx);
        List<String> selectedIndices = new ArrayList<>(indexList.getSelectedValuesList());
        String primarySelected = indexList.getSelectedValue();

        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

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
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        // ---- 工具栏（MigLayout）----
        JPanel toolBar = new JPanel(new MigLayout("insets 4, fillx", "[][][][]8[][]push", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(toolBar);

        templateCombo = new JComboBox<>();
        for (String[] t : DSL_TEMPLATES) templateCombo.addItem(I18nUtil.getMessage(t[0]));
        templateCombo.setPreferredSize(new Dimension(180, ConnectionToolbarUi.FORM_CONTROL_HEIGHT));
        SecondaryButton loadTplBtn = new SecondaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_LOAD_TEMPLATE), "icons/load.svg");
        ConnectionToolbarUi.compactButton(loadTplBtn, 104);
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
        methodCombo.setPreferredSize(new Dimension(90, ConnectionToolbarUi.FORM_CONTROL_HEIGHT));
        // Method 颜色渲染
        methodCombo.setRenderer(new MethodComboRenderer());

        pathField = new JTextField(CLUSTER_HEALTH_PATH);
        pathField.setPreferredSize(new Dimension(400, ConnectionToolbarUi.FORM_CONTROL_HEIGHT));
        pathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_PATH_PLACEHOLDER));
        // 路径框回车执行
        pathField.addActionListener(e -> executeRequest());

        executeBtn = new PrimaryButton(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_EXECUTE), "icons/send.svg");
        ConnectionToolbarUi.compactButton(executeBtn, 72);
        executeBtn.addActionListener(e -> executeRequest());
        registerCtrlEnterShortcut(executeBtn);

        requestRow.add(methodCombo);
        requestRow.add(pathField);
        requestRow.add(executeBtn);

        JPanel topArea = new JPanel(new BorderLayout(0, 4));
        topArea.setOpaque(false);
        topArea.add(toolBar, BorderLayout.NORTH);
        topArea.add(requestRow, BorderLayout.CENTER);
        panel.add(topArea, BorderLayout.NORTH);

        // ---- 主分割：DSL 编辑器(上) + 结果(下) ----
        // DSL 编辑器 - 参考 RequestBodyPanel，可编辑，用 SearchableTextArea 包装（启用搜索替换）
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setOpaque(false);
        // DSL 编辑器顶部标题 + 工具按钮（MigLayout）
        JPanel dslHeaderBar = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(dslHeaderBar);
        JLabel dslLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DSL_TITLE));
        dslLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        dslHeaderBar.add(dslLabel);

        dslEditor = createJsonEditor(true);
        dslEditor.setText("{\n  \"query\": {\n    \"match_all\": {}\n  },\n  \"size\": 60\n}");
        // 参考 RequestBodyPanel：可编辑编辑器使用 SearchableTextArea(area) 包装（启用搜索替换）
        searchableDslArea = new SearchableTextArea(dslEditor);
        editorPanel.add(dslHeaderBar, BorderLayout.NORTH);
        editorPanel.add(searchableDslArea, BorderLayout.CENTER);

        // 结果区（Tab: 表格 + 聚合 + 原始 JSON）
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setOpaque(false);

        // 响应标题栏（MigLayout）：标题 + hits 统计 + 状态
        JPanel respHeader = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push[][]", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(respHeader);
        JLabel respLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_RESPONSE_TITLE));
        respLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));

        hitsInfoLabel = new JLabel("");
        hitsInfoLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        hitsInfoLabel.setForeground(ModernColors.getPrimary());
        respStatusLabel = new JLabel("");
        respStatusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        respStatusLabel.setForeground(ModernColors.getTextSecondary());
        respHeader.add(respLabel);
        respHeader.add(hitsInfoLabel);
        respHeader.add(respStatusLabel);

        resultTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(resultTabs);
        enhancedTable = new EnhancedTablePanel(new String[]{});
        aggTable = new EnhancedTablePanel(new String[]{});
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

        JSplitPane editorSplit = ToolWindowChrome.createVerticalInnerSplitPane(
                editorPanel,
                resultPanel,
                260
        );
        editorSplit.setResizeWeight(0.4);

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
                    case "POST" -> ModernColors.getWarningDark();
                    case "PUT" -> ModernColors.getInfo();
                    case HTTP_DELETE -> ModernColors.getError();
                    case "HEAD" -> ModernColors.getSecondary();
                    default -> ModernColors.getSuccess(); // GET
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
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
                        "executeRequest");
                dslEditor.getActionMap().put("executeRequest", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
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
        url = ElasticsearchConnectionProfileStore.normalizeBaseUrl(url);
        baseUrl = url;
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        boolean useBasicAuth = getSelectedAuthMode() == AuthMode.BASIC;
        authHeader = useBasicAuth && !user.isEmpty()
                ? "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes())
                : null;

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
                    btnCardLayout.show(btnCard, DISCONNECT_CARD);
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
        btnCardLayout.show(btnCard, CONNECT_CARD);
        indexListModel.clear();
        indexFilteredModel.clear();
        indexDocCountMap.clear();
        NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.TOOLBOX_ES_DISCONNECT_SUCCESS));
    }

    /**
     * 获取当前选中/输入的 host
     */
    private String getCurrentHost() {
        return hostField.getText().trim();
    }

    /**
     * 将 host 写回地址输入框，历史由当前 profile 保存。
     */
    private void addHostHistory(String host) {
        host = ElasticsearchConnectionProfileStore.normalizeBaseUrl(host);
        hostField.setText(host);
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
        respStatusLabel.setForeground(ModernColors.getTextSecondary());
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
                    respStatusLabel.setForeground(ModernColors.getError());
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
        if (code >= 200 && code < 300) statusColor = ModernColors.getSuccess();
        else if (code >= 400 && code < 500) statusColor = ModernColors.getWarningDark();
        else if (code >= 500) statusColor = ModernColors.getError();
        else statusColor = ModernColors.getTextSecondary();

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
        for (Map.Entry<String, JsonNode> entry : node.properties()) {
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
                    for (Map.Entry<String, JsonNode> sub : bucket.properties()) {
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
            for (Map.Entry<String, JsonNode> e : first.properties()) colNames.add(e.getKey());
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
        for (Map.Entry<String, JsonNode> entry : obj.properties()) {
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
