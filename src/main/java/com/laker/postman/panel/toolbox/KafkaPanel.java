package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.PlaceholderTextArea;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.button.*;
import com.laker.postman.common.component.table.EnhancedTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Kafka 工具面板：Topic 浏览 + 发送消息 + 实时消费。
 */
@Slf4j
public class KafkaPanel extends JPanel {

    private enum ConsumeStartMode {
        LATEST("latest", false),
        EARLIEST("earliest", false),
        NONE("none", false),
        TIMESTAMP("latest", true),
        OFFSET("latest", true);

        private final String autoOffsetReset;
        private final boolean requiresStartValue;

        ConsumeStartMode(String autoOffsetReset, boolean requiresStartValue) {
            this.autoOffsetReset = autoOffsetReset;
            this.requiresStartValue = requiresStartValue;
        }

        public String autoOffsetReset() {
            return autoOffsetReset;
        }

        public boolean requiresStartValue() {
            return requiresStartValue;
        }
    }

    private record ConsumedMessage(
            String receiveTime,
            String recordTime,
            String topic,
            int partition,
            long offset,
            String key,
            String headers,
            String value) {
    }

    private static final String LABEL_DISABLED_FG = "Label.disabledForeground";
    private static final String SEPARATOR_FG = "Separator.foreground";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String CARD_CONNECT = "connect";
    private static final String CARD_DISCONNECT = "disconnect";
    private static final String MIG_GROWX = "growx";
    private static final String MIG_GROWX_WRAP = "growx, wrap";
    private static final String ACTION_KAFKA_SEND = "kafka-send";
    private static final String TIMEOUT_MS = "10000";

    private JTextField bootstrapField;
    private JTextField clientIdField;
    private JComboBox<String> securityProtocolCombo;
    private JComboBox<String> saslMechanismCombo;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField groupIdField;
    private JComboBox<String> autoOffsetCombo;
    private JTextField consumeStartValueField;
    private PrimaryButton connectBtn;
    private CardLayout btnCardLayout;
    private JPanel btnCard;
    private JLabel connectionStatusLabel;

    private SearchTextField topicSearchField;
    private DefaultListModel<String> topicListModel;
    private DefaultListModel<String> topicFilteredModel;
    private JList<String> topicList;

    private JTextField producerTopicField;
    private JTextField producerKeyField;
    /**
     * Headers 多行文本区：每行一条，格式 key: value
     */
    private PlaceholderTextArea producerHeadersArea;
    private JSpinner producerPartitionSpinner;
    private RSyntaxTextArea producerPayloadArea;
    private PrimaryButton sendBtn;
    private JLabel producerStatusLabel;

    private JTextField consumerTopicField;
    private JSpinner consumerPollTimeoutSpinner;
    private JSpinner consumerBatchSizeSpinner;
    private JSpinner consumerMaxViewSpinner;
    private PrimaryButton startConsumeBtn;
    private StopButton stopConsumeBtn;
    private JLabel consumerStatusLabel;
    private EnhancedTablePanel messageTablePanel;
    private RSyntaxTextArea detailArea;

    private final List<ConsumedMessage> consumedMessages = new ArrayList<>();
    private final AtomicReference<SwingWorker<Void, List<ConsumedMessage>>> consumeWorkerRef = new AtomicReference<>();
    private final AtomicReference<KafkaConsumer<String, String>> runningConsumerRef = new AtomicReference<>();
    /**
     * 缓存可复用的 Producer，避免每次发送都重建连接
     */
    private final AtomicReference<KafkaProducer<String, String>> cachedProducerRef = new AtomicReference<>();
    /**
     * 缓存 Producer 对应的 bootstrap，bootstrap 变更时重建
     */
    private volatile String cachedProducerBootstrap;

    public KafkaPanel() {
        initUI();
    }

    private static String t(String key, Object... args) {
        return I18nUtil.getMessage(key, args);
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        add(buildConnectionPanel(), BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTopicPanel(), buildWorkPanel());
        mainSplit.setDividerLocation(240);
        mainSplit.setDividerSize(3);
        mainSplit.setResizeWeight(0.23);
        mainSplit.setContinuousLayout(true);
        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        // ── 所有行合并到一个 MigLayout，避免子面板裁剪 FlatLaf focus 高亮边框 ──
        // 列布局：4组 标签+输入，最后一行为 startValue + 按钮区
        String cols4 = "[]8[grow,fill]8[]8[grow,fill]8[]8[grow,fill]8[]8[grow,fill]";
        JPanel form = new JPanel(new MigLayout(
                "insets 4 0 4 0, fillx, gapy 6",
                cols4,
                "[]"
        ));

        // ── 第一行：bootstrap / clientId / protocol / sasl ──
        bootstrapField = new JTextField("localhost:9092");
        bootstrapField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_HOST_PLACEHOLDER));
        bootstrapField.setPreferredSize(new Dimension(0, 32));
        bootstrapField.addActionListener(e -> loadTopics());

        clientIdField = new JTextField("easy-postman-toolbox");
        clientIdField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_CLIENT_ID_PLACEHOLDER));
        clientIdField.setPreferredSize(new Dimension(0, 32));

        securityProtocolCombo = new JComboBox<>(new String[]{"PLAINTEXT", "SASL_PLAINTEXT", "SASL_SSL", "SSL"});
        securityProtocolCombo.setPreferredSize(new Dimension(0, 32));

        saslMechanismCombo = new JComboBox<>(new String[]{"PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512"});
        saslMechanismCombo.setPreferredSize(new Dimension(0, 32));

        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_HOST)));
        form.add(bootstrapField, MIG_GROWX);
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_CLIENT_ID)));
        form.add(clientIdField, MIG_GROWX);
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_SECURITY_PROTOCOL)));
        form.add(securityProtocolCombo, MIG_GROWX);
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_SASL_MECHANISM)));
        form.add(saslMechanismCombo, MIG_GROWX_WRAP);

        // ── 第二行：user / pass / groupId / offsetReset ──
        usernameField = new JTextField("");
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_USER_PLACEHOLDER));
        usernameField.setPreferredSize(new Dimension(0, 32));

        passwordField = new JPasswordField("");
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_PASS_PLACEHOLDER));
        passwordField.setPreferredSize(new Dimension(0, 32));
        passwordField.addActionListener(e -> loadTopics());

        groupIdField = new JTextField("easy-postman-consumer");
        groupIdField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_GROUP_ID_PLACEHOLDER));
        groupIdField.setPreferredSize(new Dimension(0, 32));

        autoOffsetCombo = new JComboBox<>(new String[]{
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_LATEST),
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_EARLIEST),
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_NONE),
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_TIMESTAMP),
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_OFFSET)
        });
        autoOffsetCombo.setSelectedIndex(0);
        autoOffsetCombo.setPreferredSize(new Dimension(0, 32));

        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_USER)));
        form.add(usernameField, MIG_GROWX);
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PASS)));
        form.add(passwordField, MIG_GROWX);
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_GROUP_ID)));
        form.add(groupIdField, MIG_GROWX);
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET)));
        form.add(autoOffsetCombo, MIG_GROWX_WRAP);

        // ── 第三行：startValue / 连接按钮 / 状态（span 全部列）──
        consumeStartValueField = new JTextField("");
        consumeStartValueField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE_PLACEHOLDER));
        consumeStartValueField.setPreferredSize(new Dimension(0, 32));

        connectBtn = new PrimaryButton(t(MessageKeys.TOOLBOX_KAFKA_CONNECT), "icons/connect.svg");
        connectBtn.addActionListener(e -> loadTopics());

        SecondaryButton disconnectBtn = new SecondaryButton(t(MessageKeys.TOOLBOX_KAFKA_DISCONNECT), "icons/ws-close.svg");
        disconnectBtn.addActionListener(e -> doDisconnect());

        btnCardLayout = new CardLayout();
        btnCard = new JPanel(btnCardLayout);
        btnCard.setOpaque(false);
        btnCard.add(connectBtn, CARD_CONNECT);
        btnCard.add(disconnectBtn, CARD_DISCONNECT);
        btnCardLayout.show(btnCard, CARD_CONNECT);

        connectionStatusLabel = new JLabel("●");
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setFont(connectionStatusLabel.getFont().deriveFont(Font.BOLD, 14f));
        connectionStatusLabel.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_NOT_CONNECTED));

        // 第三行跨越所有列，用 span + split 实现
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE)), "span, split 4");
        form.add(consumeStartValueField, MIG_GROWX);
        form.add(btnCard);
        form.add(connectionStatusLabel);

        securityProtocolCombo.addActionListener(e -> updateSecurityFieldsEnabledState());
        autoOffsetCombo.addActionListener(e -> updateConsumeStartValueEnabledState());
        updateSecurityFieldsEnabledState();
        updateConsumeStartValueEnabledState();


        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildTopicPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setPreferredSize(new Dimension(230, 0));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(6, 10, 6, 6)));
        JLabel titleLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_TOPIC_MANAGEMENT));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));
        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_TOPIC_REFRESH));
        refreshBtn.addActionListener(e -> loadTopics());
        titleBar.add(titleLbl, BorderLayout.CENTER);
        titleBar.add(refreshBtn, BorderLayout.EAST);

        topicSearchField = new SearchTextField();
        topicSearchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, null);
        topicSearchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_TOPIC_SEARCH_PLACEHOLDER));
        topicSearchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        topicSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyTopicFilter();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyTopicFilter();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyTopicFilter();
            }
        });

        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setBorder(new EmptyBorder(6, 8, 4, 8));
        searchBox.add(topicSearchField, BorderLayout.CENTER);

        topicListModel = new DefaultListModel<>();
        topicFilteredModel = new DefaultListModel<>();
        topicList = new JList<>(topicFilteredModel);
        topicList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topicList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            String selected = topicList.getSelectedValue();
            if (selected != null && !selected.isBlank()) {
                producerTopicField.setText(selected);
                consumerTopicField.setText(selected);
            }
        });
        topicList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int index = topicList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        topicList.setSelectedIndex(index);
                        String selected = topicList.getSelectedValue();
                        if (selected != null) {
                            producerTopicField.setText(selected);
                            consumerTopicField.setText(selected);
                        }
                    }
                }
            }
        });
        JScrollPane topicScroll = new JScrollPane(topicList);
        topicScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel top = new JPanel(new BorderLayout());
        top.add(titleBar, BorderLayout.NORTH);
        top.add(searchBox, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(topicScroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildWorkPanel() {
        JTabbedPane workTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        workTabs.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        workTabs.addTab(
                t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_TITLE),
                IconUtil.createThemed("icons/send.svg", 16, 16),
                buildProducerPanel(),
                t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_TITLE));
        workTabs.addTab(
                t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_TITLE),
                IconUtil.createThemed("icons/start.svg", 16, 16),
                buildConsumerPanel(),
                t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_TITLE));
        return workTabs;
    }

    private JPanel buildProducerPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        // ---- 标题栏 ----
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        JLabel titleLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_TITLE));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));
        titleBar.add(titleLbl, BorderLayout.WEST);

        // ---- 参数表单（合并为单一 MigLayout，避免子面板裁剪 FlatLaf focus 高亮）----
        JPanel form = new JPanel(new MigLayout(
                "insets 8 10 6 10, fillx, gapy 6",
                "[]8[grow,fill]8[]8[grow,fill]",
                "[]"
        ));

        producerTopicField = new JTextField("");
        producerTopicField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_TOPIC_PLACEHOLDER));
        producerTopicField.setPreferredSize(new Dimension(180, 30));

        producerKeyField = new JTextField("");
        producerKeyField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_KEY_PLACEHOLDER));
        producerKeyField.setPreferredSize(new Dimension(140, 30));

        producerPartitionSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, Integer.MAX_VALUE, 1));
        producerPartitionSpinner.setPreferredSize(new Dimension(90, 30));

        sendBtn = new PrimaryButton(t(MessageKeys.TOOLBOX_KAFKA_SEND), "icons/send.svg");
        sendBtn.addActionListener(e -> sendMessage());

        // 第一行：topic + key
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_TOPIC)));
        form.add(producerTopicField, MIG_GROWX);
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_KEY)));
        form.add(producerKeyField, MIG_GROWX_WRAP);

        // 第二行：partition + send（span 剩余列）
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PARTITION)), "span, split 3");
        form.add(producerPartitionSpinner, "w 90!");
        form.add(sendBtn, "push, al right");

        // ---- Headers 区域（带标题栏，风格与 ES/Influx 一致）----
        JPanel headersPanel = new JPanel(new BorderLayout());
        JPanel headersHeader = new JPanel(new MigLayout("insets 4 8 4 8, fillx", "[]push", "[]"));
        headersHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));
        JLabel headersLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_HEADERS));
        headersLbl.setFont(headersLbl.getFont().deriveFont(Font.BOLD, 11f));
        headersHeader.add(headersLbl);

        producerHeadersArea = new PlaceholderTextArea(2, 0);
        producerHeadersArea.setLineWrap(false);
        producerHeadersArea.setBackground(ModernColors.getInputBackgroundColor());
        producerHeadersArea.setPlaceholder(t(MessageKeys.TOOLBOX_KAFKA_HEADERS_PLACEHOLDER));
        JScrollPane headersScroll = new JScrollPane(producerHeadersArea);
        headersScroll.setBorder(BorderFactory.createEmptyBorder());

        headersPanel.add(headersHeader, BorderLayout.NORTH);
        headersPanel.add(headersScroll, BorderLayout.CENTER);

        // ---- Payload 区域（带标题栏）----
        JPanel payloadPanel = new JPanel(new BorderLayout());
        JPanel payloadHeader = new JPanel(new MigLayout("insets 4 8 4 8, fillx", "[]push[]", "[]"));
        payloadHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));
        JLabel payloadLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PAYLOAD));
        payloadLbl.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        ClearButton clearButton = new ClearButton();
        clearButton.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_CLEAR_PAYLOAD));
        clearButton.addActionListener(e -> producerPayloadArea.setText(""));
        payloadHeader.add(payloadLbl);
        payloadHeader.add(clearButton);

        producerPayloadArea = new RSyntaxTextArea();
        producerPayloadArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        producerPayloadArea.setCodeFoldingEnabled(true);
        producerPayloadArea.setAntiAliasingEnabled(true);
        producerPayloadArea.setText("{\n  \"message\": \"hello kafka\"\n}");
        producerPayloadArea.getInputMap().put(KeyStroke.getKeyStroke("control ENTER"), ACTION_KAFKA_SEND);
        producerPayloadArea.getInputMap().put(KeyStroke.getKeyStroke("meta ENTER"), ACTION_KAFKA_SEND);
        producerPayloadArea.getActionMap().put(ACTION_KAFKA_SEND, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendMessage();
            }
        });
        EditorThemeUtil.loadTheme(producerPayloadArea);
        updateEditorFont();
        SearchableTextArea payloadSearchableArea = new SearchableTextArea(producerPayloadArea);

        payloadPanel.add(payloadHeader, BorderLayout.NORTH);
        payloadPanel.add(payloadSearchableArea, BorderLayout.CENTER);

        JSplitPane editorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headersPanel, payloadPanel);
        editorSplit.setDividerLocation(100);
        editorSplit.setDividerSize(5);
        editorSplit.setResizeWeight(0.25);
        editorSplit.setContinuousLayout(true);
        editorSplit.setBorder(BorderFactory.createEmptyBorder());

        producerStatusLabel = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_READY));
        producerStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        producerStatusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));

        panel.add(titleBar, BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.add(form, BorderLayout.NORTH);
        content.add(editorSplit, BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        panel.add(producerStatusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildConsumerPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        // ---- 标题栏 ----
        JPanel titleBar = new JPanel(new BorderLayout(0, 0));
        titleBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        JLabel titleLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_TITLE));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));
        titleBar.add(titleLbl, BorderLayout.WEST);

        // ---- 控制栏（合并为单一 MigLayout，避免子面板裁剪 FlatLaf focus 高亮）----
        JPanel controls = new JPanel(new MigLayout(
                "insets 8 10 6 10, fillx, gapy 6",
                "[]8[grow,fill]8[]8[110!]8[]8[110!]",
                "[]"
        ));

        consumerTopicField = new JTextField("");
        consumerTopicField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_TOPIC_PLACEHOLDER));
        consumerTopicField.setPreferredSize(new Dimension(0, 32));

        consumerPollTimeoutSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 30000, 100));
        consumerPollTimeoutSpinner.setPreferredSize(new Dimension(110, 32));

        consumerBatchSizeSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 5000, 1));
        consumerBatchSizeSpinner.setPreferredSize(new Dimension(110, 32));

        consumerMaxViewSpinner = new JSpinner(new SpinnerNumberModel(500, 50, 10000, 50));
        consumerMaxViewSpinner.setPreferredSize(new Dimension(110, 32));

        startConsumeBtn = new PrimaryButton(t(MessageKeys.TOOLBOX_KAFKA_START_CONSUME), "icons/start.svg");
        startConsumeBtn.addActionListener(e -> startConsuming());

        stopConsumeBtn = new StopButton();
        stopConsumeBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_STOP_CONSUME));
        stopConsumeBtn.setEnabled(false);
        stopConsumeBtn.addActionListener(e -> stopConsuming());

        ClearButton clearConsumeBtn = new ClearButton();
        clearConsumeBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_CLEAR));
        clearConsumeBtn.addActionListener(e -> clearConsumedMessages());

        // 第一行：topic（span 所有列）
        controls.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_TOPIC)));
        controls.add(consumerTopicField, "span, growx, wrap");

        // 第二行：poll timeout / batch size / max view
        controls.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_POLL_TIMEOUT)));
        controls.add(consumerPollTimeoutSpinner);
        controls.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_BATCH_SIZE)));
        controls.add(consumerBatchSizeSpinner);
        controls.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_MAX_VIEW)));
        controls.add(consumerMaxViewSpinner, "wrap");

        // 第三行：操作按钮（span + 右对齐）
        controls.add(clearConsumeBtn, "span, split 3, al right");
        controls.add(stopConsumeBtn);
        controls.add(startConsumeBtn);

        // ---- 消息表格 ----
        String[] columns = {
                t(MessageKeys.TOOLBOX_KAFKA_COL_TIME),
                t(MessageKeys.TOOLBOX_KAFKA_COL_RECORD_TIME),
                t(MessageKeys.TOOLBOX_KAFKA_COL_TOPIC),
                t(MessageKeys.TOOLBOX_KAFKA_COL_PARTITION),
                t(MessageKeys.TOOLBOX_KAFKA_COL_OFFSET),
                t(MessageKeys.TOOLBOX_KAFKA_COL_KEY),
                t(MessageKeys.TOOLBOX_KAFKA_COL_HEADERS),
                t(MessageKeys.TOOLBOX_KAFKA_COL_VALUE)
        };
        messageTablePanel = new EnhancedTablePanel(columns);
        messageTablePanel.getTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateDetailBySelection();
        });

        detailArea = new RSyntaxTextArea();
        detailArea.setEditable(false);
        detailArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        detailArea.setCodeFoldingEnabled(true);
        detailArea.setAntiAliasingEnabled(true);
        detailArea.setLineWrap(false);
        detailArea.setHighlightCurrentLine(false);
        EditorThemeUtil.loadTheme(detailArea);
        updateEditorFont();
        detailArea.setText("");
        SearchableTextArea searchableDetailArea = new SearchableTextArea(detailArea, false);
        searchableDetailArea.setBorder(BorderFactory.createTitledBorder(t(MessageKeys.TOOLBOX_KAFKA_MESSAGE_DETAIL)));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, messageTablePanel, searchableDetailArea);
        centerSplit.setDividerLocation(260);
        centerSplit.setDividerSize(5);
        centerSplit.setResizeWeight(0.72);
        centerSplit.setContinuousLayout(true);

        consumerStatusLabel = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_READY));
        consumerStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        consumerStatusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));

        JPanel north = new JPanel(new BorderLayout());
        north.add(titleBar, BorderLayout.NORTH);
        north.add(controls, BorderLayout.CENTER);

        panel.add(north, BorderLayout.NORTH);
        panel.add(centerSplit, BorderLayout.CENTER);
        panel.add(consumerStatusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void updateSecurityFieldsEnabledState() {
        boolean saslEnabled = selectedSecurityProtocol().startsWith("SASL");
        saslMechanismCombo.setEnabled(saslEnabled);
        usernameField.setEnabled(saslEnabled);
        passwordField.setEnabled(saslEnabled);
    }

    private void updateConsumeStartValueEnabledState() {
        ConsumeStartMode consumeStartMode = selectedConsumeStartMode();
        boolean enabled = consumeStartMode.requiresStartValue();
        consumeStartValueField.setEnabled(enabled);
        if (consumeStartMode == ConsumeStartMode.TIMESTAMP) {
            consumeStartValueField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE_PLACEHOLDER) + " (e.g. 2024-01-01 00:00:00 or ms)");
        } else if (consumeStartMode == ConsumeStartMode.OFFSET) {
            consumeStartValueField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE_PLACEHOLDER) + " (e.g. 100)");
        } else {
            consumeStartValueField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE_PLACEHOLDER));
        }
        consumeStartValueField.repaint();
    }

    private void loadTopics() {
        String bootstrap = bootstrapField.getText().trim();
        if (bootstrap.isBlank()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_KAFKA_ERR_HOST_REQUIRED));
            return;
        }

        // 在 EDT 预先读取所有 UI 字段（含 passwordField），避免非 EDT 线程读取 Swing 组件
        final Properties baseProps;
        try {
            baseProps = buildCommonClientProperties();
        } catch (IllegalArgumentException ex) {
            NotificationUtil.showWarning(rootMessage(ex));
            return;
        }

        connectBtn.setEnabled(false);
        setConnectionStatus(ModernColors.INFO, t(MessageKeys.TOOLBOX_KAFKA_STATUS_LOADING_TOPICS));

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                Properties props = new Properties();
                props.putAll(baseProps);
                props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
                props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
                try (AdminClient adminClient = AdminClient.create(props)) {
                    ListTopicsOptions options = new ListTopicsOptions().listInternal(false).timeoutMs(10000);
                    List<String> topics = new ArrayList<>(adminClient.listTopics(options).names().get(10, TimeUnit.SECONDS));
                    Collections.sort(topics);
                    return topics;
                }
            }

            @Override
            protected void done() {
                connectBtn.setEnabled(true);
                try {
                    List<String> topics = get();
                    topicListModel.clear();
                    for (String topic : topics) {
                        topicListModel.addElement(topic);
                    }
                    applyTopicFilter();
                    if (!topics.isEmpty() && producerTopicField.getText().isBlank() && consumerTopicField.getText().isBlank()) {
                        producerTopicField.setText(topics.get(0));
                        consumerTopicField.setText(topics.get(0));
                    }
                    String status = t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONNECTED, bootstrap);
                    setConnectionStatus(ModernColors.SUCCESS, status);
                    btnCardLayout.show(btnCard, CARD_DISCONNECT);
                    producerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_TOPICS_LOADED, topics.size()));
                    NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_KAFKA_STATUS_TOPICS_LOADED, topics.size()));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("Load kafka topics interrupted", ex);
                } catch (Exception ex) {
                    Throwable cause = unwrapException(ex);
                    setConnectionStatus(ModernColors.ERROR, t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONNECT_FAILED, rootMessage(cause)));
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONNECT_FAILED, rootMessage(cause)));
                    log.warn("Load kafka topics failed", cause);
                }
            }
        };
        worker.execute();
    }

    private void applyTopicFilter() {
        String keyword = topicSearchField.getText().trim().toLowerCase(Locale.ROOT);
        String previousSelection = topicList.getSelectedValue();
        topicFilteredModel.clear();
        for (int i = 0; i < topicListModel.getSize(); i++) {
            String topic = topicListModel.getElementAt(i);
            if (keyword.isBlank() || topic.toLowerCase(Locale.ROOT).contains(keyword)) {
                topicFilteredModel.addElement(topic);
            }
        }
        if (previousSelection != null && !previousSelection.isBlank()) {
            for (int i = 0; i < topicFilteredModel.getSize(); i++) {
                if (previousSelection.equals(topicFilteredModel.getElementAt(i))) {
                    topicList.setSelectedIndex(i);
                    topicList.ensureIndexIsVisible(i);
                    break;
                }
            }
        }
        if (topicList.getSelectedIndex() < 0 && keyword.isBlank() && !topicFilteredModel.isEmpty()) {
            topicList.setSelectedIndex(0);
        }
        topicSearchField.setNoResult(!keyword.isBlank() && topicFilteredModel.isEmpty());
    }

    private void sendMessage() {
        String topic = producerTopicField.getText().trim();
        if (topic.isBlank()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_KAFKA_ERR_TOPIC_REQUIRED));
            return;
        }
        String payload = producerPayloadArea.getText();
        String key = producerKeyField.getText().trim();
        Integer partition = (Integer) producerPartitionSpinner.getValue();
        List<Header> headers;
        try {
            headers = parseHeaders(producerHeadersArea.getText());
        } catch (IllegalArgumentException ex) {
            NotificationUtil.showWarning(rootMessage(ex));
            return;
        }

        // 在 EDT 预先读取所有 UI 字段（含 passwordField），避免非 EDT 线程读取 Swing 组件
        final Properties baseProps;
        try {
            baseProps = buildCommonClientProperties();
        } catch (IllegalArgumentException ex) {
            NotificationUtil.showWarning(rootMessage(ex));
            return;
        }
        final String currentBootstrap = baseProps.getProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "");

        sendBtn.setEnabled(false);
        producerStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        producerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_SENDING));

        SwingWorker<RecordMetadata, Void> worker = new SwingWorker<>() {
            @Override
            protected RecordMetadata doInBackground() throws Exception {
                KafkaProducer<String, String> producer = getOrCreateProducer(baseProps, currentBootstrap);
                ProducerRecord<String, String> producerRecord;
                String finalKey = key.isBlank() ? null : key;
                if (partition != null && partition >= 0) {
                    producerRecord = new ProducerRecord<>(topic, partition, finalKey, payload);
                } else {
                    producerRecord = new ProducerRecord<>(topic, finalKey, payload);
                }
                for (Header header : headers) {
                    producerRecord.headers().add(header);
                }
                return producer.send(producerRecord).get(15, TimeUnit.SECONDS);
            }

            @Override
            protected void done() {
                sendBtn.setEnabled(true);
                try {
                    RecordMetadata metadata = get();
                    String msg = t(MessageKeys.TOOLBOX_KAFKA_STATUS_SENT, metadata.partition(), metadata.offset());
                    producerStatusLabel.setForeground(ModernColors.SUCCESS);
                    producerStatusLabel.setText(msg);
                    NotificationUtil.showSuccess(msg);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("Send kafka message interrupted", ex);
                } catch (Exception ex) {
                    Throwable cause = unwrapException(ex);
                    // 发送失败时关闭缓存的 Producer，下次重建
                    closeAndClearCachedProducer();
                    producerStatusLabel.setForeground(ModernColors.ERROR);
                    producerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_SEND_FAILED, rootMessage(cause)));
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_KAFKA_STATUS_SEND_FAILED, rootMessage(cause)));
                    log.warn("Send kafka message failed", cause);
                }
            }
        };
        worker.execute();
    }

    /**
     * 获取或创建可复用的 KafkaProducer。
     * bootstrap 变更时自动关闭旧 Producer 并重建。
     */
    private synchronized KafkaProducer<String, String> getOrCreateProducer(Properties baseProps, String bootstrap) {
        KafkaProducer<String, String> existing = cachedProducerRef.get();
        if (existing != null && !bootstrap.equals(cachedProducerBootstrap)) {
            try {
                existing.close(Duration.ofSeconds(5));
            } catch (Exception e) {
                log.warn("Close old producer failed", e);
            }
            cachedProducerRef.set(null);
            cachedProducerBootstrap = null;
            existing = null;
        }
        if (existing == null) {
            Properties props = new Properties();
            props.putAll(baseProps);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
            props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "15000");
            KafkaProducer<String, String> newProducer = new KafkaProducer<>(props);
            cachedProducerRef.set(newProducer);
            cachedProducerBootstrap = bootstrap;
            return newProducer;
        }
        return existing;
    }

    /**
     * 发送失败或断开连接时，关闭并清理缓存的 Producer
     */
    private synchronized void closeAndClearCachedProducer() {
        KafkaProducer<String, String> producer = cachedProducerRef.getAndSet(null);
        if (producer != null) {
            try {
                producer.close(Duration.ofSeconds(5));
            } catch (Exception e) {
                log.warn("Close cached producer failed", e);
            }
            cachedProducerBootstrap = null;
        }
    }

    private void startConsuming() {
        SwingWorker<Void, List<ConsumedMessage>> existing = consumeWorkerRef.get();
        if (existing != null && !existing.isDone()) {
            return;
        }

        String topic = consumerTopicField.getText().trim();
        if (topic.isBlank()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_KAFKA_ERR_TOPIC_REQUIRED));
            return;
        }

        int pollTimeoutMs = (Integer) consumerPollTimeoutSpinner.getValue();
        int maxPollRecords = (Integer) consumerBatchSizeSpinner.getValue();
        ConsumeStartMode consumeStartMode = selectedConsumeStartMode();
        Long consumeStartValue;
        try {
            consumeStartValue = parseConsumeStartValue(consumeStartMode, consumeStartValueField.getText());
        } catch (IllegalArgumentException ex) {
            NotificationUtil.showWarning(rootMessage(ex));
            return;
        }

        // 在 EDT 预先读取所有 UI 字段（含 passwordField），避免非 EDT 线程读取 Swing 组件
        final Properties baseProps;
        try {
            baseProps = buildCommonClientProperties();
        } catch (IllegalArgumentException ex) {
            NotificationUtil.showWarning(rootMessage(ex));
            return;
        }

        // groupId 为空时生成临时唯一 groupId，避免干扰生产环境消费组
        String configuredGroupId = groupIdField.getText().trim();
        final String finalGroupId = configuredGroupId.isBlank()
                ? "easy-postman-consumer-" + System.currentTimeMillis()
                : configuredGroupId;

        setConsuming(true);
        consumerStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        consumerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONSUMING, topic));

        SwingWorker<Void, List<ConsumedMessage>> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Properties props = new Properties();
                props.putAll(baseProps);
                props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                props.put(ConsumerConfig.GROUP_ID_CONFIG, finalGroupId);
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumeStartMode.autoOffsetReset());
                // 禁用自动提交：工具类消费不应修改服务端 group offset
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
                props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(maxPollRecords));

                try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                    runningConsumerRef.set(consumer);
                    AtomicBoolean startPositionApplied = new AtomicBoolean(false);
                    consumer.subscribe(Collections.singletonList(topic), new org.apache.kafka.clients.consumer.ConsumerRebalanceListener() {
                        @Override
                        public void onPartitionsRevoked(Collection<org.apache.kafka.common.TopicPartition> partitions) {
                            // no-op
                        }

                        @Override
                        public void onPartitionsAssigned(Collection<org.apache.kafka.common.TopicPartition> partitions) {
                            if (startPositionApplied.getAndSet(true)) {
                                return;
                            }
                            applyConsumeStartPosition(consumer, partitions, consumeStartMode, consumeStartValue);
                        }
                    });
                    while (!isCancelled()) {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollTimeoutMs));
                        if (records.isEmpty()) {
                            continue;
                        }
                        List<ConsumedMessage> batch = new ArrayList<>(records.count());
                        for (ConsumerRecord<String, String> rec : records) {
                            batch.add(new ConsumedMessage(
                                    TIME_FORMATTER.format(LocalDateTime.now()),
                                    formatRecordTimestamp(rec.timestamp()),
                                    rec.topic(),
                                    rec.partition(),
                                    rec.offset(),
                                    rec.key() == null ? "" : rec.key(),
                                    formatHeaders(rec.headers()),
                                    rec.value() == null ? "" : rec.value()));
                        }
                        publish(batch);
                    }
                } catch (WakeupException wakeupException) {
                    if (!isCancelled()) {
                        throw wakeupException;
                    }
                } finally {
                    runningConsumerRef.compareAndSet(runningConsumerRef.get(), null);
                }
                return null;
            }

            @Override
            protected void process(List<List<ConsumedMessage>> chunks) {
                for (List<ConsumedMessage> batch : chunks) {
                    consumedMessages.addAll(batch);
                }
                trimConsumedMessages();
                refreshMessageTable();
                consumerStatusLabel.setForeground(ModernColors.INFO);
                consumerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_RECORDS, consumedMessages.size()));
            }

            @Override
            protected void done() {
                setConsuming(false);
                if (isCancelled()) {
                    consumerStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
                    consumerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_STOPPED));
                    return;
                }
                try {
                    get();
                    consumerStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
                    consumerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_STOPPED));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("Consume kafka interrupted", ex);
                } catch (Exception ex) {
                    Throwable cause = unwrapException(ex);
                    consumerStatusLabel.setForeground(ModernColors.ERROR);
                    consumerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONSUME_FAILED, rootMessage(cause)));
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONSUME_FAILED, rootMessage(cause)));
                    log.warn("Consume kafka message failed", cause);
                }
            }
        };
        consumeWorkerRef.set(worker);
        worker.execute();
    }

    private void stopConsuming() {
        SwingWorker<Void, List<ConsumedMessage>> worker = consumeWorkerRef.get();
        if (worker != null && !worker.isDone()) {
            worker.cancel(true);
        }
        KafkaConsumer<String, String> consumer = runningConsumerRef.get();
        if (consumer != null) {
            consumer.wakeup();
        }
    }

    private void clearConsumedMessages() {
        consumedMessages.clear();
        messageTablePanel.clearData();
        detailArea.setText("");
        SwingWorker<Void, List<ConsumedMessage>> worker = consumeWorkerRef.get();
        boolean consuming = worker != null && !worker.isDone();
        if (consuming) {
            String topic = consumerTopicField.getText().trim();
            consumerStatusLabel.setForeground(ModernColors.INFO);
            consumerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONSUMING, topic.isBlank() ? "-" : topic));
        } else {
            consumerStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
            consumerStatusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_READY));
        }
    }

    private void setConsuming(boolean consuming) {
        startConsumeBtn.setEnabled(!consuming);
        stopConsumeBtn.setEnabled(consuming);
    }

    private void trimConsumedMessages() {
        int maxView = (Integer) consumerMaxViewSpinner.getValue();
        if (consumedMessages.size() <= maxView) {
            return;
        }
        int removeCount = consumedMessages.size() - maxView;
        consumedMessages.subList(0, removeCount).clear();
        // 超出上限后全量重建表格（保证表格与 consumedMessages 同步）
        rebuildMessageTable();
    }

    /**
     * 全量重建消息表格（用于 trim 后或 clear 后）
     */
    private void rebuildMessageTable() {
        List<Object[]> rows = consumedMessages.stream()
                .map(item -> new Object[]{
                        item.receiveTime(),
                        item.recordTime(),
                        item.topic(),
                        item.partition(),
                        item.offset(),
                        item.key(),
                        item.headers(),
                        item.value()
                }).toList();
        messageTablePanel.setData(rows);
    }

    private void refreshMessageTable() {
        int tableRowCount = messageTablePanel.getTable().getRowCount();
        // 若 consumedMessages 因 trim 缩减，全量重建
        if (consumedMessages.size() < tableRowCount) {
            rebuildMessageTable();
            return;
        }
        // 增量追加新行，避免全量 setData 导致 UI 闪烁和性能开销
        for (int i = tableRowCount; i < consumedMessages.size(); i++) {
            ConsumedMessage item = consumedMessages.get(i);
            messageTablePanel.addRow(new Object[]{
                    item.receiveTime(),
                    item.recordTime(),
                    item.topic(),
                    item.partition(),
                    item.offset(),
                    item.key(),
                    item.headers(),
                    item.value()
            });
        }
        // 自动滚动到最新行
        if (messageTablePanel.getTable().getRowCount() > 0) {
            int last = messageTablePanel.getTable().getRowCount() - 1;
            messageTablePanel.getTable().scrollRectToVisible(
                    messageTablePanel.getTable().getCellRect(last, 0, true));
        }
    }

    private void updateDetailBySelection() {
        int viewRow = messageTablePanel.getTable().getSelectedRow();
        if (viewRow < 0) {
            detailArea.setText("");
            return;
        }
        // 将视图行索引转换为模型行索引，防止排序后错位
        int modelRow = messageTablePanel.getTable().convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= consumedMessages.size()) {
            detailArea.setText("");
            return;
        }
        ConsumedMessage msg = consumedMessages.get(modelRow);

        String detail = """
                Receive Time: %s
                Record Time: %s
                Topic: %s
                Partition: %s
                Offset: %s
                
                Key:
                %s
                
                Headers:
                %s
                
                Value:
                %s
                """.formatted(msg.receiveTime(), msg.recordTime(), msg.topic(), msg.partition(),
                msg.offset(), msg.key(), msg.headers(), msg.value());
        detailArea.setText(detail);
        detailArea.setCaretPosition(0);
    }

    private void updateEditorFont() {
        if (detailArea != null) {
            detailArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        }
        if (producerPayloadArea != null) {
            producerPayloadArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        }
    }

    private Properties buildCommonClientProperties() {
        String bootstrap = bootstrapField.getText().trim();
        if (bootstrap.isBlank()) {
            throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_KAFKA_ERR_HOST_REQUIRED));
        }

        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        String clientId = clientIdField.getText().trim();
        if (!clientId.isBlank()) {
            props.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId);
        }

        String securityProtocol = selectedSecurityProtocol();
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);

        if (securityProtocol.startsWith("SASL")) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (username.isBlank() || password.isBlank()) {
                throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_KAFKA_ERR_SASL_CREDENTIAL_REQUIRED));
            }

            String mechanism = selectedSaslMechanism();
            props.put(SaslConfigs.SASL_MECHANISM, mechanism);
            String loginModule = mechanism.startsWith("SCRAM")
                    ? "org.apache.kafka.common.security.scram.ScramLoginModule"
                    : "org.apache.kafka.common.security.plain.PlainLoginModule";
            props.put(SaslConfigs.SASL_JAAS_CONFIG,
                    loginModule + " required username=\"" + escapeJaasValue(username)
                            + "\" password=\"" + escapeJaasValue(password) + "\";");
        }

        return props;
    }

    private String selectedSecurityProtocol() {
        Object selected = securityProtocolCombo.getSelectedItem();
        return selected == null ? "PLAINTEXT" : selected.toString();
    }

    private String selectedSaslMechanism() {
        Object selected = saslMechanismCombo.getSelectedItem();
        return selected == null ? "PLAIN" : selected.toString();
    }

    private ConsumeStartMode selectedConsumeStartMode() {
        int idx = autoOffsetCombo.getSelectedIndex();
        return switch (idx) {
            case 1 -> ConsumeStartMode.EARLIEST;
            case 2 -> ConsumeStartMode.NONE;
            case 3 -> ConsumeStartMode.TIMESTAMP;
            case 4 -> ConsumeStartMode.OFFSET;
            default -> ConsumeStartMode.LATEST;
        };
    }

    private Long parseConsumeStartValue(ConsumeStartMode consumeStartMode, String startValueText) {
        if (!consumeStartMode.requiresStartValue()) {
            return null;
        }
        String valueText = startValueText == null ? "" : startValueText.trim();
        if (valueText.isBlank()) {
            throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_KAFKA_ERR_OFFSET_VALUE_REQUIRED));
        }
        if (consumeStartMode == ConsumeStartMode.TIMESTAMP) {
            // 优先尝试解析 "yyyy-MM-dd HH:mm:ss" 或 "yyyy-MM-dd HH:mm:ss.SSS" 格式
            String[] datePatterns = {"yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
            for (String pattern : datePatterns) {
                try {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
                    LocalDateTime ldt = (pattern.equals("yyyy-MM-dd"))
                            ? java.time.LocalDate.parse(valueText, fmt).atStartOfDay()
                            : LocalDateTime.parse(valueText, fmt);
                    return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                } catch (Exception ignored) {
                    // 继续尝试下一个格式
                }
            }
            // 尝试纯数字毫秒时间戳
            try {
                long value = Long.parseLong(valueText);
                if (value < 0) throw new NumberFormatException("negative");
                return value;
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_KAFKA_ERR_OFFSET_VALUE_INVALID, valueText));
            }
        }
        // OFFSET 模式：纯数字
        try {
            long value = Long.parseLong(valueText);
            if (value < 0) {
                throw new NumberFormatException("negative");
            }
            return value;
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_KAFKA_ERR_OFFSET_VALUE_INVALID, valueText));
        }
    }

    private static List<Header> parseHeaders(String headersText) {
        if (headersText == null || headersText.isBlank()) {
            return List.of();
        }
        List<Header> headers = new ArrayList<>();
        String[] lines = headersText.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                continue;
            }
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_KAFKA_ERR_HEADER_FORMAT, i + 1));
            }
            String headerKey = line.substring(0, separator).trim();
            String headerValue = line.substring(separator + 1).trim();
            if (headerKey.isBlank()) {
                throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_KAFKA_ERR_HEADER_FORMAT, i + 1));
            }
            headers.add(new RecordHeader(headerKey, headerValue.getBytes(StandardCharsets.UTF_8)));
        }
        return headers;
    }

    private static void applyConsumeStartPosition(
            KafkaConsumer<String, String> consumer,
            Collection<TopicPartition> partitions,
            ConsumeStartMode consumeStartMode,
            Long consumeStartValue) {
        if (partitions == null || partitions.isEmpty()) {
            return;
        }
        // For latest/earliest/none, rely on Kafka's auto.offset.reset and committed offsets semantics.
        if (consumeStartMode == ConsumeStartMode.LATEST
                || consumeStartMode == ConsumeStartMode.EARLIEST
                || consumeStartMode == ConsumeStartMode.NONE) {
            return;
        }
        if (consumeStartMode == ConsumeStartMode.OFFSET) {
            long offset = consumeStartValue == null ? 0L : consumeStartValue;
            for (TopicPartition partition : partitions) {
                consumer.seek(partition, offset);
            }
            return;
        }
        if (consumeStartMode == ConsumeStartMode.TIMESTAMP) {
            long timestamp = consumeStartValue == null ? 0L : consumeStartValue;
            Map<TopicPartition, Long> targetTimestamps = new HashMap<>();
            for (TopicPartition partition : partitions) {
                targetTimestamps.put(partition, timestamp);
            }
            Map<TopicPartition, OffsetAndTimestamp> offsetsByTimestamp = consumer.offsetsForTimes(targetTimestamps);
            for (TopicPartition partition : partitions) {
                OffsetAndTimestamp offsetAndTimestamp = offsetsByTimestamp.get(partition);
                if (offsetAndTimestamp == null) {
                    consumer.seekToEnd(Collections.singletonList(partition));
                } else {
                    consumer.seek(partition, offsetAndTimestamp.offset());
                }
            }
        }
    }

    private static String formatHeaders(Iterable<Header> headers) {
        List<String> values = new ArrayList<>();
        for (Header header : headers) {
            String headerValue = headerValueToDisplay(header.value());
            values.add(header.key() + "=" + headerValue);
        }
        return String.join("; ", values);
    }

    private static String headerValueToDisplay(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String utf8Text = new String(bytes, StandardCharsets.UTF_8);
        if (isLikelyReadableText(utf8Text)) {
            return utf8Text;
        }
        return "base64:" + Base64.getEncoder().encodeToString(bytes);
    }

    private static boolean isLikelyReadableText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isISOControl(ch) && !Character.isWhitespace(ch)) {
                return false;
            }
        }
        return true;
    }

    private static String formatRecordTimestamp(long timestamp) {
        if (timestamp < 0) {
            return "";
        }
        return TIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
    }

    private static String escapeJaasValue(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void doDisconnect() {
        stopConsuming();
        closeAndClearCachedProducer();
        topicListModel.clear();
        topicFilteredModel.clear();
        btnCardLayout.show(btnCard, CARD_CONNECT);
        setConnectionStatus(UIManager.getColor(LABEL_DISABLED_FG), t(MessageKeys.TOOLBOX_KAFKA_STATUS_NOT_CONNECTED));
        NotificationUtil.showInfo(t(MessageKeys.TOOLBOX_KAFKA_DISCONNECT_SUCCESS));
    }

    private void setConnectionStatus(Color color, String message) {
        connectionStatusLabel.setForeground(color);
        connectionStatusLabel.setToolTipText(message);
    }

    private static Throwable unwrapException(Exception ex) {
        if (ex instanceof ExecutionException ee && ee.getCause() != null) {
            return ee.getCause();
        }
        return ex;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String msg = current.getMessage();
        return (msg == null || msg.isBlank()) ? current.toString() : msg;
    }
}
