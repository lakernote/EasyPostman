package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.WebSocketPerformanceData;


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
import com.laker.postman.panel.performance.model.PerformanceTreeNode;
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
import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 生命周期节点属性面板。
 */
@Slf4j
public class WebSocketStagePropertyPanel extends JPanel {
    private static final String POSTMAN_JS_SYNTAX = "text/postman-javascript";
    private static final String CUSTOM_BODY_CARD = "customBody";
    private static final String REQUEST_BODY_CARD = "requestBody";
    private static final int SEND_EDITOR_MIN_HEIGHT = 190;
    private static final int SEND_EDITOR_PREFERRED_HEIGHT = 230;
    private static volatile boolean scriptEditorSyntaxRegistered;

    public enum Stage {
        CONNECT,
        SEND,
        READ,
        CLOSE
    }

    private final Stage stage;
    private EasyJSpinner connectTimeoutSpinner;
    private EasyComboBox<WebSocketPerformanceData.SendMode> sendModeBox;
    private EasyComboBox<WebSocketPerformanceData.SendContentSource> sendContentSourceBox;
    private EasyJSpinner sendCountSpinner;
    private EasyJSpinner sendIntervalSpinner;
    private EasyComboBox<WebSocketPerformanceData.CompletionMode> completionModeBox;
    private EasyJSpinner readTimeoutSpinner;
    private EasyJSpinner holdConnectionSpinner;
    private EasyJSpinner targetMessageCountSpinner;
    private EasyTextField messageFilterField;
    private RSyntaxTextArea customSendBodyArea;
    private RSyntaxTextArea sendPreScriptArea;
    private transient AutoCompletion sendPreScriptAc;
    private JTabbedPane sendEditorTabs;
    private JPanel messageTemplatePanel;
    private CardLayout messageTemplateLayout;
    private JPanel sendEditorActionsPanel;
    private SnippetButton sendScriptSnippetButton;
    private IndicatorTabComponent messageTemplateTab;
    private IndicatorTabComponent sendPreScriptTab;
    private JPanel sendContentSourceField;
    private JPanel sendCountField;
    private JPanel sendIntervalField;
    private JLabel sendCountLabel;
    private JLabel sendIntervalLabel;
    private JLabel sendContentSourceLabel;
    private JLabel readTimeoutLabel;
    private JLabel holdConnectionLabel;
    private JLabel targetMessageCountLabel;
    private JLabel messageFilterLabel;
    private JLabel sendHintLabel;
    private JTextArea readHintArea;
    private JLabel closeHintLabel;
    private PerformanceTreeNode currentNode;
    private boolean initialized;

    public WebSocketStagePropertyPanel(Stage stage) {
        this.stage = stage;
        setLayout(new GridBagLayout());
        PerformanceStagePropertyLayout.applyCompactBorder(this);
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        GridBagConstraints gbc = PerformanceStagePropertyLayout.createBaseConstraints();

        if (stage == Stage.CONNECT) {
            connectTimeoutSpinner = EasyJSpinner.intSpinner(10000, 100, 600000, 100);
        } else if (stage == Stage.SEND) {
            initSendControls();
        } else if (stage == Stage.READ) {
            initReadControls();
        }

        for (EasyJSpinner spinner : getAllSpinners()) {
            PerformanceStagePropertyLayout.configureFieldWidth(spinner,
                    PerformanceStagePropertyLayout.SPINNER_FIELD_WIDTH,
                    PerformanceStagePropertyLayout.SPINNER_FIELD_WIDTH);
        }
        if (messageFilterField != null) {
            PerformanceStagePropertyLayout.configureFieldWidth(messageFilterField,
                    PerformanceStagePropertyLayout.TEXT_FIELD_WIDTH,
                    PerformanceStagePropertyLayout.TEXT_FIELD_WIDTH);
        }

        switch (stage) {
            case CONNECT -> buildConnectPanel(gbc);
            case SEND -> buildSendPanel(gbc);
            case READ -> buildReadPanel(gbc);
            case CLOSE -> buildClosePanel(gbc);
        }
        if (stage == Stage.CONNECT || stage == Stage.READ) {
            PerformanceStagePropertyLayout.addVerticalFiller(this, gbc, 2);
        }

        if (stage == Stage.SEND) {
            sendModeBox.addActionListener(e -> updateSendModeState());
            sendContentSourceBox.addActionListener(e -> updateSendModeState());
            sendEditorTabs.addChangeListener(e -> sendScriptSnippetButton.setVisible(sendEditorTabs.getSelectedIndex() == 1));
        } else if (stage == Stage.READ) {
            completionModeBox.addActionListener(e -> updateReadModeState());
        }
        updateSendModeState();
        updateReadModeState();
        initialized = true;
    }

    private void initSendControls() {
        sendModeBox = new EasyComboBox<>(
                WebSocketPerformanceData.SendMode.values(),
                EasyComboBox.WidthMode.FIXED_MAX
        );
        sendContentSourceBox = new EasyComboBox<>(
                WebSocketPerformanceData.SendContentSource.values(),
                EasyComboBox.WidthMode.FIXED_MAX
        );
        sendCountSpinner = EasyJSpinner.intSpinner(3, 1, 100000, 1);
        sendIntervalSpinner = EasyJSpinner.intSpinner(1000, 0, 3600000, 100);
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
        installSendRenderers();
    }

    private void initReadControls() {
        completionModeBox = new EasyComboBox<>(
                WebSocketPerformanceData.CompletionMode.values(),
                EasyComboBox.WidthMode.FIXED_MAX
        );
        readTimeoutSpinner = EasyJSpinner.intSpinner(10000, 100, 600000, 100);
        holdConnectionSpinner = EasyJSpinner.intSpinner(30000, 100, 3600000, 1000);
        targetMessageCountSpinner = EasyJSpinner.intSpinner(1, 1, 100000, 1);
        messageFilterField = new EasyTextField(20);
        installReadRenderer();
    }

    private void installSendRenderers() {
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
    }

    private void installReadRenderer() {
        completionModeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Object displayValue = value;
                if (value instanceof WebSocketPerformanceData.CompletionMode mode) {
                    displayValue = switch (mode) {
                        case SINGLE_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE);
                        case UNTIL_MATCH -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MATCHED_MESSAGE);
                        case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIXED_DURATION);
                        case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MESSAGE_COUNT);
                    };
                }
                return super.getListCellRendererComponent(list, displayValue, index, isSelected, cellHasFocus);
            }
        });
    }

    private void buildConnectPanel(GridBagConstraints gbc) {
        PerformanceStagePropertyLayout.addCenteredCompactFormRow(this, gbc,
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
        int preferredHeight = PerformanceStagePropertyLayout.FIELD_HEIGHT + label.getPreferredSize().height + 3;
        panel.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        panel.setMinimumSize(new Dimension(minimumWidth, preferredHeight));
        return panel;
    }

    private void buildReadPanel(GridBagConstraints gbc) {
        readTimeoutLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_FIRST_MESSAGE_TIMEOUT));
        holdConnectionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_OBSERVE_DURATION));
        targetMessageCountLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_TARGET_MESSAGE_COUNT));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints rowGbc = new GridBagConstraints();
        rowGbc.insets = new Insets(3, 6, 3, 6);
        rowGbc.anchor = GridBagConstraints.WEST;
        rowGbc.gridx = 0;
        rowGbc.gridy = 0;

        addCompactFormRow(formPanel, rowGbc,
                new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_READ_MODE)),
                completionModeBox);
        messageFilterLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_MESSAGE_FILTER));
        addCompactFormRow(formPanel, rowGbc, messageFilterLabel, messageFilterField);
        addCompactFormRow(formPanel, rowGbc, readTimeoutLabel, readTimeoutSpinner);
        addCompactFormRow(formPanel, rowGbc, holdConnectionLabel, holdConnectionSpinner);
        addCompactFormRow(formPanel, rowGbc, targetMessageCountLabel, targetMessageCountSpinner);

        rowGbc.gridx = 0;
        rowGbc.gridwidth = 2;
        rowGbc.weightx = 0;
        rowGbc.fill = GridBagConstraints.HORIZONTAL;
        readHintArea = new JTextArea(2, 42);
        readHintArea.setEditable(false);
        readHintArea.setFocusable(false);
        readHintArea.setOpaque(false);
        readHintArea.setLineWrap(true);
        readHintArea.setWrapStyleWord(true);
        readHintArea.setForeground(UIManager.getColor("Label.disabledForeground"));
        formPanel.add(readHintArea, rowGbc);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTH;
        add(formPanel, gbc);
        gbc.gridy++;
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
        ensureScriptEditorSyntaxRegistered();
        area.setSyntaxEditingStyle(POSTMAN_JS_SYNTAX);
        area.setCodeFoldingEnabled(true);
        area.setAutoIndentEnabled(true);
        area.setBracketMatchingEnabled(true);
        area.setMarkOccurrences(true);
        configureBaseEditor(area);
        return addAutoCompletion(area);
    }

    private static void ensureScriptEditorSyntaxRegistered() {
        if (scriptEditorSyntaxRegistered) {
            return;
        }
        synchronized (WebSocketStagePropertyPanel.class) {
            if (scriptEditorSyntaxRegistered) {
                return;
            }
            AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
            atmf.putMapping(POSTMAN_JS_SYNTAX, PostmanJavaScriptTokenMaker.class.getName());
            FoldParserManager.get().addFoldParserMapping(POSTMAN_JS_SYNTAX, new CurlyFoldParser());
            scriptEditorSyntaxRegistered = true;
        }
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

    public void setNode(PerformanceTreeNode node) {
        ensureInitialized();
        this.currentNode = node;
        WebSocketPerformanceData data = node != null && node.webSocketPerformanceData != null
                ? node.webSocketPerformanceData
                : new WebSocketPerformanceData();
        switch (stage) {
            case CONNECT -> connectTimeoutSpinner.setValue(data.connectTimeoutMs);
            case SEND -> {
                sendModeBox.setSelectedItem(data.sendMode);
                sendContentSourceBox.setSelectedItem(data.sendContentSource);
                sendCountSpinner.setValue(data.sendCount);
                sendIntervalSpinner.setValue(data.sendIntervalMs);
                setEditorText(customSendBodyArea, null, data.customSendBody == null ? "" : data.customSendBody);
                setEditorText(sendPreScriptArea, sendPreScriptAc, data.sendPreScript == null ? "" : data.sendPreScript);
                boolean hasActiveCustomBody = data.sendContentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT
                        && hasText(customSendBodyArea);
                sendEditorTabs.setSelectedIndex(!hasActiveCustomBody && hasText(sendPreScriptArea) ? 1 : 0);
                updateSendModeState();
                updateEditorIndicators();
            }
            case READ -> {
                completionModeBox.setSelectedItem(data.completionMode == null
                        ? WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE
                        : data.completionMode);
                readTimeoutSpinner.setValue(data.firstMessageTimeoutMs);
                holdConnectionSpinner.setValue(data.holdConnectionMs);
                targetMessageCountSpinner.setValue(data.targetMessageCount);
                messageFilterField.setText(data.messageFilter == null ? "" : data.messageFilter);
                updateReadModeState();
            }
            case CLOSE -> {
                // Close step has no persisted editable fields.
            }
        }
    }

    public void saveData() {
        if (currentNode == null || !initialized) {
            return;
        }
        forceCommitAllSpinners();
        WebSocketPerformanceData data = currentNode.webSocketPerformanceData != null
                ? currentNode.webSocketPerformanceData
                : new WebSocketPerformanceData();
        switch (stage) {
            case CONNECT -> data.connectTimeoutMs = connectTimeoutSpinner.getCommittedIntValue();
            case SEND -> {
                data.sendMode = (WebSocketPerformanceData.SendMode) sendModeBox.getSelectedItem();
                data.sendContentSource = (WebSocketPerformanceData.SendContentSource) sendContentSourceBox.getSelectedItem();
                data.customSendBody = customSendBodyArea.getText();
                data.sendPreScript = sendPreScriptArea.getText();
                data.sendCount = sendCountSpinner.getCommittedIntValue();
                data.sendIntervalMs = sendIntervalSpinner.getCommittedIntValue();
            }
            case READ -> {
                data.completionMode = completionModeBox.getSelectedItem() == null
                        ? WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE
                        : (WebSocketPerformanceData.CompletionMode) completionModeBox.getSelectedItem();
                data.firstMessageTimeoutMs = readTimeoutSpinner.getCommittedIntValue();
                data.holdConnectionMs = holdConnectionSpinner.getCommittedIntValue();
                data.targetMessageCount = targetMessageCountSpinner.getCommittedIntValue();
                data.messageFilter = messageFilterField.getText().trim();
            }
            case CLOSE -> {
                // No editable fields for close step.
            }
        }
        currentNode.webSocketPerformanceData = data;
    }

    public void forceCommitAllSpinners() {
        if (!initialized) {
            return;
        }
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

    private void updateReadModeState() {
        if (stage != Stage.READ || messageFilterLabel == null || readTimeoutLabel == null
                || holdConnectionLabel == null || targetMessageCountLabel == null || readHintArea == null) {
            return;
        }
        WebSocketPerformanceData.CompletionMode mode =
                (WebSocketPerformanceData.CompletionMode) completionModeBox.getSelectedItem();
        boolean showMessageFilter = WebSocketPerformanceData.usesMessageFilter(mode);
        boolean showReadTimeout = mode == WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE
                || mode == WebSocketPerformanceData.CompletionMode.UNTIL_MATCH
                || mode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT;
        boolean showHoldConnection = mode == WebSocketPerformanceData.CompletionMode.FIXED_DURATION;
        boolean showTargetCount = mode == WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT;

        holdConnectionLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_OBSERVE_DURATION));

        messageFilterLabel.setVisible(showMessageFilter);
        messageFilterField.setVisible(showMessageFilter);
        readTimeoutLabel.setVisible(showReadTimeout);
        readTimeoutSpinner.setVisible(showReadTimeout);
        holdConnectionLabel.setVisible(showHoldConnection);
        holdConnectionSpinner.setVisible(showHoldConnection);
        targetMessageCountLabel.setVisible(showTargetCount);
        targetMessageCountSpinner.setVisible(showTargetCount);
        readHintArea.setText(resolveReadModeHint(mode));
        revalidate();
        repaint();
    }

    static String resolveReadModeHint(WebSocketPerformanceData.CompletionMode mode) {
        if (mode == null) {
            mode = WebSocketPerformanceData.CompletionMode.SINGLE_MESSAGE;
        }
        String modeHint = switch (mode) {
            case SINGLE_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_HINT_FIRST_MESSAGE);
            case UNTIL_MATCH -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_HINT_MATCHED_MESSAGE);
            case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_HINT_FIXED_DURATION);
            case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_HINT_MESSAGE_COUNT);
        };
        return modeHint + "\n" + I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_HINT_MEMORY);
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
        List<EasyJSpinner> spinners = new ArrayList<>();
        addSpinner(spinners, connectTimeoutSpinner);
        addSpinner(spinners, sendCountSpinner);
        addSpinner(spinners, sendIntervalSpinner);
        addSpinner(spinners, readTimeoutSpinner);
        addSpinner(spinners, holdConnectionSpinner);
        addSpinner(spinners, targetMessageCountSpinner);
        return spinners;
    }

    private void addSpinner(List<EasyJSpinner> spinners, EasyJSpinner spinner) {
        if (spinner != null) {
            spinners.add(spinner);
        }
    }
}
