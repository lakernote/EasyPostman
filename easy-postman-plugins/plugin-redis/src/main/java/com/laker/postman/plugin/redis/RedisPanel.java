package com.laker.postman.plugin.redis;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.ToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSidebarHeader;
import com.laker.postman.common.component.ToolWindowSidebarToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.common.component.button.PrimaryButton;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.SecondaryButton;
import com.laker.postman.common.component.connection.ConnectionToolbarUi;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.plugin.api.PluginStorage;
import com.laker.postman.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.laker.postman.plugin.redis.RedisI18n.t;

@Slf4j
public class RedisPanel extends JPanel {
    private static final int MAX_KEY_SCAN = 2000;
    private static final int MAX_HISTORY = 30;
    private static final String CONNECT_CARD = "connect";
    private static final String DISCONNECT_CARD = "disconnect";
    private static final int HOST_FIELD_WIDTH = 280;
    private static final int PORT_FIELD_WIDTH = 78;
    private static final int DB_FIELD_WIDTH = 58;
    private static final int AUTH_MODE_WIDTH = 100;
    private static final int AUTH_FIELD_WIDTH = HOST_FIELD_WIDTH;
    private static final int CONNECTION_BUTTON_WIDTH = 78;

    private static final String CMD_GET = "GET";
    private static final String CMD_SET = "SET";
    private static final String CMD_DEL = "DEL";
    private static final String CMD_EXISTS = "EXISTS";
    private static final String CMD_TYPE = "TYPE";
    private static final String CMD_TTL = "TTL";
    private static final String CMD_HGET = "HGET";
    private static final String CMD_HGETALL = "HGETALL";
    private static final String CMD_LRANGE = "LRANGE";
    private static final String CMD_SMEMBERS = "SMEMBERS";
    private static final String CMD_ZRANGE = "ZRANGE";

    private static final String[] COMMANDS = {
            CMD_GET, CMD_SET, CMD_DEL, CMD_EXISTS, CMD_TYPE, CMD_TTL, CMD_HGET, CMD_HGETALL, CMD_LRANGE, CMD_SMEMBERS, CMD_ZRANGE
    };

    private static final String[][] TEMPLATES = {
            {MessageKeys.TOOLBOX_REDIS_TPL_GET, CMD_GET, "user:1", "", ""},
            {MessageKeys.TOOLBOX_REDIS_TPL_SET_JSON, CMD_SET, "user:1", "", "{\n  \"name\": \"alice\",\n  \"age\": 18\n}"},
            {MessageKeys.TOOLBOX_REDIS_TPL_HGETALL, CMD_HGETALL, "user:100", "", ""},
            {MessageKeys.TOOLBOX_REDIS_TPL_LRANGE, CMD_LRANGE, "queue:jobs", "0 20", ""},
            {MessageKeys.TOOLBOX_REDIS_TPL_SMEMBERS, CMD_SMEMBERS, "tag:online", "", ""},
            {MessageKeys.TOOLBOX_REDIS_TPL_ZRANGE, CMD_ZRANGE, "rank:score", "0 20", ""},
            {MessageKeys.TOOLBOX_REDIS_TPL_TTL, CMD_TTL, "session:token", "", ""},
            {MessageKeys.TOOLBOX_REDIS_TPL_DEL, CMD_DEL, "tmp:key", "", ""}
    };

    private static final Pattern ARG_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'|(\\S+)");
    private static final Pattern SIMPLE_HOST_PORT_PATTERN = Pattern.compile("^([^:]+):(\\d{1,5})$");

    private static class HistoryEntry {
        final String command;
        final String key;
        final String args;
        final String value;

        HistoryEntry(String command, String key, String args, String value) {
            this.command = command;
            this.key = key;
            this.args = args;
            this.value = value;
        }
    }

    private JComboBox<String> profileCombo;
    private JButton newProfileBtn;
    private JButton saveProfileBtn;
    private JButton saveAsProfileBtn;
    private JButton deleteProfileBtn;
    private JTextField hostField;
    private EasyJSpinner portSpinner;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private EasyJSpinner dbSpinner;
    private JComboBox<AuthMode> authModeCombo;
    private JPanel connectionPanel;
    private JPanel connectionForm;
    private JPanel authRow;
    private SecondaryButton connectBtn;
    private SecondaryButton disconnectBtn;
    private CardLayout btnCardLayout;
    private JPanel btnCard;

    private SearchTextField keySearchField;
    private DefaultListModel<String> keyListModel;
    private DefaultListModel<String> keyFilteredModel;
    private JList<String> keyList;
    private final Map<String, String> keyTypeMap = new HashMap<>();
    private final Map<String, Long> keyTtlMap = new HashMap<>();

    private JComboBox<String> templateCombo;
    private JComboBox<String> commandCombo;
    private JTextField keyField;
    private JTextField argsField;
    private RSyntaxTextArea valueEditor;
    private RSyntaxTextArea resultArea;
    private SearchableTextArea searchableValueArea;
    private SearchableTextArea searchableResultArea;
    private JLabel keyMetaLabel;
    private JLabel respStatusLabel;
    private PrimaryButton executeBtn;

    private final Deque<HistoryEntry> requestHistory = new ArrayDeque<>();
    private DefaultListModel<HistoryEntry> historyListModel;
    private JList<HistoryEntry> historyList;

    private JSplitPane mainSplit;
    private transient JedisPooled jedis;
    private boolean connected = false;
    private final RedisConnectionProfileStore connectionProfileStore;
    private final Map<String, RedisConnectionProfile> connectionProfilesByName = new LinkedHashMap<>();
    private boolean loadingConnectionProfiles;

    @RequiredArgsConstructor
    private enum AuthMode {
        NONE(MessageKeys.TOOLBOX_REDIS_AUTH_NONE),
        BASIC(MessageKeys.TOOLBOX_REDIS_AUTH_BASIC);

        private final String messageKey;

        String displayName() {
            return t(messageKey);
        }
    }

    public RedisPanel() {
        this(PluginStorage.noop());
    }

    public RedisPanel(PluginStorage storage) {
        this.connectionProfileStore = new RedisConnectionProfileStore(storage);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ToolWindowSurfaceStyle.applyCard(this);

        add(buildConnectionPanel(), BorderLayout.NORTH);

        mainSplit = ToolWindowChrome.createHorizontalInnerSplitPane(
                buildLeftPanel(),
                buildMainPanel(),
                240
        );
        mainSplit.setResizeWeight(0.0);
        mainSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
            int loc = (int) e.getNewValue();
            if (loc > 10) mainSplit.putClientProperty("savedDividerLocation", loc);
        });
        add(mainSplit, BorderLayout.CENTER);
        ToolWindowSurfaceStyle.applyPanelTreeCard(this);
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        connectionPanel = panel;
        ToolWindowSurfaceStyle.applySectionHeader(panel, 3, 6, 3, 6);

        connectionForm = new JPanel(new MigLayout(
                "insets 0, fillx, gapy 2, novisualpadding, hidemode 3",
                "[grow,fill]",
                "[][]"
        ));
        connectionForm.setOpaque(false);

        profileCombo = new JComboBox<>();
        profileCombo.setEditable(false);
        compactRedisControl(profileCombo);
        profileCombo.setRenderer(ConnectionToolbarUi.displayRenderer(value -> value == null ? "" : value));
        profileCombo.addActionListener(e -> applySelectedConnectionProfile());

        newProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_REDIS_PROFILE_NEW),
                "icons/plus.svg", e -> createNewConnectionProfile());
        saveProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_REDIS_PROFILE_SAVE),
                "icons/save.svg", e -> saveCurrentConnectionProfile(true));
        saveProfileBtn.setToolTipText(t(MessageKeys.TOOLBOX_REDIS_PROFILE_SAVE) + " (Ctrl+S)");
        saveAsProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_REDIS_PROFILE_SAVE_AS),
                "icons/duplicate.svg", e -> saveCurrentConnectionProfileAs());
        deleteProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_REDIS_PROFILE_DELETE),
                "icons/delete.svg", e -> deleteSelectedConnectionProfile());

        hostField = new JTextField("localhost");
        compactRedisControl(hostField);
        hostField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_REDIS_HOST_PLACEHOLDER));
        hostField.addActionListener(e -> doConnect());

        portSpinner = EasyJSpinner.intSpinner(6379, 1, 65535, 1);
        compactRedisControl(portSpinner);

        dbSpinner = EasyJSpinner.intSpinner(0, 0, 15, 1);
        compactRedisControl(dbSpinner);

        usernameField = new JTextField("");
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_REDIS_USER_PLACEHOLDER));
        compactRedisControl(usernameField);

        passwordField = new JPasswordField("");
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_REDIS_PASS_PLACEHOLDER));
        compactRedisControl(passwordField);
        passwordField.addActionListener(e -> doConnect());

        authModeCombo = ConnectionToolbarUi.comboBox(AuthMode.values(), AuthMode::displayName);
        compactRedisControl(authModeCombo);
        authModeCombo.addActionListener(e -> setAuthOptionsVisible(getSelectedAuthMode() == AuthMode.BASIC));

        connectBtn = new SecondaryButton(t(MessageKeys.TOOLBOX_REDIS_CONNECT), "icons/connect.svg");
        compactRedisButton(connectBtn);
        connectBtn.addActionListener(e -> doConnect());

        disconnectBtn = new SecondaryButton(t(MessageKeys.TOOLBOX_REDIS_DISCONNECT), "icons/ws-close.svg");
        compactRedisButton(disconnectBtn);
        disconnectBtn.addActionListener(e -> doDisconnect());

        btnCardLayout = new CardLayout();
        btnCard = new JPanel(btnCardLayout);
        btnCard.setOpaque(false);
        btnCard.add(connectBtn, CONNECT_CARD);
        btnCard.add(disconnectBtn, DISCONNECT_CARD);
        btnCardLayout.show(btnCard, CONNECT_CARD);

        JPanel toolbar = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gapx 0",
                ConnectionToolbarUi.profileActionColumns()
                        + ConnectionToolbarUi.connectionFieldColumns(HOST_FIELD_WIDTH) + "4"
                        + "[36!,right]4[" + PORT_FIELD_WIDTH + "!,fill]4"
                        + "[28!,right]4[" + DB_FIELD_WIDTH + "!,fill]4"
                        + ConnectionToolbarUi.connectionFieldColumns(AUTH_MODE_WIDTH)
                        + "6[" + CONNECTION_BUTTON_WIDTH + "!]push",
                "[" + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!]"
        ));
        toolbar.setOpaque(false);
        toolbar.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_REDIS_PROFILE)));
        toolbar.add(profileCombo);
        toolbar.add(newProfileBtn);
        toolbar.add(saveProfileBtn);
        toolbar.add(saveAsProfileBtn);
        toolbar.add(deleteProfileBtn);
        toolbar.add(ConnectionToolbarUi.verticalSeparator(),
                "w 1!, h " + ConnectionToolbarUi.VERTICAL_SEPARATOR_HEIGHT + "!");
        toolbar.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_REDIS_HOST)));
        toolbar.add(hostField);
        toolbar.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_REDIS_PORT)));
        toolbar.add(portSpinner);
        toolbar.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_REDIS_DB)));
        toolbar.add(dbSpinner);
        toolbar.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_REDIS_AUTH)));
        toolbar.add(authModeCombo);
        toolbar.add(btnCard, "h " + ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT + "!");

        authRow = new JPanel(new MigLayout(
                "insets 2 0 2 0, fillx, novisualpadding, gapx 0",
                ConnectionToolbarUi.profileActionColumns()
                        + ConnectionToolbarUi.connectionFieldColumns(AUTH_FIELD_WIDTH) + "4"
                        + "[36!,right]4[" + PORT_FIELD_WIDTH + "!,fill]4"
                        + "[28!,right]4[" + DB_FIELD_WIDTH + "!,fill]4"
                        + ConnectionToolbarUi.connectionFieldColumns(AUTH_MODE_WIDTH)
                        + "6[" + CONNECTION_BUTTON_WIDTH + "!]push",
                "[]"
        ));
        authRow.setOpaque(false);
        authRow.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_REDIS_USER)), "skip 7");
        authRow.add(usernameField);
        authRow.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_REDIS_PASS)));
        authRow.add(passwordField, "span 5, growx");

        connectionForm.add(toolbar, "growx, wrap");
        connectionForm.add(authRow, "growx");
        panel.add(connectionForm, BorderLayout.CENTER);
        setAuthOptionsVisible(false);

        ConnectionToolbarUi.registerSaveShortcut(connectionForm, () -> saveCurrentConnectionProfile(true));
        loadSavedConnectionProfiles(null);
        return panel;
    }

    private void setAuthOptionsVisible(boolean visible) {
        if (authRow == null || connectionForm == null) {
            return;
        }
        if (authModeCombo != null) {
            AuthMode targetMode = visible ? AuthMode.BASIC : AuthMode.NONE;
            if (authModeCombo.getSelectedItem() != targetMode) {
                authModeCombo.setSelectedItem(targetMode);
            }
        }
        authRow.setVisible(visible);
        ConnectionToolbarUi.lockConnectionPanelHeight(connectionPanel, visible);
        connectionForm.revalidate();
        connectionForm.repaint();
        revalidate();
        repaint();
    }

    private <T extends JComponent> T compactRedisControl(T component) {
        Dimension preferredSize = component.getPreferredSize();
        int width = Math.max(preferredSize == null ? 0 : preferredSize.width, 1);
        component.setPreferredSize(new Dimension(width, ConnectionToolbarUi.FORM_CONTROL_HEIGHT));
        component.setMinimumSize(new Dimension(1, ConnectionToolbarUi.FORM_CONTROL_HEIGHT));
        return component;
    }

    private void compactRedisButton(JButton button) {
        button.setPreferredSize(new Dimension(CONNECTION_BUTTON_WIDTH, ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT));
        button.setMinimumSize(new Dimension(CONNECTION_BUTTON_WIDTH, ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(CONNECTION_BUTTON_WIDTH, ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT));
        button.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
    }

    private AuthMode getSelectedAuthMode() {
        Object selected = authModeCombo == null ? null : authModeCombo.getSelectedItem();
        return selected instanceof AuthMode authMode ? authMode : AuthMode.NONE;
    }

    private JComponent buildLeftPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMinimumSize(new Dimension(120, 0));
        wrapper.setPreferredSize(new Dimension(240, 0));

        JTabbedPane leftTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(leftTabs);
        leftTabs.addTab(t(MessageKeys.TOOLBOX_REDIS_KEYS_MANAGEMENT), buildKeyPanel());
        leftTabs.addTab(t(MessageKeys.TOOLBOX_REDIS_HISTORY), buildHistoryPanel());
        wrapper.add(leftTabs, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildKeyPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);

        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.setToolTipText(t(MessageKeys.TOOLBOX_REDIS_KEYS_REFRESH));
        refreshBtn.addActionListener(e -> loadKeysAsync());
        ToolWindowSidebarHeader titleBar = new ToolWindowSidebarHeader(
                t(MessageKeys.TOOLBOX_REDIS_KEYS_MANAGEMENT), refreshBtn);

        keySearchField = new SearchTextField();
        keySearchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_REDIS_KEYS_SEARCH_PLACEHOLDER));
        keySearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        keySearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyKeyFilter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyKeyFilter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyKeyFilter();
            }
        });

        keyListModel = new DefaultListModel<>();
        keyFilteredModel = new DefaultListModel<>();
        keyList = new JList<>(keyFilteredModel);
        keyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        keyList.setCellRenderer(new KeyCellRenderer());
        keyList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            List<String> selected = keyList.getSelectedValuesList();
            if (selected.size() == 1) {
                String key = selected.get(0);
                keyField.setText(key);
                loadKeyMetaIfAbsent(key);
            } else {
                keyMetaLabel.setText("");
            }
        });
        keyList.addMouseListener(buildKeyListMouseListener());

        JScrollPane listScroll = new JScrollPane(keyList);
        ToolWindowSurfaceStyle.applyListScrollPaneCard(listScroll, keyList);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleBar, BorderLayout.NORTH);
        top.add(new ToolWindowSidebarToolbar(null, keySearchField), BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(listScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);

        ClearButton clearHistBtn = new ClearButton();
        clearHistBtn.setToolTipText(t(MessageKeys.TOOLBOX_REDIS_HISTORY_CLEAR));
        clearHistBtn.addActionListener(e -> {
            requestHistory.clear();
            historyListModel.clear();
        });
        ToolWindowSidebarHeader titleBar = new ToolWindowSidebarHeader(
                t(MessageKeys.TOOLBOX_REDIS_HISTORY), clearHistBtn);

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
        ToolWindowSurfaceStyle.applyListScrollPaneCard(scroll, historyList);

        JLabel tipLbl = new JLabel("<html><center><small>" + t(MessageKeys.TOOLBOX_REDIS_HISTORY_EMPTY) + "</small></center></html>");
        tipLbl.setHorizontalAlignment(SwingConstants.CENTER);
        tipLbl.setForeground(ModernColors.getTextSecondary());
        tipLbl.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        panel.add(titleBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(tipLbl, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildMainPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);
        panel.setMinimumSize(new Dimension(0, 0));
        panel.add(buildActionBar(), BorderLayout.NORTH);

        JSplitPane split = ToolWindowChrome.createVerticalInnerSplitPane(
                buildValuePanel(),
                buildResultPanel(),
                210
        );
        split.setResizeWeight(0.36);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionBar() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 6 6 6 6, fillx, gapy 6",
                "[]8[grow,fill]8[]push[]6[]6[]",
                "[][]"
        ));
        ToolWindowSurfaceStyle.applySectionCard(panel);

        templateCombo = new JComboBox<>();
        for (String[] template : TEMPLATES) {
            templateCombo.addItem(template[0]);
        }
        templateCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String key) {
                    lbl.setText(t(key));
                }
                return lbl;
            }
        });

        SecondaryButton loadTplBtn = new SecondaryButton(t(MessageKeys.TOOLBOX_REDIS_LOAD_TEMPLATE), "icons/load.svg");
        loadTplBtn.addActionListener(e -> loadTemplate(templateCombo.getSelectedIndex()));

        commandCombo = new JComboBox<>(COMMANDS);
        commandCombo.setPreferredSize(new Dimension(110, 30));
        commandCombo.addActionListener(e -> updateArgsPlaceholder());

        keyField = new JTextField();
        keyField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_REDIS_KEY_PLACEHOLDER));
        keyField.addActionListener(e -> executeCommand());

        argsField = new JTextField();
        argsField.addActionListener(e -> executeCommand());

        executeBtn = new PrimaryButton(t(MessageKeys.TOOLBOX_REDIS_EXECUTE), "icons/send.svg");
        executeBtn.addActionListener(e -> executeCommand());

        JPanel row1 = new JPanel(new MigLayout(
                "insets 0, fillx",
                "[]8[grow,fill]8[]push",
                "[]"
        ));
        row1.setOpaque(false);
        row1.add(new JLabel(t(MessageKeys.TOOLBOX_REDIS_LOAD_TEMPLATE)));
        row1.add(templateCombo, "growx");
        row1.add(loadTplBtn);

        JPanel row2 = new JPanel(new MigLayout(
                "insets 2, fillx",
                "[]8[120!]8[]8[grow,fill]8[]8[grow,fill]8[]",
                "[]"
        ));
        row2.setOpaque(false);
        row2.add(new JLabel(t(MessageKeys.TOOLBOX_REDIS_COMMAND)));
        row2.add(commandCombo, "w 120!");
        row2.add(new JLabel(t(MessageKeys.TOOLBOX_REDIS_KEY)));
        row2.add(keyField, "growx");
        row2.add(new JLabel(t(MessageKeys.TOOLBOX_REDIS_ARGS)));
        row2.add(argsField, "growx");
        row2.add(executeBtn);

        panel.add(row1, "span, growx, wrap");
        panel.add(row2, "span, growx");

        updateArgsPlaceholder();
        return panel;
    }

    private JPanel buildValuePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applySectionCard(panel);
        JPanel header = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]push[]", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(header);
        JLabel title = new JLabel(t(MessageKeys.TOOLBOX_REDIS_VALUE_TITLE));
        title.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        header.add(title);

        JButton formatBtn = new JButton(IconUtil.createThemed("icons/format.svg", 16, 16));
        formatBtn.setFocusable(false);
        formatBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        formatBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        formatBtn.setToolTipText(t(MessageKeys.TOOLBOX_REDIS_FORMAT_JSON));
        formatBtn.addActionListener(e -> formatValueJson());
        header.add(formatBtn);

        valueEditor = createJsonEditor(true);
        searchableValueArea = new SearchableTextArea(valueEditor);

        panel.add(header, BorderLayout.NORTH);
        panel.add(searchableValueArea, BorderLayout.CENTER);
        registerCtrlEnterShortcut();
        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applySectionCard(panel);
        panel.setMinimumSize(new Dimension(0, 0));
        // title 固定, keyMetaLabel 可伸缩但有最大宽, respStatusLabel 固定右侧
        JPanel header = new JPanel(new MigLayout("insets 2 4 2 4, fillx", "[]8[grow,fill]push[]", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(header);
        JLabel title = new JLabel(t(MessageKeys.TOOLBOX_REDIS_RESPONSE_TITLE));
        title.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        keyMetaLabel = new JLabel("");
        keyMetaLabel.setForeground(ModernColors.getPrimary());
        // 超长 key 名用省略号截断，不撑开面板
        keyMetaLabel.setMinimumSize(new Dimension(0, 0));
        respStatusLabel = new JLabel("");
        respStatusLabel.setForeground(ModernColors.getTextSecondary());
        header.add(title);
        header.add(keyMetaLabel, "growx, wmax 400");
        header.add(respStatusLabel);

        resultArea = createJsonEditor(false);
        resultArea.setLineWrap(false);
        resultArea.setHighlightCurrentLine(false);
        searchableResultArea = new SearchableTextArea(resultArea, false);
        searchableResultArea.setLineNumbersEnabled(false);

        panel.add(header, BorderLayout.NORTH);
        panel.add(searchableResultArea, BorderLayout.CENTER);
        return panel;
    }

    private RSyntaxTextArea createJsonEditor(boolean editable) {
        RSyntaxTextArea area = new RSyntaxTextArea(10, 60);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setEditable(editable);
        EditorThemeUtil.loadTheme(area);
        area.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        return area;
    }

    private void registerCtrlEnterShortcut() {
        valueEditor.getInputMap().put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                "execRedisCmd");
        valueEditor.getActionMap().put("execRedisCmd", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                executeBtn.doClick();
            }
        });
    }

    @Override
    public void removeNotify() {
        closeJedisQuietly();
        super.removeNotify();
    }

    private java.awt.event.MouseAdapter buildKeyListMouseListener() {
        return new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                    int idx = keyList.locationToIndex(evt.getPoint());
                    if (idx >= 0) {
                        keyList.setSelectedIndex(idx);
                        String selectedKey = keyList.getSelectedValue();
                        if (selectedKey != null) {
                            keyField.setText(selectedKey);
                            executeKeyQuickRead(selectedKey);
                        }
                    }
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) showKeyPopup(evt);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (evt.isPopupTrigger()) showKeyPopup(evt);
            }
        };
    }

    private void showKeyPopup(java.awt.event.MouseEvent evt) {
        int idx = keyList.locationToIndex(evt.getPoint());
        if (idx >= 0 && !keyList.isSelectedIndex(idx)) {
            keyList.setSelectedIndex(idx);
        }
        List<String> selectedKeys = new ArrayList<>(keyList.getSelectedValuesList());
        String selected = keyList.getSelectedValue();

        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);
        JMenuItem copyNameItem = new JMenuItem(t(MessageKeys.TOOLBOX_REDIS_KEY_COPY));
        copyNameItem.setEnabled(selected != null);
        copyNameItem.addActionListener(e -> {
            if (selected != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selected), null);
                NotificationUtil.showSuccess(selected);
            }
        });
        menu.add(copyNameItem);

        JMenuItem quickReadItem = new JMenuItem(t(MessageKeys.TOOLBOX_REDIS_KEY_QUICK_READ));
        quickReadItem.setEnabled(selected != null);
        quickReadItem.addActionListener(e -> {
            if (selected != null) {
                keyField.setText(selected);
                executeKeyQuickRead(selected);
            }
        });
        menu.add(quickReadItem);

        JMenuItem deleteItem = new JMenuItem(t(MessageKeys.TOOLBOX_REDIS_KEY_DELETE));
        deleteItem.setEnabled(!selectedKeys.isEmpty());
        deleteItem.addActionListener(e -> deleteKeys(selectedKeys));
        menu.add(deleteItem);

        menu.show(keyList, evt.getX(), evt.getY());
    }

    private void doConnect() {
        String hostInput = getHostText();
        if (hostInput.isBlank()) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_REDIS_ERR_HOST_REQUIRED));
            return;
        }
        ParsedHostPort parsed = parseHostAndPort(hostInput, portSpinner.getCommittedIntValue());
        String host = parsed.host();
        if (host.isBlank()) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_REDIS_ERR_HOST_REQUIRED));
            return;
        }
        int port = parsed.port();
        portSpinner.setValue(port);
        int db = dbSpinner.getCommittedIntValue();
        boolean useAuth = getSelectedAuthMode() == AuthMode.BASIC;
        String user = useAuth ? usernameField.getText().trim() : "";
        String pass = useAuth ? new String(passwordField.getPassword()) : "";

        connectBtn.setEnabled(false);
        respStatusLabel.setText(t(MessageKeys.TOOLBOX_REDIS_STATUS_CONNECTING));

        SwingWorker<JedisPooled, Void> worker = new SwingWorker<>() {
            @Override
            protected JedisPooled doInBackground() {
                DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                        .database(db)
                        .connectionTimeoutMillis(10_000)
                        .socketTimeoutMillis(10_000);
                if (!user.isBlank()) {
                    builder.user(user);
                }
                if (!pass.isBlank()) {
                    builder.password(pass);
                }
                JedisPooled client = new JedisPooled(new HostAndPort(host, port), builder.build());
                try {
                    client.ping();
                    return client;
                } catch (Exception e) {
                    try {
                        client.close();
                    } catch (Exception ignore) {
                    }
                    throw e;
                }
            }

            @Override
            protected void done() {
                connectBtn.setEnabled(true);
                try {
                    JedisPooled newClient = get();
                    closeJedisQuietly();
                    jedis = newClient;
                    connected = true;
                    rememberHostHistory(host);
                    btnCardLayout.show(btnCard, DISCONNECT_CARD);
                    respStatusLabel.setText(t(MessageKeys.TOOLBOX_REDIS_STATUS_CONNECTED_SIMPLE));
                    NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_REDIS_CONNECT_SUCCESS, host, port, db));
                    loadKeysAsync();
                } catch (Exception ex) {
                    connected = false;
                    btnCardLayout.show(btnCard, CONNECT_CARD);
                    respStatusLabel.setText(t(MessageKeys.TOOLBOX_REDIS_STATUS_CONNECT_FAILED));
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_REDIS_ERR_CONNECT_FAILED, extractErr(ex)));
                    log.warn("Redis connect failed", ex);
                }
            }
        };
        worker.execute();
    }

    private void doDisconnect() {
        closeJedisQuietly();
        connected = false;
        btnCardLayout.show(btnCard, CONNECT_CARD);
        keyListModel.clear();
        keyFilteredModel.clear();
        keyTypeMap.clear();
        keyTtlMap.clear();
        keyMetaLabel.setText("");
        respStatusLabel.setText("");
        NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_REDIS_DISCONNECT_SUCCESS));
    }

    private void closeJedisQuietly() {
        if (jedis != null) {
            try {
                jedis.close();
            } catch (Exception ignore) {
            }
            jedis = null;
        }
    }

    private void loadKeysAsync() {
        if (!connected || jedis == null) return;
        respStatusLabel.setText(t(MessageKeys.TOOLBOX_REDIS_STATUS_LOADING_KEYS));

        SwingWorker<KeyLoadResult, Void> worker = new SwingWorker<>() {
            @Override
            protected KeyLoadResult doInBackground() {
                KeyLoadResult result = new KeyLoadResult();
                String cursor = ScanParams.SCAN_POINTER_START;
                ScanParams params = new ScanParams().count(300);
                Set<String> dedup = new LinkedHashSet<>();
                do {
                    var scanResult = jedis.scan(cursor, params);
                    cursor = scanResult.getCursor();
                    for (String key : scanResult.getResult()) {
                        dedup.add(key);
                        if (dedup.size() >= MAX_KEY_SCAN) break;
                    }
                    if (dedup.size() >= MAX_KEY_SCAN) break;
                } while (!ScanParams.SCAN_POINTER_START.equals(cursor));

                result.keys.addAll(dedup);
                Collections.sort(result.keys);

                int metaLimit = Math.min(result.keys.size(), 500);
                for (int i = 0; i < metaLimit; i++) {
                    String key = result.keys.get(i);
                    try {
                        result.types.put(key, jedis.type(key));
                        result.ttls.put(key, jedis.ttl(key));
                    } catch (Exception ignore) {
                        // ignore single key metadata failures
                    }
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    KeyLoadResult result = get();
                    keyListModel.clear();
                    for (String key : result.keys) {
                        keyListModel.addElement(key);
                    }
                    keyTypeMap.clear();
                    keyTypeMap.putAll(result.types);
                    keyTtlMap.clear();
                    keyTtlMap.putAll(result.ttls);
                    applyKeyFilter();
                    respStatusLabel.setText(t(MessageKeys.TOOLBOX_REDIS_STATUS_KEYS_LOADED, result.keys.size()));
                    Object saved = mainSplit.getClientProperty("savedDividerLocation");
                    if (saved instanceof Integer loc) {
                        SwingUtilities.invokeLater(() -> mainSplit.setDividerLocation(loc));
                    }
                } catch (Exception ex) {
                    respStatusLabel.setText(t(MessageKeys.TOOLBOX_REDIS_STATUS_ERROR, "-"));
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_REDIS_ERR_EXECUTE_FAILED, extractErr(ex)));
                    log.warn("Load Redis keys failed", ex);
                }
            }
        };
        worker.execute();
    }

    private void applyKeyFilter() {
        String kw = keySearchField == null ? "" : keySearchField.getText().trim().toLowerCase();
        keyFilteredModel.clear();
        for (int i = 0; i < keyListModel.size(); i++) {
            String key = keyListModel.get(i);
            if (kw.isEmpty() || key.toLowerCase().contains(kw)) {
                keyFilteredModel.addElement(key);
            }
        }
        keySearchField.setNoResult(!kw.isEmpty() && keyFilteredModel.isEmpty());
    }

    private void deleteKeys(List<String> keys) {
        if (!connected || jedis == null || keys == null || keys.isEmpty()) return;

        String msg = keys.size() == 1
                ? MessageFormat.format(t(MessageKeys.TOOLBOX_REDIS_KEY_DELETE_CONFIRM), keys.get(0))
                : MessageFormat.format(t(MessageKeys.TOOLBOX_REDIS_KEY_DELETE_BATCH_CONFIRM), keys.size());
        int opt = JOptionPane.showConfirmDialog(this, msg,
                t(MessageKeys.TOOLBOX_REDIS_KEY_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;

        SwingWorker<Long, Void> worker = new SwingWorker<>() {
            @Override
            protected Long doInBackground() {
                return jedis.del(keys.toArray(String[]::new));
            }

            @Override
            protected void done() {
                try {
                    long deleted = get();
                    NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_REDIS_KEY_DELETE_SUCCESS, deleted));
                    loadKeysAsync();
                } catch (Exception ex) {
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_REDIS_ERR_EXECUTE_FAILED, extractErr(ex)));
                }
            }
        };
        worker.execute();
    }

    private void executeCommand() {
        if (!connected || jedis == null) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_REDIS_ERR_NOT_CONNECTED));
            return;
        }

        String command = Objects.toString(commandCombo.getSelectedItem(), "").trim().toUpperCase(Locale.ROOT);
        String key = keyField.getText().trim();
        String args = argsField.getText().trim();
        String value = valueEditor.getText();

        if (commandNeedsKey(command) && key.isBlank()) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_REDIS_ERR_KEY_REQUIRED));
            return;
        }

        final long start = System.currentTimeMillis();
        respStatusLabel.setText(t(MessageKeys.TOOLBOX_REDIS_STATUS_EXECUTING));
        executeBtn.setEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                Object result = runRedisCommand(command, key, args, value);
                return renderResult(result);
            }

            @Override
            protected void done() {
                executeBtn.setEnabled(true);
                long cost = System.currentTimeMillis() - start;
                try {
                    String text = get();
                    resultArea.setText(text == null ? "" : text);
                    resultArea.setCaretPosition(0);
                    respStatusLabel.setText(t(MessageKeys.TOOLBOX_REDIS_STATUS_OK, cost));
                    addHistory(command, key, args, value);
                    loadKeyMeta(key, true);
                    if (isMutatingCommand(command)) {
                        loadKeysAsync();
                    }
                } catch (Exception ex) {
                    respStatusLabel.setText(t(MessageKeys.TOOLBOX_REDIS_STATUS_ERROR, cost));
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_REDIS_ERR_EXECUTE_FAILED, extractErr(ex)));
                }
            }
        };
        worker.execute();
    }

    private Object runRedisCommand(String command, String key, String args, String value) {
        List<String> argList = parseArgs(args);
        return switch (command) {
            case CMD_GET -> jedis.get(key);
            case CMD_SET -> {
                String actualValue = value == null ? "" : value.strip();
                if (actualValue.isBlank()) {
                    actualValue = String.join(" ", argList).strip();
                }
                if (actualValue.isBlank()) {
                    throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_REDIS_ERR_SET_VALUE_REQUIRED));
                }
                yield jedis.set(key, actualValue);
            }
            case CMD_DEL -> jedis.del(key);
            case CMD_EXISTS -> jedis.exists(key);
            case CMD_TYPE -> jedis.type(key);
            case CMD_TTL -> jedis.ttl(key);
            case CMD_HGET -> {
                if (argList.isEmpty()) {
                    throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_REDIS_ERR_ARG_REQUIRED));
                }
                yield jedis.hget(key, argList.get(0));
            }
            case CMD_HGETALL -> jedis.hgetAll(key);
            case CMD_LRANGE -> {
                long start = argList.isEmpty() ? 0L : parseLongOrThrow(argList.get(0), "start");
                long end = argList.size() < 2 ? 99L : parseLongOrThrow(argList.get(1), "end");
                yield jedis.lrange(key, start, end);
            }
            case CMD_SMEMBERS -> jedis.smembers(key);
            case CMD_ZRANGE -> {
                long start = argList.isEmpty() ? 0L : parseLongOrThrow(argList.get(0), "start");
                long end = argList.size() < 2 ? 99L : parseLongOrThrow(argList.get(1), "end");
                yield jedis.zrange(key, start, end);
            }
            default ->
                    throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_REDIS_ERR_UNSUPPORTED_COMMAND, command));
        };
    }

    private long parseLongOrThrow(String raw, String name) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + ": " + raw, e);
        }
    }

    private boolean commandNeedsKey(String command) {
        return !command.isBlank();
    }

    private boolean isMutatingCommand(String command) {
        return CMD_SET.equals(command) || CMD_DEL.equals(command);
    }

    private List<String> parseArgs(String args) {
        if (args == null || args.isBlank()) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        Matcher matcher = ARG_PATTERN.matcher(args);
        while (matcher.find()) {
            if (matcher.group(1) != null) list.add(matcher.group(1));
            else if (matcher.group(2) != null) list.add(matcher.group(2));
            else if (matcher.group(3) != null) list.add(matcher.group(3));
        }
        return list;
    }

    private String renderResult(Object result) {
        if (result == null) return "(nil)";
        if (result instanceof String s) {
            return JsonUtil.isTypeJSON(s) ? JsonUtil.toJsonPrettyStr(s) : s;
        }
        if (result instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (result instanceof Number || result instanceof Boolean) {
            return String.valueOf(result);
        }
        if (result instanceof Map || result instanceof Collection) {
            return JsonUtil.toJsonPrettyStr(result);
        }
        return JsonUtil.toJsonPrettyStr(result);
    }

    private void loadTemplate(int idx) {
        if (idx < 0 || idx >= TEMPLATES.length) return;
        String[] tpl = TEMPLATES[idx];
        commandCombo.setSelectedItem(tpl[1]);
        keyField.setText(tpl[2]);
        argsField.setText(tpl[3]);
        valueEditor.setText(tpl[4]);
        valueEditor.setCaretPosition(0);
    }

    private void updateArgsPlaceholder() {
        String cmd = Objects.toString(commandCombo.getSelectedItem(), "");
        String placeholder = switch (cmd) {
            case CMD_HGET -> "field";
            case CMD_LRANGE, CMD_ZRANGE -> "start end";
            default -> t(MessageKeys.TOOLBOX_REDIS_ARGS_PLACEHOLDER);
        };
        argsField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
    }

    private void formatValueJson() {
        String text = valueEditor.getText();
        if (text == null || text.isBlank()) return;
        try {
            valueEditor.setText(JsonUtil.toJsonPrettyStr(text));
            valueEditor.setCaretPosition(0);
        } catch (Exception e) {
            NotificationUtil.showError(t(MessageKeys.TOOLBOX_REDIS_ERR_INVALID_JSON));
        }
    }

    private void addHistory(String command, String key, String args, String value) {
        HistoryEntry entry = new HistoryEntry(command, key, args, value == null ? "" : value);
        Iterator<HistoryEntry> it = requestHistory.iterator();
        while (it.hasNext()) {
            HistoryEntry old = it.next();
            if (Objects.equals(old.command, entry.command)
                    && Objects.equals(old.key, entry.key)
                    && Objects.equals(old.args, entry.args)
                    && Objects.equals(old.value, entry.value)) {
                it.remove();
                break;
            }
        }
        requestHistory.addFirst(entry);
        while (requestHistory.size() > MAX_HISTORY) {
            requestHistory.removeLast();
        }
        historyListModel.clear();
        for (HistoryEntry h : requestHistory) {
            historyListModel.addElement(h);
        }
    }

    private void applyHistory(HistoryEntry entry) {
        if (entry == null) return;
        commandCombo.setSelectedItem(entry.command);
        keyField.setText(entry.key);
        argsField.setText(entry.args);
        valueEditor.setText(entry.value);
        valueEditor.setCaretPosition(0);
    }

    private void loadKeyMetaIfAbsent(String key) {
        loadKeyMeta(key, false);
    }

    private void loadKeyMeta(String key, boolean forceRefresh) {
        if (key == null || key.isBlank() || !connected || jedis == null) return;

        if (!forceRefresh && keyTypeMap.containsKey(key) && keyTtlMap.containsKey(key)) {
            keyMetaLabel.setText(formatKeyMeta(keyTypeMap.get(key), keyTtlMap.get(key)));
            return;
        }

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                Map<String, Object> map = new HashMap<>();
                map.put("type", jedis.type(key));
                map.put("ttl", jedis.ttl(key));
                return map;
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> map = get();
                    String type = Objects.toString(map.get("type"), "-");
                    long ttl = ((Number) map.get("ttl")).longValue();
                    if ("none".equals(type)) {
                        // key 已不存在，清除过期缓存
                        keyTypeMap.remove(key);
                        keyTtlMap.remove(key);
                        keyMetaLabel.setText("");
                    } else {
                        keyTypeMap.put(key, type);
                        keyTtlMap.put(key, ttl);
                        keyMetaLabel.setText(formatKeyMeta(type, ttl));
                    }
                    keyList.repaint();
                } catch (Exception ignore) {
                    // ignore metadata failures
                }
            }
        };
        worker.execute();
    }

    private void executeKeyQuickRead(String key) {
        if (key == null || key.isBlank()) return;
        String cachedType = keyTypeMap.get(key);
        if (cachedType != null) {
            applyQuickReadCommand(cachedType);
            return;
        }
        if (!connected || jedis == null) {
            applyQuickReadCommand("string");
            return;
        }
        // 异步获取 type，避免在 EDT 上阻塞
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    return jedis.type(key);
                } catch (Exception ignore) {
                    return "string";
                }
            }

            @Override
            protected void done() {
                try {
                    String resolvedType = get();
                    keyTypeMap.put(key, resolvedType);
                    applyQuickReadCommand(resolvedType);
                } catch (Exception ignore) {
                    applyQuickReadCommand("string");
                }
            }
        }.execute();
    }

    private void applyQuickReadCommand(String type) {
        switch (type) {
            case "hash" -> {
                commandCombo.setSelectedItem(CMD_HGETALL);
                argsField.setText("");
            }
            case "list" -> {
                commandCombo.setSelectedItem(CMD_LRANGE);
                argsField.setText("0 50");
            }
            case "set" -> {
                commandCombo.setSelectedItem(CMD_SMEMBERS);
                argsField.setText("");
            }
            case "zset" -> {
                commandCombo.setSelectedItem(CMD_ZRANGE);
                argsField.setText("0 50");
            }
            default -> {
                commandCombo.setSelectedItem(CMD_GET);
                argsField.setText("");
            }
        }
        executeCommand();
    }

    private String formatKeyMeta(String type, Long ttl) {
        return "[" + type + "]  TTL: " + ttlDisplay(ttl);
    }

    private String ttlDisplay(Long ttl) {
        if (ttl == null) return "-";
        if (ttl == -1L) return t(MessageKeys.TOOLBOX_REDIS_TTL_PERMANENT);
        if (ttl == -2L) return t(MessageKeys.TOOLBOX_REDIS_TTL_EXPIRED);
        long s = ttl;
        long d = s / 86400;
        s %= 86400;
        long h = s / 3600;
        s %= 3600;
        long m = s / 60;
        s %= 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        sb.append(s).append("s");
        return sb.toString().trim();
    }

    private String getHostText() {
        return hostField.getText().trim();
    }

    private void rememberHostHistory(String host) {
        if (host == null || host.isBlank()) return;
        hostField.setText(host.trim());
    }

    private void loadSavedConnectionProfiles(String preferredProfileId) {
        loadingConnectionProfiles = true;
        connectionProfilesByName.clear();
        DefaultComboBoxModel<String> profileModel = new DefaultComboBoxModel<>();
        List<RedisConnectionProfile> profiles = connectionProfileStore.loadProfiles();
        for (RedisConnectionProfile profile : profiles) {
            connectionProfilesByName.put(profile.getName(), profile);
            profileModel.addElement(profile.getName());
        }
        profileCombo.setModel(profileModel);
        profileCombo.setEditable(false);
        loadingConnectionProfiles = false;

        RedisConnectionProfile selectedProfile = selectProfile(profiles, preferredProfileId);
        updateDeleteProfileButton(selectedProfile);
        if (selectedProfile != null) {
            profileCombo.setSelectedItem(selectedProfile.getName());
            applyConnectionProfile(selectedProfile);
        } else {
            hostField.setText("localhost");
        }
    }

    private RedisConnectionProfile selectProfile(List<RedisConnectionProfile> profiles, String preferredProfileId) {
        if (profiles == null || profiles.isEmpty()) {
            return null;
        }
        String preferredId = defaultString(preferredProfileId).trim();
        if (!preferredId.isBlank()) {
            for (RedisConnectionProfile profile : profiles) {
                if (preferredId.equals(profile.getId())) {
                    return profile;
                }
            }
        }
        return connectionProfileStore.loadActiveProfile().orElse(profiles.get(0));
    }

    private void applySelectedConnectionProfile() {
        if (loadingConnectionProfiles) {
            return;
        }
        RedisConnectionProfile profile = connectionProfilesByName.get(getProfileNameText());
        if (profile != null) {
            applyConnectionProfile(profile);
        }
        updateDeleteProfileButton(profile);
    }

    private void applyConnectionProfile(RedisConnectionProfile profile) {
        hostField.setText(defaultString(profile.getHost()).isBlank() ? "localhost" : profile.getHost().trim());
        portSpinner.setValue(profile.getPort());
        dbSpinner.setValue(profile.getDatabase());
        usernameField.setText(defaultString(profile.getUsername()));
        passwordField.setText(defaultString(profile.getPassword()));
        setAuthOptionsVisible(hasAuthOptions(profile));
    }

    private boolean hasAuthOptions(RedisConnectionProfile profile) {
        return !defaultString(profile.getUsername()).isBlank()
                || !defaultString(profile.getPassword()).isBlank();
    }

    private void createNewConnectionProfile() {
        RedisConnectionProfile defaultProfile = RedisConnectionProfileStore.defaultProfile();
        Optional<String> profileName = promptNewProfileName(uniqueProfileName(t(MessageKeys.TOOLBOX_REDIS_PROFILE_NEW_DEFAULT)));
        if (profileName.isEmpty()) {
            return;
        }
        RedisConnectionProfile newProfile = RedisConnectionProfile.builder()
                .id("redis-" + UUID.randomUUID())
                .name(profileName.get())
                .host(defaultProfile.getHost())
                .port(defaultProfile.getPort())
                .database(defaultProfile.getDatabase())
                .username(defaultString(defaultProfile.getUsername()))
                .password(defaultString(defaultProfile.getPassword()))
                .hostHistory(defaultProfile.getHostHistory())
                .build();
        connectionProfileStore.upsertProfile(newProfile);
        loadSavedConnectionProfiles(newProfile.getId());
        NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_REDIS_PROFILE_SAVED, newProfile.getName()));
        hostField.requestFocusInWindow();
    }

    private void saveCurrentConnectionProfile(boolean notify) {
        String hostInput = getHostText();
        ParsedHostPort parsed = parseHostAndPort(hostInput, portSpinner.getCommittedIntValue());
        String host = parsed.host();
        if (host.isBlank()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_REDIS_ERR_HOST_REQUIRED));
            return;
        }
        int port = parsed.port();
        portSpinner.setValue(port);
        saveConnectionProfile(
                host,
                port,
                dbSpinner.getCommittedIntValue(),
                getSelectedAuthMode() == AuthMode.BASIC ? usernameField.getText().trim() : "",
                getSelectedAuthMode() == AuthMode.BASIC ? new String(passwordField.getPassword()) : "",
                notify
        );
    }

    private void saveCurrentConnectionProfileAs() {
        String hostInput = getHostText();
        ParsedHostPort parsed = parseHostAndPort(hostInput, portSpinner.getCommittedIntValue());
        String host = parsed.host();
        if (host.isBlank()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_REDIS_ERR_HOST_REQUIRED));
            return;
        }
        int port = parsed.port();
        int database = dbSpinner.getCommittedIntValue();
        String suggestedName = uniqueProfileName(connectionProfileNameSuggestion(host, port, database));
        Optional<String> profileName = promptNewProfileName(suggestedName);
        if (profileName.isEmpty()) {
            return;
        }
        portSpinner.setValue(port);
        saveConnectionProfile(
                host,
                port,
                database,
                getSelectedAuthMode() == AuthMode.BASIC ? usernameField.getText().trim() : "",
                getSelectedAuthMode() == AuthMode.BASIC ? new String(passwordField.getPassword()) : "",
                profileName.get(),
                true
        );
    }

    private void saveConnectionProfile(String host, int port, int database, String username, String password, boolean notify) {
        saveConnectionProfile(host, port, database, username, password, defaultProfileName(host, port, database), notify);
    }

    private void saveConnectionProfile(String host, int port, int database, String username, String password,
                                       String profileName, boolean notify) {
        RedisConnectionProfile existingProfile = connectionProfilesByName.get(profileName);
        RedisConnectionProfile savedProfile = RedisConnectionProfile.builder()
                .id(existingProfile == null ? "redis-" + UUID.randomUUID() : existingProfile.getId())
                .name(profileName)
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .hostHistory(currentHostHistoryWith(host))
                .build();
        connectionProfileStore.upsertProfile(savedProfile);
        loadSavedConnectionProfiles(savedProfile.getId());
        if (notify) {
            NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_REDIS_PROFILE_SAVED, savedProfile.getName()));
        }
    }

    private void deleteSelectedConnectionProfile() {
        RedisConnectionProfile profile = connectionProfilesByName.get(getProfileNameText());
        if (profile == null) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_REDIS_PROFILE_NOT_SELECTED));
            return;
        }
        if (isDefaultProfile(profile)) {
            updateDeleteProfileButton(profile);
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_REDIS_PROFILE_DEFAULT_NOT_DELETABLE));
            return;
        }
        int option = JOptionPane.showConfirmDialog(
                this,
                t(MessageKeys.TOOLBOX_REDIS_PROFILE_DELETE_CONFIRM, profile.getName()),
                t(MessageKeys.TOOLBOX_REDIS_PROFILE_DELETE_CONFIRM_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (option != JOptionPane.YES_OPTION) {
            return;
        }
        connectionProfileStore.deleteProfile(profile.getId());
        loadSavedConnectionProfiles(null);
        NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_REDIS_PROFILE_DELETED, profile.getName()));
    }

    private String getProfileNameText() {
        Object editor = profileCombo.getEditor().getEditorComponent();
        if (editor instanceof JTextField textField) {
            return textField.getText().trim();
        }
        Object item = profileCombo.getSelectedItem();
        return item == null ? "" : item.toString().trim();
    }

    private Optional<String> promptNewProfileName(String initialName) {
        Object input = JOptionPane.showInputDialog(
                this,
                t(MessageKeys.TOOLBOX_REDIS_PROFILE_SAVE_AS_PROMPT),
                t(MessageKeys.TOOLBOX_REDIS_PROFILE_SAVE_AS_TITLE),
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                initialName
        );
        if (input == null) {
            return Optional.empty();
        }
        String profileName = input.toString().trim();
        if (profileName.isBlank()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_REDIS_PROFILE_NAME_REQUIRED));
            return Optional.empty();
        }
        if (connectionProfilesByName.containsKey(profileName)) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_REDIS_PROFILE_NAME_EXISTS, profileName));
            return Optional.empty();
        }
        return Optional.of(profileName);
    }

    private String defaultProfileName(String host, int port, int database) {
        String selectedName = getProfileNameText();
        return selectedName.isBlank() ? host + ":" + port + "/db" + database : selectedName;
    }

    private String connectionProfileNameSuggestion(String host, int port, int database) {
        return host + ":" + port + "/db" + database;
    }

    private String uniqueProfileName(String baseName) {
        String base = defaultString(baseName).trim();
        if (base.isBlank()) {
            base = t(MessageKeys.TOOLBOX_REDIS_PROFILE_NEW_DEFAULT);
        }
        String candidate = base;
        int suffix = 2;
        while (connectionProfilesByName.containsKey(candidate)) {
            candidate = base + " " + suffix++;
        }
        return candidate;
    }

    private void updateDeleteProfileButton(RedisConnectionProfile profile) {
        deleteProfileBtn.setEnabled(profile != null && !isDefaultProfile(profile));
    }

    private static boolean isDefaultProfile(RedisConnectionProfile profile) {
        return profile != null && RedisConnectionProfileStore.DEFAULT_PROFILE_ID.equals(profile.getId());
    }

    private List<String> currentHostHistoryWith(String activeHost) {
        RedisConnectionProfile profile = connectionProfilesByName.get(getProfileNameText());
        List<String> existingHistory = profile == null ? List.of() : profile.getHostHistory();
        return RedisConnectionProfileStore.normalizeHostHistory(existingHistory, activeHost);
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private ParsedHostPort parseHostAndPort(String rawHost, int fallbackPort) {
        String value = rawHost == null ? "" : rawHost.trim();
        if (value.isBlank()) {
            return new ParsedHostPort("", fallbackPort);
        }

        String noScheme = value.replaceFirst("^redis://", "")
                .replaceFirst("^rediss://", "")
                .replaceFirst("^https?://", "");
        int slashIdx = noScheme.indexOf('/');
        if (slashIdx >= 0) {
            noScheme = noScheme.substring(0, slashIdx);
        }

        Matcher matcher = SIMPLE_HOST_PORT_PATTERN.matcher(noScheme);
        if (matcher.matches()) {
            try {
                int port = Integer.parseInt(matcher.group(2));
                if (port >= 1 && port <= 65535) {
                    return new ParsedHostPort(matcher.group(1), port);
                }
            } catch (NumberFormatException ignore) {
            }
        }

        return new ParsedHostPort(noScheme, fallbackPort);
    }

    private record ParsedHostPort(String host, int port) {
    }

    private String extractErr(Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.toString() : root.getMessage();
    }

    private static class KeyLoadResult {
        final List<String> keys = new ArrayList<>();
        final Map<String, String> types = new HashMap<>();
        final Map<String, Long> ttls = new HashMap<>();
    }

    private class KeyCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String key) {
                Long ttl = keyTtlMap.get(key);
                if (ttl != null) {
                    String metaColor = ModernColors.toHtmlColor(isSelected
                            ? lbl.getForeground()
                            : ModernColors.getTextHint());
                    lbl.setText("<html>" + escapeHtml(key) + "&nbsp;<font color='" + metaColor + "'>TTL: " + ttlDisplay(ttl) + "</font></html>");
                } else {
                    lbl.setText(key);
                }
            }
            return lbl;
        }
    }

    private static class HistoryCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof HistoryEntry h) {
                String key = h.key == null ? "" : h.key;
                if (key.length() > 36) key = key.substring(0, 35) + "…";
                String commandColor = ModernColors.toHtmlColor(ModernColors.getWarning());
                lbl.setText("<html><b><font color='" + commandColor + "'>" + h.command + "</font></b> " + escapeHtml(key) + "</html>");
                lbl.setToolTipText(h.command + " " + h.key + " " + h.args);
            }
            return lbl;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
