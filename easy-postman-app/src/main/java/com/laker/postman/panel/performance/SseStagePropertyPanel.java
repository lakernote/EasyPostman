package com.laker.postman.panel.performance;

import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * SSE 生命周期节点属性面板。
 */
public class SseStagePropertyPanel extends JPanel {
    public enum Stage {
        CONNECT,
        AWAIT
    }

    private final Stage stage;
    private final EasyJSpinner connectTimeoutSpinner;
    private final EasyComboBox<SsePerformanceData.CompletionMode> completionModeBox;
    private final EasyJSpinner awaitTimeoutSpinner;
    private final EasyJSpinner holdConnectionSpinner;
    private final EasyJSpinner targetMessageCountSpinner;
    private final EasyTextField eventNameFilterField;
    private final EasyTextField messageFilterField;
    private JLabel eventNameFilterLabel;
    private JLabel awaitTimeoutLabel;
    private JLabel holdConnectionLabel;
    private JLabel targetMessageCountLabel;
    private JLabel messageFilterLabel;
    private JTextArea modeHintArea;
    private JMeterTreeNode requestNode;

    public SseStagePropertyPanel(Stage stage) {
        this.stage = stage;
        setLayout(new GridBagLayout());
        PerformanceStagePropertyLayout.applyCompactBorder(this);
        GridBagConstraints gbc = PerformanceStagePropertyLayout.createBaseConstraints();

        connectTimeoutSpinner = EasyJSpinner.intSpinner(10000, 100, 600000, 100);
        completionModeBox = new EasyComboBox<>(
                SsePerformanceData.CompletionMode.values(),
                EasyComboBox.WidthMode.FIXED_MAX
        );
        awaitTimeoutSpinner = EasyJSpinner.intSpinner(10000, 100, 600000, 100);
        holdConnectionSpinner = EasyJSpinner.intSpinner(30000, 100, 3600000, 1000);
        targetMessageCountSpinner = EasyJSpinner.intSpinner(1, 1, 100000, 1);
        eventNameFilterField = new EasyTextField(20);
        messageFilterField = new EasyTextField(20);

        for (EasyJSpinner spinner : getAllSpinners()) {
            PerformanceStagePropertyLayout.configureFieldWidth(spinner,
                    PerformanceStagePropertyLayout.SPINNER_FIELD_WIDTH,
                    PerformanceStagePropertyLayout.SPINNER_FIELD_WIDTH);
        }
        PerformanceStagePropertyLayout.configureFieldWidth(eventNameFilterField,
                PerformanceStagePropertyLayout.TEXT_FIELD_WIDTH,
                PerformanceStagePropertyLayout.TEXT_FIELD_WIDTH);
        PerformanceStagePropertyLayout.configureFieldWidth(messageFilterField,
                PerformanceStagePropertyLayout.TEXT_FIELD_WIDTH,
                PerformanceStagePropertyLayout.TEXT_FIELD_WIDTH);

        completionModeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object displayValue = value;
                if (value instanceof SsePerformanceData.CompletionMode mode) {
                    displayValue = switch (mode) {
                        case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_FIRST_MESSAGE);
                        case MATCHED_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_MATCHED_MESSAGE);
                        case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_FIXED_DURATION);
                        case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_MESSAGE_COUNT);
                        case STREAM_CLOSED -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_STREAM_CLOSED);
                    };
                }
                return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
            }
        });

        switch (stage) {
            case CONNECT -> buildConnectPanel(gbc);
            case AWAIT -> buildAwaitPanel(gbc);
        }
        PerformanceStagePropertyLayout.addVerticalFiller(this, gbc, 2);

        completionModeBox.addActionListener(e -> updateAwaitModeState());
        updateAwaitModeState();
    }

    private void buildConnectPanel(GridBagConstraints gbc) {
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(this, gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_CONNECT_TIMEOUT)),
                connectTimeoutSpinner);
    }

    private void buildAwaitPanel(GridBagConstraints gbc) {
        awaitTimeoutLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_AWAIT_TIMEOUT));
        holdConnectionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_HOLD_CONNECTION));
        targetMessageCountLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_TARGET_MESSAGE_COUNT));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints rowGbc = new GridBagConstraints();
        rowGbc.insets = new Insets(3, 6, 3, 6);
        rowGbc.anchor = GridBagConstraints.WEST;
        rowGbc.gridx = 0;
        rowGbc.gridy = 0;

        addCompactFormRow(formPanel, rowGbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_AWAIT_MODE)),
                completionModeBox);
        eventNameFilterLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_EVENT_FILTER));
        addCompactFormRow(formPanel, rowGbc, eventNameFilterLabel, eventNameFilterField);
        messageFilterLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_MESSAGE_FILTER));
        addCompactFormRow(formPanel, rowGbc, messageFilterLabel, messageFilterField);
        addCompactFormRow(formPanel, rowGbc, awaitTimeoutLabel, awaitTimeoutSpinner);
        addCompactFormRow(formPanel, rowGbc, holdConnectionLabel, holdConnectionSpinner);
        addCompactFormRow(formPanel, rowGbc, targetMessageCountLabel, targetMessageCountSpinner);

        rowGbc.gridx = 0;
        rowGbc.gridwidth = 2;
        rowGbc.weightx = 0;
        rowGbc.fill = GridBagConstraints.HORIZONTAL;
        modeHintArea = new JTextArea(2, 42);
        modeHintArea.setEditable(false);
        modeHintArea.setFocusable(false);
        modeHintArea.setOpaque(false);
        modeHintArea.setLineWrap(true);
        modeHintArea.setWrapStyleWord(true);
        modeHintArea.setForeground(UIManager.getColor("Label.disabledForeground"));
        formPanel.add(modeHintArea, rowGbc);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTH;
        add(formPanel, gbc);
        gbc.gridy++;
    }

    public void setRequestNode(JMeterTreeNode requestNode) {
        this.requestNode = requestNode;
        SsePerformanceData data = requestNode != null && requestNode.ssePerformanceData != null
                ? requestNode.ssePerformanceData
                : new SsePerformanceData();
        connectTimeoutSpinner.setValue(data.connectTimeoutMs);
        completionModeBox.setSelectedItem(data.completionMode);
        awaitTimeoutSpinner.setValue(data.firstMessageTimeoutMs);
        holdConnectionSpinner.setValue(data.holdConnectionMs);
        targetMessageCountSpinner.setValue(data.targetMessageCount);
        eventNameFilterField.setText(data.eventNameFilter == null ? "" : data.eventNameFilter);
        messageFilterField.setText(data.messageFilter == null ? "" : data.messageFilter);
        updateAwaitModeState();
    }

    public void saveData() {
        if (requestNode == null) {
            return;
        }
        forceCommitAllSpinners();
        SsePerformanceData data = requestNode.ssePerformanceData != null ? requestNode.ssePerformanceData : new SsePerformanceData();
        switch (stage) {
            case CONNECT -> data.connectTimeoutMs = connectTimeoutSpinner.getCommittedIntValue();
            case AWAIT -> {
                data.completionMode = (SsePerformanceData.CompletionMode) completionModeBox.getSelectedItem();
                data.firstMessageTimeoutMs = awaitTimeoutSpinner.getCommittedIntValue();
                data.holdConnectionMs = holdConnectionSpinner.getCommittedIntValue();
                data.targetMessageCount = targetMessageCountSpinner.getCommittedIntValue();
                data.eventNameFilter = eventNameFilterField.getText().trim();
                data.messageFilter = messageFilterField.getText().trim();
            }
        }
        requestNode.ssePerformanceData = data;
    }

    public void forceCommitAllSpinners() {
        getAllSpinners().forEach(EasyJSpinner::forceCommit);
    }

    private void updateAwaitModeState() {
        if (stage != Stage.AWAIT || eventNameFilterLabel == null || awaitTimeoutLabel == null
                || holdConnectionLabel == null || targetMessageCountLabel == null
                || messageFilterLabel == null || modeHintArea == null) {
            return;
        }
        SsePerformanceData.CompletionMode mode = (SsePerformanceData.CompletionMode) completionModeBox.getSelectedItem();
        boolean showEventFilter = SsePerformanceData.usesEventNameFilter(mode);
        boolean showAwaitTimeout = mode == SsePerformanceData.CompletionMode.FIRST_MESSAGE
                || mode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE
                || mode == SsePerformanceData.CompletionMode.MESSAGE_COUNT;
        boolean showMessageFilter = mode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE;
        boolean showHoldConnection = mode == SsePerformanceData.CompletionMode.FIXED_DURATION
                || mode == SsePerformanceData.CompletionMode.MESSAGE_COUNT
                || mode == SsePerformanceData.CompletionMode.STREAM_CLOSED;
        boolean showTargetCount = mode == SsePerformanceData.CompletionMode.MESSAGE_COUNT;

        String holdConnectionKey = switch (mode) {
            case MESSAGE_COUNT -> MessageKeys.PERFORMANCE_SSE_MAX_WAIT_DURATION;
            case STREAM_CLOSED -> MessageKeys.PERFORMANCE_SSE_STREAM_CLOSE_TIMEOUT;
            default -> MessageKeys.PERFORMANCE_SSE_OBSERVE_DURATION;
        };
        holdConnectionLabel.setText(I18nUtil.getMessage(holdConnectionKey));
        String awaitTimeoutKey = switch (mode) {
            case FIRST_MESSAGE -> MessageKeys.PERFORMANCE_SSE_FIRST_EVENT_TIMEOUT;
            case MATCHED_MESSAGE -> MessageKeys.PERFORMANCE_SSE_MATCHED_MESSAGE_TIMEOUT;
            case MESSAGE_COUNT -> MessageKeys.PERFORMANCE_SSE_FIRST_MESSAGE_TIMEOUT;
            case FIXED_DURATION, STREAM_CLOSED -> MessageKeys.PERFORMANCE_SSE_AWAIT_TIMEOUT;
        };
        awaitTimeoutLabel.setText(I18nUtil.getMessage(awaitTimeoutKey));

        eventNameFilterLabel.setVisible(showEventFilter);
        eventNameFilterField.setVisible(showEventFilter);
        messageFilterLabel.setVisible(showMessageFilter);
        messageFilterField.setVisible(showMessageFilter);
        awaitTimeoutLabel.setVisible(showAwaitTimeout);
        awaitTimeoutSpinner.setVisible(showAwaitTimeout);
        holdConnectionLabel.setVisible(showHoldConnection);
        holdConnectionSpinner.setVisible(showHoldConnection);
        targetMessageCountLabel.setVisible(showTargetCount);
        targetMessageCountSpinner.setVisible(showTargetCount);
        modeHintArea.setText(resolveModeHint(mode));
        revalidate();
        repaint();
    }

    private String resolveModeHint(SsePerformanceData.CompletionMode mode) {
        if (mode == null) {
            mode = SsePerformanceData.CompletionMode.FIRST_MESSAGE;
        }
        return switch (mode) {
            case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_HINT_FIRST_MESSAGE);
            case MATCHED_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_HINT_MATCHED_MESSAGE);
            case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_HINT_FIXED_DURATION);
            case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_HINT_MESSAGE_COUNT);
            case STREAM_CLOSED -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_HINT_STREAM_CLOSED);
        };
    }

    private void addCompactFormRow(JPanel panel, GridBagConstraints gbc, JComponent label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        panel.add(field, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
    }

    private List<EasyJSpinner> getAllSpinners() {
        return Arrays.asList(connectTimeoutSpinner, awaitTimeoutSpinner, holdConnectionSpinner, targetMessageCountSpinner);
    }
}
