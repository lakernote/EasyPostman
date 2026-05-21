package com.laker.postman.panel.performance;

import com.laker.postman.common.component.EasyJSpinner;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.EasyTextField;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.button.SnippetButton;
import com.laker.postman.common.component.dialog.SnippetDialog;
import com.laker.postman.common.component.editor.PostmanJavaScriptTokenMaker;
import com.laker.postman.common.component.editor.ScriptSnippetManager;
import com.laker.postman.common.component.tab.IndicatorTabComponent;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.Snippet;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.folding.CurlyFoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * WebSocket 生命周期节点属性面板。
 */
@Slf4j
public class WebSocketStagePropertyPanel extends JPanel {
    private static final String POSTMAN_JS_SYNTAX = "text/postman-javascript";
    private static final String CUSTOM_BODY_CARD = "customBody";
    private static final String REQUEST_BODY_CARD = "requestBody";
    private static final int FIELD_HEIGHT = 28;
    private static final int SPINNER_FIELD_WIDTH = 118;
    private static final int SEND_EDITOR_MIN_HEIGHT = 190;
    private static final int SEND_EDITOR_PREFERRED_HEIGHT = 230;

    static {
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping(POSTMAN_JS_SYNTAX, PostmanJavaScriptTokenMaker.class.getName());
        FoldParserManager.get().addFoldParserMapping(POSTMAN_JS_SYNTAX, new CurlyFoldParser());
    }

    public enum Stage {
        CONNECT,
        SEND,
        AWAIT,
        CLOSE
    }

    private final Stage stage;
    private final EasyJSpinner connectTimeoutSpinner;
    private final EasyComboBox<WebSocketPerformanceData.SendMode> sendModeBox;
    private final EasyComboBox<WebSocketPerformanceData.SendContentSource> sendContentSourceBox;
    private final EasyJSpinner sendCountSpinner;
    private final EasyJSpinner sendIntervalSpinner;
    private final JComboBox<WebSocketPerformanceData.CompletionMode> completionModeBox;
    private final EasyJSpinner awaitTimeoutSpinner;
    private final EasyJSpinner holdConnectionSpinner;
    private final EasyJSpinner targetMessageCountSpinner;
    private final EasyTextField messageFilterField;
    private final RSyntaxTextArea customSendBodyArea;
    private final RSyntaxTextArea sendPreScriptArea;
    private final transient AutoCompletion sendPreScriptAc;
    private final JTabbedPane sendEditorTabs;
    private final JPanel messageTemplatePanel;
    private final CardLayout messageTemplateLayout;
    private final JPanel sendEditorActionsPanel;
    private final SnippetButton sendScriptSnippetButton;
    private final IndicatorTabComponent messageTemplateTab;
    private final IndicatorTabComponent sendPreScriptTab;
    private JPanel sendContentSourceField;
    private JPanel sendCountField;
    private JPanel sendIntervalField;
    private JLabel sendCountLabel;
    private JLabel sendIntervalLabel;
    private JLabel sendContentSourceLabel;
    private JLabel awaitTimeoutLabel;
    private JLabel holdConnectionLabel;
    private JLabel targetMessageCountLabel;
    private JLabel sendHintLabel;
    private JTextArea awaitHintArea;
    private JLabel closeHintLabel;
    private JMeterTreeNode currentNode;

    public WebSocketStagePropertyPanel(Stage stage) {
        this.stage = stage;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        connectTimeoutSpinner = new EasyJSpinner(new SpinnerNumberModel(10000, 100, 600000, 100));
        sendModeBox = new EasyComboBox<>(
                WebSocketPerformanceData.SendMode.values(),
                EasyComboBox.WidthMode.FIXED_MAX
        );
        sendContentSourceBox = new EasyComboBox<>(
                WebSocketPerformanceData.SendContentSource.values(),
                EasyComboBox.WidthMode.FIXED_MAX
        );
        sendCountSpinner = new EasyJSpinner(new SpinnerNumberModel(3, 1, 100000, 1));
        sendIntervalSpinner = new EasyJSpinner(new SpinnerNumberModel(1000, 0, 3600000, 100));
        completionModeBox = new JComboBox<>(WebSocketPerformanceData.CompletionMode.values());
        awaitTimeoutSpinner = new EasyJSpinner(new SpinnerNumberModel(10000, 100, 600000, 100));
        holdConnectionSpinner = new EasyJSpinner(new SpinnerNumberModel(30000, 100, 3600000, 1000));
        targetMessageCountSpinner = new EasyJSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        messageFilterField = new EasyTextField(20);
        customSendBodyArea = new RSyntaxTextArea(8, 40);
        configureTemplateEditor(customSendBodyArea);
        sendPreScriptArea = new RSyntaxTextArea(8, 40);
        sendPreScriptAc = configureScriptEditor(sendPreScriptArea);
        messageTemplateLayout = new CardLayout();
        messageTemplatePanel = new JPanel(messageTemplateLayout);
        sendEditorTabs = new JTabbedPane(SwingConstants.LEFT);
        messageTemplateTab = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_TAB_MESSAGE_TEMPLATE));
        sendPreScriptTab = new IndicatorTabComponent(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_TAB_PRE_SCRIPT));
        sendScriptSnippetButton = new SnippetButton();
        sendEditorActionsPanel = createSendEditorActionsPanel();
        buildSendEditorTabs();
        addEditorIndicatorListeners();

        for (EasyJSpinner spinner : getAllSpinners()) {
            spinner.setPreferredSize(new Dimension(100, FIELD_HEIGHT));
        }
        configureFieldWidth(sendCountSpinner, SPINNER_FIELD_WIDTH, SPINNER_FIELD_WIDTH);
        configureFieldWidth(sendIntervalSpinner, SPINNER_FIELD_WIDTH, SPINNER_FIELD_WIDTH);

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
        sendContentSourceBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object displayValue = value;
                if (value instanceof WebSocketPerformanceData.SendContentSource source) {
                    displayValue = switch (source) {
                        case REQUEST_BODY -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_CONTENT_REQUEST_BODY);
                        case CUSTOM_TEXT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_CONTENT_CUSTOM_TEXT);
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
        sendContentSourceBox.addActionListener(e -> updateSendModeState());
        completionModeBox.addActionListener(e -> updateAwaitModeState());
        sendEditorTabs.addChangeListener(e -> sendScriptSnippetButton.setVisible(sendEditorTabs.getSelectedIndex() == 1));
        updateSendModeState();
        updateAwaitModeState();
    }

    private void buildConnectPanel(GridBagConstraints gbc) {
        addFormRow(gbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_CONNECT_TIMEOUT)),
                connectTimeoutSpinner);
    }

    private void buildSendPanel(GridBagConstraints gbc) {
        addSendSettingsRow(gbc);
        addEditorTabsRow(gbc, sendEditorTabs);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        sendHintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_HINT));
        sendHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(sendHintLabel, gbc);
    }

    private void addSendSettingsRow(GridBagConstraints gbc) {
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setOpaque(false);
        GridBagConstraints row = new GridBagConstraints();
        row.insets = new Insets(0, 0, 0, 12);
        row.gridy = 0;
        row.fill = GridBagConstraints.HORIZONTAL;
        row.anchor = GridBagConstraints.NORTHWEST;

        row.gridx = 0;
        row.weightx = 0;
        settingsPanel.add(createFieldBlock(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_MODE),
                sendModeBox
        ), row);

        row.gridx++;
        row.weightx = 0;
        sendContentSourceField = createFieldBlock(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_CONTENT_SOURCE),
                sendContentSourceBox
        );
        sendContentSourceLabel = (JLabel) sendContentSourceField.getClientProperty("fieldLabel");
        settingsPanel.add(sendContentSourceField, row);

        row.gridx++;
        row.weightx = 0;
        sendCountField = createFieldBlock(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_COUNT),
                sendCountSpinner
        );
        sendCountLabel = (JLabel) sendCountField.getClientProperty("fieldLabel");
        settingsPanel.add(sendCountField, row);

        row.gridx++;
        row.insets = new Insets(0, 0, 0, 12);
        sendIntervalField = createFieldBlock(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_INTERVAL),
                sendIntervalSpinner
        );
        sendIntervalLabel = (JLabel) sendIntervalField.getClientProperty("fieldLabel");
        settingsPanel.add(sendIntervalField, row);

        row.gridx++;
        row.weightx = 0.3;
        row.insets = new Insets(0, 0, 0, 0);
        row.fill = GridBagConstraints.HORIZONTAL;
        settingsPanel.add(Box.createHorizontalGlue(), row);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(settingsPanel, gbc);
        gbc.gridy++;
    }

    private JPanel createFieldBlock(String labelText, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setForeground(UIManager.getColor("Label.disabledForeground"));
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.putClientProperty("fieldLabel", label);
        int preferredWidth = Math.max(label.getPreferredSize().width, field.getPreferredSize().width);
        int minimumWidth = Math.max(label.getMinimumSize().width, field.getMinimumSize().width);
        int preferredHeight = FIELD_HEIGHT + label.getPreferredSize().height + 3;
        panel.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        panel.setMinimumSize(new Dimension(minimumWidth, preferredHeight));
        return panel;
    }

    private void configureFieldWidth(JComponent component, int preferredWidth, int minimumWidth) {
        Dimension preferredSize = new Dimension(preferredWidth, FIELD_HEIGHT);
        Dimension minimumSize = new Dimension(minimumWidth, FIELD_HEIGHT);
        component.setPreferredSize(preferredSize);
        component.setMinimumSize(minimumSize);
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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        awaitHintArea = new JTextArea(2, 52);
        awaitHintArea.setEditable(false);
        awaitHintArea.setFocusable(false);
        awaitHintArea.setOpaque(false);
        awaitHintArea.setLineWrap(true);
        awaitHintArea.setWrapStyleWord(true);
        awaitHintArea.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(awaitHintArea, gbc);
    }

    private void buildClosePanel(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        closeHintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_CLOSE_HINT));
        closeHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        add(closeHintLabel, gbc);
    }

    private void buildSendEditorTabs() {
        messageTemplatePanel.add(new SearchableTextArea(customSendBodyArea), CUSTOM_BODY_CARD);
        messageTemplatePanel.add(createRequestBodySourcePanel(), REQUEST_BODY_CARD);

        sendEditorTabs.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_TAB_MESSAGE_TEMPLATE), messageTemplatePanel);
        sendEditorTabs.setTabComponentAt(0, messageTemplateTab);
        sendEditorTabs.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_TAB_PRE_SCRIPT), new SearchableTextArea(sendPreScriptArea));
        sendEditorTabs.setTabComponentAt(1, sendPreScriptTab);
    }

    private JPanel createRequestBodySourcePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getBorderLightColor()),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        JTextArea hint = createHintArea(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY_SOURCE_HINT), 3);
        panel.add(hint, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createSendEditorActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        sendScriptSnippetButton.addActionListener(e -> openSendScriptSnippetDialog());
        panel.add(sendScriptSnippetButton);
        return panel;
    }

    private void configureTemplateEditor(RSyntaxTextArea area) {
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        configureBaseEditor(area);
    }

    private AutoCompletion configureScriptEditor(RSyntaxTextArea area) {
        area.setSyntaxEditingStyle(POSTMAN_JS_SYNTAX);
        area.setCodeFoldingEnabled(true);
        area.setAutoIndentEnabled(true);
        area.setBracketMatchingEnabled(true);
        area.setMarkOccurrences(true);
        configureBaseEditor(area);
        return addAutoCompletion(area);
    }

    private void configureBaseEditor(RSyntaxTextArea area) {
        area.setAntiAliasingEnabled(true);
        area.setPaintTabLines(true);
        area.setTabSize(4);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretColor(ModernColors.PRIMARY);
        EditorThemeUtil.loadTheme(area);
        area.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
    }

    private JTextArea createHintArea(String text, int rows) {
        JTextArea area = new JTextArea(rows, 52);
        area.setText(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setForeground(UIManager.getColor("Label.disabledForeground"));
        area.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        return area;
    }

    private AutoCompletion addAutoCompletion(RSyntaxTextArea area) {
        var provider = ScriptSnippetManager.createCompletionProvider();
        AutoCompletion ac = new AutoCompletion(provider);
        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationEnabled(true);
        ac.setAutoActivationDelay(200);
        ac.setAutoCompleteSingleChoices(false);
        ac.setShowDescWindow(true);
        ac.setParameterAssistanceEnabled(false);
        ac.install(area);
        return ac;
    }

    private void openSendScriptSnippetDialog() {
        if (sendEditorTabs.getSelectedIndex() != 1) {
            sendEditorTabs.setSelectedIndex(1);
        }
        SnippetDialog dialog = new SnippetDialog();
        dialog.setVisible(true);
        Snippet selected = dialog.getSelectedSnippet();
        if (selected == null) {
            return;
        }

        String selectedText = sendPreScriptArea.getSelectedText();
        String codeToInsert = selected.code;
        int caretPosition = sendPreScriptArea.getCaretPosition();
        try {
            int lineStart = sendPreScriptArea.getLineStartOffsetOfCurrentLine();
            if (caretPosition > lineStart && selectedText == null) {
                String lineText = sendPreScriptArea.getText(lineStart, caretPosition - lineStart);
                if (!lineText.trim().isEmpty()) {
                    codeToInsert = "\n" + codeToInsert;
                }
            }
        } catch (Exception ex) {
            log.error("Error calculating WebSocket send script insert position", ex);
        }
        sendPreScriptArea.replaceSelection(codeToInsert);
        sendPreScriptArea.requestFocusInWindow();
    }

    private void setEditorText(RSyntaxTextArea area, AutoCompletion ac, String text) {
        if (ac != null) {
            ac.setAutoActivationEnabled(false);
        }
        area.setText(text);
        area.setCaretPosition(0);
        if (ac != null) {
            ac.setAutoActivationEnabled(true);
        }
    }

    private void addEditorIndicatorListeners() {
        customSendBodyArea.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateEditorIndicators));
        sendPreScriptArea.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateEditorIndicators));
    }

    private void updateEditorIndicators() {
        WebSocketPerformanceData.SendContentSource contentSource =
                (WebSocketPerformanceData.SendContentSource) sendContentSourceBox.getSelectedItem();
        boolean customBodyActive = contentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT;
        messageTemplateTab.setShowIndicator(customBodyActive && hasText(customSendBodyArea));
        sendPreScriptTab.setShowIndicator(hasText(sendPreScriptArea));
    }

    private boolean hasText(JTextArea area) {
        return area.getText() != null && !area.getText().trim().isEmpty();
    }

    private static final class SimpleDocumentListener implements DocumentListener {
        private final Runnable action;

        private SimpleDocumentListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            action.run();
        }
    }

    public void setNode(JMeterTreeNode node) {
        this.currentNode = node;
        WebSocketPerformanceData data = node != null && node.webSocketPerformanceData != null
                ? node.webSocketPerformanceData
                : new WebSocketPerformanceData();
        connectTimeoutSpinner.setValue(data.connectTimeoutMs);
        sendModeBox.setSelectedItem(data.sendMode);
        sendContentSourceBox.setSelectedItem(data.sendContentSource);
        sendCountSpinner.setValue(data.sendCount);
        sendIntervalSpinner.setValue(data.sendIntervalMs);
        completionModeBox.setSelectedItem(data.completionMode);
        awaitTimeoutSpinner.setValue(data.firstMessageTimeoutMs);
        holdConnectionSpinner.setValue(data.holdConnectionMs);
        targetMessageCountSpinner.setValue(data.targetMessageCount);
        messageFilterField.setText(data.messageFilter == null ? "" : data.messageFilter);
        setEditorText(customSendBodyArea, null, data.customSendBody == null ? "" : data.customSendBody);
        setEditorText(sendPreScriptArea, sendPreScriptAc, data.sendPreScript == null ? "" : data.sendPreScript);
        boolean hasActiveCustomBody = data.sendContentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT
                && hasText(customSendBodyArea);
        sendEditorTabs.setSelectedIndex(!hasActiveCustomBody && hasText(sendPreScriptArea) ? 1 : 0);
        updateSendModeState();
        updateAwaitModeState();
        updateEditorIndicators();
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
                data.sendContentSource = (WebSocketPerformanceData.SendContentSource) sendContentSourceBox.getSelectedItem();
                data.customSendBody = customSendBodyArea.getText();
                data.sendPreScript = sendPreScriptArea.getText();
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
        WebSocketPerformanceData.SendContentSource contentSource =
                (WebSocketPerformanceData.SendContentSource) sendContentSourceBox.getSelectedItem();
        boolean showContentFields = mode != WebSocketPerformanceData.SendMode.NONE;
        boolean showRepeatFields = mode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT;
        boolean showCustomContent = showContentFields && contentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT;
        sendContentSourceLabel.setVisible(showContentFields);
        sendContentSourceBox.setVisible(showContentFields);
        sendCountLabel.setVisible(showRepeatFields);
        sendCountSpinner.setVisible(showRepeatFields);
        sendIntervalLabel.setVisible(showRepeatFields);
        sendIntervalSpinner.setVisible(showRepeatFields);
        sendContentSourceField.setVisible(showContentFields);
        sendCountField.setVisible(showRepeatFields);
        sendIntervalField.setVisible(showRepeatFields);
        sendEditorTabs.setVisible(showContentFields);
        sendEditorActionsPanel.setVisible(showContentFields);
        messageTemplateLayout.show(messageTemplatePanel, showCustomContent ? CUSTOM_BODY_CARD : REQUEST_BODY_CARD);
        sendScriptSnippetButton.setVisible(sendEditorTabs.getSelectedIndex() == 1);
        sendHintLabel.setVisible(mode == WebSocketPerformanceData.SendMode.REQUEST_BODY_ON_CONNECT
                || mode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT);
        updateEditorIndicators();
        revalidate();
        repaint();
    }

    private void updateAwaitModeState() {
        if (stage != Stage.AWAIT || awaitTimeoutLabel == null || holdConnectionLabel == null
                || targetMessageCountLabel == null || awaitHintArea == null) {
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
        awaitHintArea.setText(resolveAwaitModeHint(mode));
        revalidate();
        repaint();
    }

    static String resolveAwaitModeHint(WebSocketPerformanceData.CompletionMode mode) {
        if (mode == null) {
            mode = WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE;
        }
        return switch (mode) {
            case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_HINT_FIRST_MESSAGE);
            case MATCHED_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_HINT_MATCHED_MESSAGE);
            case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_HINT_FIXED_DURATION);
            case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_HINT_MESSAGE_COUNT);
        };
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

    private void addEditorTabsRow(GridBagConstraints gbc, JComponent editorTabs) {
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel editorPanel = new JPanel(new BorderLayout(0, 4));
        editorPanel.add(editorTabs, BorderLayout.CENTER);
        editorPanel.add(sendEditorActionsPanel, BorderLayout.SOUTH);
        editorPanel.setMinimumSize(new Dimension(320, SEND_EDITOR_MIN_HEIGHT));
        editorPanel.setPreferredSize(new Dimension(640, SEND_EDITOR_PREFERRED_HEIGHT));
        editorTabs.setMinimumSize(new Dimension(320, SEND_EDITOR_MIN_HEIGHT));
        editorTabs.setPreferredSize(new Dimension(640, SEND_EDITOR_PREFERRED_HEIGHT));
        add(editorPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
    }

    private List<EasyJSpinner> getAllSpinners() {
        return Arrays.asList(connectTimeoutSpinner, sendCountSpinner, sendIntervalSpinner,
                awaitTimeoutSpinner, holdConnectionSpinner, targetMessageCountSpinner);
    }
}
