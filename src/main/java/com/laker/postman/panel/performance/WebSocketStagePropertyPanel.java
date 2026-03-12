package com.laker.postman.panel.performance;

import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * WebSocket 生命周期节点属性面板。
 */
public class WebSocketStagePropertyPanel extends JPanel {

    public enum Stage {
        CONNECT,
        SEND,
        AWAIT,
        CLOSE
    }

    private final Stage stage;
    private final EasyJSpinner connectTimeoutSpinner;
    private final JComboBox<WebSocketPerformanceData.SendMode> sendModeBox;
    private final EasyJSpinner sendCountSpinner;
    private final EasyJSpinner sendIntervalSpinner;
    private final JComboBox<WebSocketPerformanceData.CompletionMode> completionModeBox;
    private final EasyJSpinner awaitTimeoutSpinner;
    private final EasyJSpinner holdConnectionSpinner;
    private final EasyJSpinner targetMessageCountSpinner;
    private final EasyTextField messageFilterField;
    private JLabel sendCountLabel;
    private JLabel sendIntervalLabel;
    private JLabel awaitTimeoutLabel;
    private JLabel holdConnectionLabel;
    private JLabel targetMessageCountLabel;
    private JLabel sendHintLabel;
    private JLabel fixedDurationHintLabel;
    private JLabel closeHintLabel;
    private JMeterTreeNode currentNode;

    public WebSocketStagePropertyPanel(Stage stage) {
        this.stage = stage;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        connectTimeoutSpinner = new EasyJSpinner(new SpinnerNumberModel(10000, 100, 600000, 100));
        sendModeBox = new JComboBox<>(WebSocketPerformanceData.SendMode.values());
        sendCountSpinner = new EasyJSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
        sendIntervalSpinner = new EasyJSpinner(new SpinnerNumberModel(1000, 0, 3600000, 100));
        completionModeBox = new JComboBox<>(WebSocketPerformanceData.CompletionMode.values());
        awaitTimeoutSpinner = new EasyJSpinner(new SpinnerNumberModel(10000, 100, 600000, 100));
        holdConnectionSpinner = new EasyJSpinner(new SpinnerNumberModel(30000, 100, 3600000, 1000));
        targetMessageCountSpinner = new EasyJSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        messageFilterField = new EasyTextField(20);

        for (EasyJSpinner spinner : getAllSpinners()) {
            spinner.setPreferredSize(new Dimension(100, 28));
        }

        sendModeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object displayValue = value;
                if (value instanceof WebSocketPerformanceData.SendMode mode) {
                    displayValue = switch (mode) {
                        case NONE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_NONE);
                        case REQUEST_BODY_ON_CONNECT ->
                                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY);
                        case REQUEST_BODY_REPEAT ->
                                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY_REPEAT);
                    };
                }
                return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
            }
        });
        completionModeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object displayValue = value;
                if (value instanceof WebSocketPerformanceData.CompletionMode mode) {
                    displayValue = switch (mode) {
                        case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE);
                        case MATCHED_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MATCHED_MESSAGE);
                        case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIXED_DURATION);
                        case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MESSAGE_COUNT);
                    };
                }
                return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
            }
        });

        switch (stage) {
            case CONNECT -> buildConnectPanel(gbc);
            case SEND -> buildSendPanel(gbc);
            case AWAIT -> buildAwaitPanel(gbc);
            case CLOSE -> buildClosePanel(gbc);
        }

        sendModeBox.addActionListener(e -> updateSendModeState());
        completionModeBox.addActionListener(e -> updateAwaitModeState());
        updateSendModeState();
        updateAwaitModeState();
    }

    private void buildConnectPanel(GridBagConstraints gbc) {
        addFormRow(gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_CONNECT_TIMEOUT)),
                connectTimeoutSpinner);
    }

    private void buildSendPanel(GridBagConstraints gbc) {
        addFormRow(gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_MODE)),
                sendModeBox);
        sendCountLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_COUNT));
        sendIntervalLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_INTERVAL));
        addFormRow(gbc, sendCountLabel, sendCountSpinner);
        addFormRow(gbc, sendIntervalLabel, sendIntervalSpinner);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        sendHintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_HINT));
        sendHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(sendHintLabel, gbc);
    }

    private void buildAwaitPanel(GridBagConstraints gbc) {
        awaitTimeoutLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_FIRST_MESSAGE_TIMEOUT));
        holdConnectionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_OBSERVE_DURATION));
        targetMessageCountLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_TARGET_MESSAGE_COUNT));

        addFormRow(gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_AWAIT_MODE)),
                completionModeBox);
        addFormRow(gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_MESSAGE_FILTER)),
                messageFilterField);
        addFormRow(gbc, awaitTimeoutLabel, awaitTimeoutSpinner);
        addFormRow(gbc, holdConnectionLabel, holdConnectionSpinner);
        addFormRow(gbc, targetMessageCountLabel, targetMessageCountSpinner);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        fixedDurationHintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_FIXED_DURATION_HINT));
        fixedDurationHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(fixedDurationHintLabel, gbc);
    }

    private void buildClosePanel(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        closeHintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_CLOSE_HINT));
        closeHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(closeHintLabel, gbc);
    }

    public void setNode(JMeterTreeNode node) {
        this.currentNode = node;
        WebSocketPerformanceData data = node != null && node.webSocketPerformanceData != null
                ? node.webSocketPerformanceData
                : new WebSocketPerformanceData();
        connectTimeoutSpinner.setValue(data.connectTimeoutMs);
        sendModeBox.setSelectedItem(data.sendMode);
        sendCountSpinner.setValue(data.sendCount);
        sendIntervalSpinner.setValue(data.sendIntervalMs);
        completionModeBox.setSelectedItem(data.completionMode);
        awaitTimeoutSpinner.setValue(data.firstMessageTimeoutMs);
        holdConnectionSpinner.setValue(data.holdConnectionMs);
        targetMessageCountSpinner.setValue(data.targetMessageCount);
        messageFilterField.setText(data.messageFilter == null ? "" : data.messageFilter);
        updateSendModeState();
        updateAwaitModeState();
    }

    public void saveData() {
        if (currentNode == null) {
            return;
        }
        forceCommitAllSpinners();
        WebSocketPerformanceData data = currentNode.webSocketPerformanceData != null
                ? currentNode.webSocketPerformanceData
                : new WebSocketPerformanceData();
        switch (stage) {
            case CONNECT -> data.connectTimeoutMs = (Integer) connectTimeoutSpinner.getValue();
            case SEND -> {
                data.sendMode = (WebSocketPerformanceData.SendMode) sendModeBox.getSelectedItem();
                data.sendCount = (Integer) sendCountSpinner.getValue();
                data.sendIntervalMs = (Integer) sendIntervalSpinner.getValue();
            }
            case AWAIT -> {
                data.completionMode = (WebSocketPerformanceData.CompletionMode) completionModeBox.getSelectedItem();
                data.firstMessageTimeoutMs = (Integer) awaitTimeoutSpinner.getValue();
                data.holdConnectionMs = (Integer) holdConnectionSpinner.getValue();
                data.targetMessageCount = (Integer) targetMessageCountSpinner.getValue();
                data.messageFilter = messageFilterField.getText().trim();
            }
            case CLOSE -> {
                // No editable fields for close step.
            }
        }
        currentNode.webSocketPerformanceData = data;
    }

    public void forceCommitAllSpinners() {
        getAllSpinners().forEach(EasyJSpinner::forceCommit);
    }

    private void updateSendModeState() {
        if (stage != Stage.SEND || sendHintLabel == null) {
            return;
        }
        WebSocketPerformanceData.SendMode mode = (WebSocketPerformanceData.SendMode) sendModeBox.getSelectedItem();
        boolean showRepeatFields = mode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT;
        sendCountLabel.setVisible(showRepeatFields);
        sendCountSpinner.setVisible(showRepeatFields);
        sendIntervalLabel.setVisible(showRepeatFields);
        sendIntervalSpinner.setVisible(showRepeatFields);
        sendHintLabel.setVisible(mode == WebSocketPerformanceData.SendMode.REQUEST_BODY_ON_CONNECT
                || mode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT);
        revalidate();
        repaint();
    }

    private void updateAwaitModeState() {
        if (stage != Stage.AWAIT || awaitTimeoutLabel == null || holdConnectionLabel == null
                || targetMessageCountLabel == null || fixedDurationHintLabel == null) {
            return;
        }
        WebSocketPerformanceData.CompletionMode mode =
                (WebSocketPerformanceData.CompletionMode) completionModeBox.getSelectedItem();
        boolean showAwaitTimeout = mode == WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE
                || mode == WebSocketPerformanceData.CompletionMode.MATCHED_MESSAGE
                || mode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT;
        boolean showHoldConnection = mode == WebSocketPerformanceData.CompletionMode.FIXED_DURATION
                || mode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT;
        boolean showTargetCount = mode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT;
        boolean showFixedHint = mode == WebSocketPerformanceData.CompletionMode.FIXED_DURATION;

        holdConnectionLabel.setText(I18nUtil.getMessage(
                mode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT
                        ? MessageKeys.PERFORMANCE_WS_MAX_WAIT_DURATION
                        : MessageKeys.PERFORMANCE_WS_OBSERVE_DURATION
        ));

        awaitTimeoutLabel.setVisible(showAwaitTimeout);
        awaitTimeoutSpinner.setVisible(showAwaitTimeout);
        holdConnectionLabel.setVisible(showHoldConnection);
        holdConnectionSpinner.setVisible(showHoldConnection);
        targetMessageCountLabel.setVisible(showTargetCount);
        targetMessageCountSpinner.setVisible(showTargetCount);
        fixedDurationHintLabel.setVisible(showFixedHint);
        revalidate();
        repaint();
    }

    private void addFormRow(GridBagConstraints gbc, JComponent label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        add(field, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
    }

    private List<EasyJSpinner> getAllSpinners() {
        return Arrays.asList(connectTimeoutSpinner, sendCountSpinner, sendIntervalSpinner,
                awaitTimeoutSpinner, holdConnectionSpinner, targetMessageCountSpinner);
    }
}
