package com.laker.postman.panel.toolbox.kafka;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.panel.toolbox.kafka.connection.ui.KafkaConnectionPanel;
import com.laker.postman.panel.toolbox.kafka.consumer.KafkaConsumeStartMode;
import com.laker.postman.panel.toolbox.kafka.consumer.KafkaConsumedMessage;
import com.laker.postman.panel.toolbox.kafka.consumer.ui.KafkaConsumerPanel;
import com.laker.postman.panel.toolbox.kafka.producer.ui.KafkaProducerPanel;
import com.laker.postman.panel.toolbox.kafka.shared.KafkaPanelSupport;
import com.laker.postman.panel.toolbox.kafka.ui.KafkaTopicPanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
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

    private static final String LABEL_DISABLED_FG = "Label.disabledForeground";
    private static final String SEPARATOR_FG = "Separator.foreground";
    private static final String CARD_CONNECT = "connect";
    private static final String CARD_DISCONNECT = "disconnect";
    private static final String TIMEOUT_MS = "3000";

    private KafkaConnectionPanel connectionPanel;

    private KafkaTopicPanel topicPanel;
    private KafkaProducerPanel producerPanel;
    private KafkaConsumerPanel consumerPanel;

    private final List<KafkaConsumedMessage> consumedMessages = new ArrayList<>();
    private final AtomicReference<SwingWorker<Void, List<KafkaConsumedMessage>>> consumeWorkerRef = new AtomicReference<>();
    private final AtomicReference<KafkaConsumer<String, String>> runningConsumerRef = new AtomicReference<>();
    /**
     * 缓存可复用的 Producer，避免每次发送都重建连接
     */
    private final AtomicReference<KafkaProducer<String, String>> cachedProducerRef = new AtomicReference<>();
    /**
     * 缓存 Producer 对应的配置签名，配置变更时重建
     */
    private volatile String cachedProducerConfigSignature;

    public KafkaPanel() {
        initUI();
    }

    private static String t(String key, Object... args) {
        return I18nUtil.getMessage(key, args);
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        connectionPanel = new KafkaConnectionPanel(this::loadTopics, this::doDisconnect);
        connectionPanel.securityProtocolCombo.addActionListener(e -> updateSecurityFieldsEnabledState());
        producerPanel = new KafkaProducerPanel(this::sendMessage);
        consumerPanel = new KafkaConsumerPanel(this::startConsuming, this::stopConsuming, this::clearConsumedMessages, this::updateDetailBySelection);
        consumerPanel.autoOffsetCombo.addActionListener(e -> updateConsumeStartValueEnabledState());
        topicPanel = new KafkaTopicPanel(this::loadTopics, this::applyTopicSelection);
        updateSecurityFieldsEnabledState();
        updateConsumeStartValueEnabledState();
        updateEditorFont();
        topicPanel.topicSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
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

        add(connectionPanel, BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, topicPanel, buildWorkPanel());
        mainSplit.setDividerLocation(240);
        mainSplit.setDividerSize(3);
        mainSplit.setResizeWeight(0.23);
        mainSplit.setContinuousLayout(true);
        add(mainSplit, BorderLayout.CENTER);
    }

    private JComponent buildWorkPanel() {
        JTabbedPane workTabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        workTabs.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        workTabs.addTab(
                t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_TITLE),
                IconUtil.createThemed("icons/send.svg", 16, 16),
                producerPanel,
                t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_TITLE));
        workTabs.addTab(
                t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_TITLE),
                IconUtil.createThemed("icons/start.svg", 16, 16),
                consumerPanel,
                t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_TITLE));
        return workTabs;
    }

    private void applyTopicSelection(String selected) {
        if (selected == null || selected.isBlank()) {
            return;
        }
        producerPanel.topicField.setText(selected);
        consumerPanel.topicField.setText(selected);
    }


    private void updateSecurityFieldsEnabledState() {
        boolean saslEnabled = selectedSecurityProtocol().startsWith("SASL");
        connectionPanel.saslMechanismCombo.setEnabled(saslEnabled);
        connectionPanel.usernameField.setEnabled(saslEnabled);
        connectionPanel.passwordField.setEnabled(saslEnabled);
    }

    private void updateConsumeStartValueEnabledState() {
        KafkaConsumeStartMode consumeStartMode = selectedConsumeStartMode();
        boolean enabled = consumeStartMode.requiresStartValue();
        consumerPanel.consumeStartValueField.setEnabled(enabled);
        if (consumeStartMode == KafkaConsumeStartMode.TIMESTAMP) {
            consumerPanel.consumeStartValueField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE_PLACEHOLDER) + " (e.g. 2024-01-01 00:00:00 or ms)");
        } else if (consumeStartMode == KafkaConsumeStartMode.OFFSET) {
            consumerPanel.consumeStartValueField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE_PLACEHOLDER) + " (e.g. 100)");
        } else {
            consumerPanel.consumeStartValueField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE_PLACEHOLDER));
        }
        consumerPanel.consumeStartValueField.repaint();
    }

    private void loadTopics() {
        String bootstrap = connectionPanel.bootstrapField.getText().trim();
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

        connectionPanel.connectBtn.setEnabled(false);
        setConnectionStatus(ModernColors.INFO, t(MessageKeys.TOOLBOX_KAFKA_STATUS_LOADING_TOPICS));

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                Properties props = new Properties();
                props.putAll(baseProps);
                props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
                props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
                try (AdminClient adminClient = AdminClient.create(props)) {
                    ListTopicsOptions options = new ListTopicsOptions().listInternal(false).timeoutMs(Integer.parseInt(TIMEOUT_MS));
                    List<String> topics = new ArrayList<>(adminClient.listTopics(options).names().get(Integer.parseInt(TIMEOUT_MS), TimeUnit.MILLISECONDS));
                    Collections.sort(topics);
                    return topics;
                }
            }

            @Override
            protected void done() {
                connectionPanel.connectBtn.setEnabled(true);
                try {
                    List<String> topics = get();
                    topicPanel.topicListModel.clear();
                    for (String topic : topics) {
                        topicPanel.topicListModel.addElement(topic);
                    }
                    applyTopicFilter();
                    if (!topics.isEmpty() && producerPanel.topicField.getText().isBlank() && consumerPanel.topicField.getText().isBlank()) {
                        producerPanel.topicField.setText(topics.get(0));
                        consumerPanel.topicField.setText(topics.get(0));
                    }
                    String status = t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONNECTED, bootstrap);
                    setConnectionStatus(ModernColors.SUCCESS, status);
                    connectionPanel.btnCardLayout.show(connectionPanel.btnCard, CARD_DISCONNECT);
                    producerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_TOPICS_LOADED, topics.size()));
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
        String keyword = topicPanel.topicSearchField.getText().trim().toLowerCase(Locale.ROOT);
        String previousSelection = topicPanel.topicList.getSelectedValue();
        topicPanel.topicFilteredModel.clear();
        for (int i = 0; i < topicPanel.topicListModel.getSize(); i++) {
            String topic = topicPanel.topicListModel.getElementAt(i);
            if (keyword.isBlank() || topic.toLowerCase(Locale.ROOT).contains(keyword)) {
                topicPanel.topicFilteredModel.addElement(topic);
            }
        }
        if (previousSelection != null && !previousSelection.isBlank()) {
            for (int i = 0; i < topicPanel.topicFilteredModel.getSize(); i++) {
                if (previousSelection.equals(topicPanel.topicFilteredModel.getElementAt(i))) {
                    topicPanel.topicList.setSelectedIndex(i);
                    topicPanel.topicList.ensureIndexIsVisible(i);
                    break;
                }
            }
        }
        if (topicPanel.topicList.getSelectedIndex() < 0 && keyword.isBlank() && !topicPanel.topicFilteredModel.isEmpty()) {
            topicPanel.topicList.setSelectedIndex(0);
        }
        topicPanel.topicSearchField.setNoResult(!keyword.isBlank() && topicPanel.topicFilteredModel.isEmpty());
    }

    private void sendMessage() {
        String topic = producerPanel.topicField.getText().trim();
        if (topic.isBlank()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_KAFKA_ERR_TOPIC_REQUIRED));
            return;
        }
        String payload = producerPanel.payloadArea.getText();
        String key = producerPanel.keyField.getText().trim();
        Integer partition = (Integer) producerPanel.partitionSpinner.getValue();
        List<Header> headers;
        try {
            headers = KafkaPanelSupport.parseHeaders(producerPanel.headersArea.getText());
        } catch (IllegalArgumentException ex) {
            NotificationUtil.showWarning(rootMessage(ex));
            return;
        }
        final Properties producerCustomProps;
        try {
            producerCustomProps = KafkaPanelSupport.parseCustomKafkaProperties(producerPanel.customPropsPanel.getValue());
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
        final Properties producerProps = buildProducerProperties(baseProps, producerCustomProps);
        final String producerConfigSignature = buildPropertiesSignature(producerProps);

        producerPanel.sendBtn.setEnabled(false);
        producerPanel.statusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        producerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_SENDING));

        SwingWorker<RecordMetadata, Void> worker = new SwingWorker<>() {
            @Override
            protected RecordMetadata doInBackground() throws Exception {
                KafkaProducer<String, String> producer = getOrCreateProducer(producerProps, producerConfigSignature);
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
                producerPanel.sendBtn.setEnabled(true);
                try {
                    RecordMetadata metadata = get();
                    String msg = t(MessageKeys.TOOLBOX_KAFKA_STATUS_SENT, metadata.partition(), metadata.offset());
                    producerPanel.statusLabel.setForeground(ModernColors.SUCCESS);
                    producerPanel.statusLabel.setText(msg);
                    NotificationUtil.showSuccess(msg);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("Send kafka message interrupted", ex);
                } catch (Exception ex) {
                    Throwable cause = unwrapException(ex);
                    // 发送失败时关闭缓存的 Producer，下次重建
                    closeAndClearCachedProducer();
                    producerPanel.statusLabel.setForeground(ModernColors.ERROR);
                    producerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_SEND_FAILED, rootMessage(cause)));
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_KAFKA_STATUS_SEND_FAILED, rootMessage(cause)));
                    log.warn("Send kafka message failed", cause);
                }
            }
        };
        worker.execute();
    }

    /**
     * 获取或创建可复用的 KafkaProducer。
     * 配置变更时自动关闭旧 Producer 并重建。
     */
    private synchronized KafkaProducer<String, String> getOrCreateProducer(Properties producerProps, String producerConfigSignature) {
        KafkaProducer<String, String> existing = cachedProducerRef.get();
        if (existing != null && !producerConfigSignature.equals(cachedProducerConfigSignature)) {
            try {
                existing.close(Duration.ofSeconds(5));
            } catch (Exception e) {
                log.warn("Close old producer failed", e);
            }
            cachedProducerRef.set(null);
            cachedProducerConfigSignature = null;
            existing = null;
        }
        if (existing == null) {
            KafkaProducer<String, String> newProducer = new KafkaProducer<>(producerProps);
            cachedProducerRef.set(newProducer);
            cachedProducerConfigSignature = producerConfigSignature;
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
        }
        cachedProducerConfigSignature = null;
    }

    private void startConsuming() {
        SwingWorker<Void, List<KafkaConsumedMessage>> existing = consumeWorkerRef.get();
        if (existing != null && !existing.isDone()) {
            return;
        }

        String topic = consumerPanel.topicField.getText().trim();
        if (topic.isBlank()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_KAFKA_ERR_TOPIC_REQUIRED));
            return;
        }

        int pollTimeoutMs = (Integer) consumerPanel.pollTimeoutSpinner.getValue();
        int maxPollRecords = (Integer) consumerPanel.batchSizeSpinner.getValue();
        KafkaConsumeStartMode consumeStartMode = selectedConsumeStartMode();
        Long consumeStartValue;
        try {
            consumeStartValue = KafkaPanelSupport.parseConsumeStartValue(consumeStartMode, consumerPanel.consumeStartValueField.getText());
        } catch (IllegalArgumentException ex) {
            NotificationUtil.showWarning(rootMessage(ex));
            return;
        }
        final Properties consumerCustomProps;
        try {
            consumerCustomProps = KafkaPanelSupport.parseCustomKafkaProperties(consumerPanel.customPropsPanel.getValue());
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
        String configuredGroupId = consumerPanel.groupIdField.getText().trim();
        final String finalGroupId = configuredGroupId.isBlank()
                ? "easy-postman-consumer-" + System.currentTimeMillis()
                : configuredGroupId;

        setConsuming(true);
        consumerPanel.statusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        consumerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONSUMING, topic));

        SwingWorker<Void, List<KafkaConsumedMessage>> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Properties props = buildConsumerProperties(baseProps, consumerCustomProps, finalGroupId, consumeStartMode, maxPollRecords);

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
                            KafkaPanelSupport.applyConsumeStartPosition(consumer, partitions, consumeStartMode, consumeStartValue);
                        }
                    });
                    while (!isCancelled()) {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollTimeoutMs));
                        if (records.isEmpty()) {
                            continue;
                        }
                        List<KafkaConsumedMessage> batch = new ArrayList<>(records.count());
                        for (ConsumerRecord<String, String> rec : records) {
                            batch.add(new KafkaConsumedMessage(
                                    KafkaPanelSupport.formatRecordTimestamp(System.currentTimeMillis()),
                                    KafkaPanelSupport.formatRecordTimestamp(rec.timestamp()),
                                    rec.topic(),
                                    rec.partition(),
                                    rec.offset(),
                                    rec.key() == null ? "" : rec.key(),
                                    KafkaPanelSupport.formatHeaders(rec.headers()),
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
            protected void process(List<List<KafkaConsumedMessage>> chunks) {
                for (List<KafkaConsumedMessage> batch : chunks) {
                    consumedMessages.addAll(batch);
                }
                trimConsumedMessages();
                refreshMessageTable();
                consumerPanel.statusLabel.setForeground(ModernColors.INFO);
                consumerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_RECORDS, consumedMessages.size()));
            }

            @Override
            protected void done() {
                setConsuming(false);
                if (isCancelled()) {
                    consumerPanel.statusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
                    consumerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_STOPPED));
                    return;
                }
                try {
                    get();
                    consumerPanel.statusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
                    consumerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_STOPPED));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("Consume kafka interrupted", ex);
                } catch (Exception ex) {
                    Throwable cause = unwrapException(ex);
                    consumerPanel.statusLabel.setForeground(ModernColors.ERROR);
                    consumerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONSUME_FAILED, rootMessage(cause)));
                    NotificationUtil.showError(t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONSUME_FAILED, rootMessage(cause)));
                    log.warn("Consume kafka message failed", cause);
                }
            }
        };
        consumeWorkerRef.set(worker);
        worker.execute();
    }

    private void stopConsuming() {
        SwingWorker<Void, List<KafkaConsumedMessage>> worker = consumeWorkerRef.get();
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
        consumerPanel.messageTablePanel.clearData();
        consumerPanel.clearDetail();
        SwingWorker<Void, List<KafkaConsumedMessage>> worker = consumeWorkerRef.get();
        boolean consuming = worker != null && !worker.isDone();
        if (consuming) {
            String topic = consumerPanel.topicField.getText().trim();
            consumerPanel.statusLabel.setForeground(ModernColors.INFO);
            consumerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_CONSUMING, topic.isBlank() ? "-" : topic));
        } else {
            consumerPanel.statusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
            consumerPanel.statusLabel.setText(t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_READY));
        }
    }

    private void setConsuming(boolean consuming) {
        consumerPanel.setConsuming(consuming);
    }

    private void trimConsumedMessages() {
        int maxView = (Integer) consumerPanel.maxViewSpinner.getValue();
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
        consumerPanel.messageTablePanel.setData(rows);
    }

    private void refreshMessageTable() {
        int tableRowCount = consumerPanel.messageTablePanel.getTable().getRowCount();
        // 若 consumedMessages 因 trim 缩减，全量重建
        if (consumedMessages.size() < tableRowCount) {
            rebuildMessageTable();
            return;
        }
        // 增量追加新行，避免全量 setData 导致 UI 闪烁和性能开销
        for (int i = tableRowCount; i < consumedMessages.size(); i++) {
            KafkaConsumedMessage item = consumedMessages.get(i);
            consumerPanel.messageTablePanel.addRow(new Object[]{
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
        if (consumerPanel.messageTablePanel.getTable().getRowCount() > 0) {
            int last = consumerPanel.messageTablePanel.getTable().getRowCount() - 1;
            consumerPanel.messageTablePanel.getTable().scrollRectToVisible(
                    consumerPanel.messageTablePanel.getTable().getCellRect(last, 0, true));
        }
    }

    private void updateDetailBySelection() {
        int viewRow = consumerPanel.messageTablePanel.getTable().getSelectedRow();
        if (viewRow < 0) {
            consumerPanel.clearDetail();
            return;
        }
        // 将视图行索引转换为模型行索引，防止排序后错位
        int modelRow = consumerPanel.messageTablePanel.getTable().convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= consumedMessages.size()) {
            consumerPanel.clearDetail();
            return;
        }
        consumerPanel.updateDetail(consumedMessages.get(modelRow));
    }

    private void updateEditorFont() {
        producerPanel.updateEditorFont();
        consumerPanel.updateEditorFont();
    }

    private Properties buildCommonClientProperties() {
        String bootstrap = connectionPanel.bootstrapField.getText().trim();
        if (bootstrap.isBlank()) {
            throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_KAFKA_ERR_HOST_REQUIRED));
        }

        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        String clientId = connectionPanel.clientIdField.getText().trim();
        if (!clientId.isBlank()) {
            props.put(CommonClientConfigs.CLIENT_ID_CONFIG, clientId);
        }

        String securityProtocol = selectedSecurityProtocol();
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);

        if (securityProtocol.startsWith("SASL")) {
            String username = connectionPanel.usernameField.getText().trim();
            String password = new String(connectionPanel.passwordField.getPassword());
            if (username.isBlank() || password.isBlank()) {
                throw new IllegalArgumentException(t(MessageKeys.TOOLBOX_KAFKA_ERR_SASL_CREDENTIAL_REQUIRED));
            }

            String mechanism = selectedSaslMechanism();
            props.put(SaslConfigs.SASL_MECHANISM, mechanism);
            String loginModule = mechanism.startsWith("SCRAM")
                    ? "org.apache.kafka.common.security.scram.ScramLoginModule"
                    : "org.apache.kafka.common.security.plain.PlainLoginModule";
            props.put(SaslConfigs.SASL_JAAS_CONFIG,
                    loginModule + " required username=\"" + KafkaPanelSupport.escapeJaasValue(username)
                            + "\" password=\"" + KafkaPanelSupport.escapeJaasValue(password) + "\";");
        }

        return props;
    }

    private Properties buildProducerProperties(Properties baseProps, Properties producerCustomProps) {
        Properties props = new Properties();
        props.putAll(baseProps);
        props.putAll(producerCustomProps);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
        props.putIfAbsent(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, TIMEOUT_MS);
        props.putIfAbsent(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "15000");
        return props;
    }

    private Properties buildConsumerProperties(
            Properties baseProps,
            Properties consumerCustomProps,
            String groupId,
            KafkaConsumeStartMode consumeStartMode,
            int maxPollRecords) {
        Properties props = new Properties();
        props.putAll(baseProps);
        props.putAll(consumerCustomProps);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumeStartMode.autoOffsetReset());
        // 禁用自动提交：工具类消费不应修改服务端 group offset
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(maxPollRecords));
        return props;
    }

    private String selectedSecurityProtocol() {
        Object selected = connectionPanel.securityProtocolCombo.getSelectedItem();
        return selected == null ? "PLAINTEXT" : selected.toString();
    }

    private String selectedSaslMechanism() {
        Object selected = connectionPanel.saslMechanismCombo.getSelectedItem();
        return selected == null ? "PLAIN" : selected.toString();
    }

    private KafkaConsumeStartMode selectedConsumeStartMode() {
        int idx = consumerPanel.autoOffsetCombo.getSelectedIndex();
        return switch (idx) {
            case 1 -> KafkaConsumeStartMode.EARLIEST;
            case 2 -> KafkaConsumeStartMode.NONE;
            case 3 -> KafkaConsumeStartMode.TIMESTAMP;
            case 4 -> KafkaConsumeStartMode.OFFSET;
            default -> KafkaConsumeStartMode.LATEST;
        };
    }

    private static String buildPropertiesSignature(Properties properties) {
        return KafkaPanelSupport.buildPropertiesSignature(properties);
    }

    private void doDisconnect() {
        stopConsuming();
        closeAndClearCachedProducer();
        topicPanel.topicListModel.clear();
        topicPanel.topicFilteredModel.clear();
        connectionPanel.btnCardLayout.show(connectionPanel.btnCard, CARD_CONNECT);
        setConnectionStatus(UIManager.getColor(LABEL_DISABLED_FG), t(MessageKeys.TOOLBOX_KAFKA_STATUS_NOT_CONNECTED));
        NotificationUtil.showInfo(t(MessageKeys.TOOLBOX_KAFKA_DISCONNECT_SUCCESS));
    }

    private void setConnectionStatus(Color color, String message) {
        connectionPanel.connectionStatusLabel.setForeground(color);
        connectionPanel.connectionStatusLabel.setToolTipText(message);
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
