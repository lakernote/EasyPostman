package com.laker.postman.panel.performance;

import com.laker.postman.common.component.EasyJSpinner;
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
    private final JComboBox<SsePerformanceData.CompletionMode> completionModeBox;
    private final EasyJSpinner awaitTimeoutSpinner;
    private final EasyJSpinner holdConnectionSpinner;
    private final EasyJSpinner targetMessageCountSpinner;
    private final EasyTextField eventNameFilterField;
    private final EasyTextField messageFilterField;
    private JLabel awaitTimeoutLabel;
    private JLabel holdConnectionLabel;
    private JLabel targetMessageCountLabel;
    private JLabel messageFilterLabel;
    private JTextArea modeHintArea;
    private JMeterTreeNode requestNode;

    public SseStagePropertyPanel(Stage stage) {
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
        completionModeBox = new JComboBox<>(SsePerformanceData.CompletionMode.values());
        awaitTimeoutSpinner = new EasyJSpinner(new SpinnerNumberModel(10000, 100, 600000, 100));
        holdConnectionSpinner = new EasyJSpinner(new SpinnerNumberModel(30000, 100, 3600000, 1000));
        targetMessageCountSpinner = new EasyJSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        eventNameFilterField = new EasyTextField(20);
        messageFilterField = new EasyTextField(20);

        for (EasyJSpinner spinner : getAllSpinners()) {
            spinner.setPreferredSize(new Dimension(100, 28));
        }
        eventNameFilterField.setPreferredSize(new Dimension(420, 28));
        messageFilterField.setPreferredSize(new Dimension(420, 28));
        completionModeBox.setPreferredSize(new Dimension(420, 28));

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
                    };
                }
                return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
            }
        });

        switch (stage) {
            case CONNECT -> buildConnectPanel(gbc);
            case AWAIT -> buildAwaitPanel(gbc);
        }
        addVerticalFiller(gbc);

        completionModeBox.addActionListener(e -> updateAwaitModeState());
        updateAwaitModeState();
    }

    private void buildConnectPanel(GridBagConstraints gbc) {
        addFormRow(gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_CONNECT_TIMEOUT)),
                connectTimeoutSpinner);
    }

    private void buildAwaitPanel(GridBagConstraints gbc) {
        awaitTimeoutLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_AWAIT_TIMEOUT));
        holdConnectionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_HOLD_CONNECTION));
        targetMessageCountLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_TARGET_MESSAGE_COUNT));
        addFormRow(gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_AWAIT_MODE)),
                completionModeBox);
        addFormRow(gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_EVENT_FILTER)),
                eventNameFilterField);
        messageFilterLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_MESSAGE_FILTER));
        addFormRow(gbc, messageFilterLabel, messageFilterField);
        addFormRow(gbc, awaitTimeoutLabel, awaitTimeoutSpinner);
        addFormRow(gbc, holdConnectionLabel, holdConnectionSpinner);
        addFormRow(gbc, targetMessageCountLabel, targetMessageCountSpinner);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        modeHintArea = new JTextArea(2, 52);
        modeHintArea.setEditable(false);
        modeHintArea.setFocusable(false);
        modeHintArea.setOpaque(false);
        modeHintArea.setLineWrap(true);
        modeHintArea.setWrapStyleWord(true);
        modeHintArea.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(modeHintArea, gbc);
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
            case CONNECT -> data.connectTimeoutMs = (Integer) connectTimeoutSpinner.getValue();
            case AWAIT -> {
                data.completionMode = (SsePerformanceData.CompletionMode) completionModeBox.getSelectedItem();
                data.firstMessageTimeoutMs = (Integer) awaitTimeoutSpinner.getValue();
                data.holdConnectionMs = (Integer) holdConnectionSpinner.getValue();
                data.targetMessageCount = (Integer) targetMessageCountSpinner.getValue();
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
        if (stage != Stage.AWAIT || awaitTimeoutLabel == null || holdConnectionLabel == null
                || targetMessageCountLabel == null || messageFilterLabel == null || modeHintArea == null) {
            return;
        }
        SsePerformanceData.CompletionMode mode = (SsePerformanceData.CompletionMode) completionModeBox.getSelectedItem();
        boolean showAwaitTimeout = mode == SsePerformanceData.CompletionMode.FIRST_MESSAGE
                || mode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE
                || mode == SsePerformanceData.CompletionMode.MESSAGE_COUNT;
        boolean showMessageFilter = mode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE;
        boolean showHoldConnection = mode == SsePerformanceData.CompletionMode.FIXED_DURATION
                || mode == SsePerformanceData.CompletionMode.MESSAGE_COUNT;
        boolean showTargetCount = mode == SsePerformanceData.CompletionMode.MESSAGE_COUNT;

        awaitTimeoutLabel.setText(I18nUtil.getMessage(
                mode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE
                        ? MessageKeys.PERFORMANCE_SSE_MATCHED_MESSAGE_TIMEOUT
                        : MessageKeys.PERFORMANCE_SSE_FIRST_MESSAGE_TIMEOUT
        ));
        holdConnectionLabel.setText(I18nUtil.getMessage(
                mode == SsePerformanceData.CompletionMode.MESSAGE_COUNT
                        ? MessageKeys.PERFORMANCE_SSE_MAX_WAIT_DURATION
                        : MessageKeys.PERFORMANCE_SSE_OBSERVE_DURATION
        ));

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
        };
    }

    private void addFormRow(GridBagConstraints gbc, JComponent label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        add(field, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weightx = 0;
    }

    private void addVerticalFiller(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
    }

    private List<EasyJSpinner> getAllSpinners() {
        return Arrays.asList(connectTimeoutSpinner, awaitTimeoutSpinner, holdConnectionSpinner, targetMessageCountSpinner);
    }
}
