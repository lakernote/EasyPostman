package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.variable.VariableParser;
import com.laker.postman.variable.VariableSegment;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.ViewportClippedTokenPainter;
import com.laker.postman.common.component.button.*;
import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.service.variable.VariableResolver;
import com.laker.postman.variable.VariableType;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.Caret;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;

/**
 * 请求Body相关的独立面板，支持none、form-data、x-www-form-urlencoded、raw
 */
@Slf4j
public class RequestBodyPanel extends JPanel {
    public static final String BODY_TYPE_NONE = RequestBodyTypes.BODY_TYPE_NONE;
    public static final String BODY_TYPE_FORM_DATA = RequestBodyTypes.BODY_TYPE_FORM_DATA;
    public static final String BODY_TYPE_FORM_URLENCODED = RequestBodyTypes.BODY_TYPE_FORM_URLENCODED;
    public static final String BODY_TYPE_RAW = RequestBodyTypes.BODY_TYPE_RAW;
    public static final String RAW_TYPE_JSON = RequestBodyTypes.RAW_TYPE_JSON;
    public static final String RAW_TYPE_TEXT = RequestBodyTypes.RAW_TYPE_TEXT;
    public static final String RAW_TYPE_XML = RequestBodyTypes.RAW_TYPE_XML;

    @Getter
    private EasyComboBox<String> bodyTypeComboBox;
    @Getter
    private EasyComboBox<String> rawTypeComboBox;
    @Getter
    private FormDataTablePanel formDataTablePanel;
    @Getter
    private FormUrlencodedTablePanel formUrlencodedTablePanel;
    @Getter
    private RSyntaxTextArea bodyArea;
    private CardLayout bodyCardLayout;
    private JPanel bodyCardPanel;
    private String currentBodyType = BODY_TYPE_NONE;
    @Getter
    private WebSocketSendButton wsSendButton;
    private EditButton bulkEditButton;
    private FormatButton formatButton;
    private CompressButton compressButton;
    private RequestBodyBulkEditSupport bulkEditSupport;
    private final boolean isWebSocketMode;

    private Timer wsTimer; // 定时发送用
    private WebSocketTimedSendButton wsTimedSendButton; // 定时发送按钮
    private JTextField wsIntervalField; // 定时间隔输入框
    private JCheckBox wsClearInputCheckBox; // 清空输入复选框
    private SearchableTextArea searchableTextArea; // 集成了搜索功能的文本编辑器（HTTP模式）
    private SearchButton searchButton; // 搜索按钮（HTTP模式）
    private WrapToggleButton wrapButton; // 换行按钮（HTTP模式）
    private boolean editable = true;

    private RequestBodyVariableAutocompleteController autocompleteController;

    @Setter
    private transient ActionListener wsSendActionListener; // 外部注入的发送回调

    public RequestBodyPanel(RequestItemProtocolEnum protocol) {
        this.isWebSocketMode = protocol.isWebSocketProtocol();
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        // 设置边距
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        if (isWebSocketMode) {
            initWebSocketBodyPanel();
        } else {
            initHttpBodyPanel();
        }
    }

    /**
     * 初始化 HTTP 模式下的 Body 面板
     */
    private void initHttpBodyPanel() {
        JPanel bodyTypePanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(bodyTypePanel);
        JPanel topPanel = new JPanel();
        ToolWindowSurfaceStyle.applyCard(topPanel);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 0));

        String[] bodyTypes = new String[]{BODY_TYPE_NONE, BODY_TYPE_FORM_DATA, BODY_TYPE_FORM_URLENCODED, BODY_TYPE_RAW};
        bodyTypeComboBox = new EasyComboBox<>(bodyTypes, EasyComboBox.WidthMode.DYNAMIC);
        bodyTypeComboBox.setSelectedItem(currentBodyType);
        bodyTypeComboBox.addActionListener(e -> switchBodyType((String) bodyTypeComboBox.getSelectedItem()));
        topPanel.add(bodyTypeComboBox);
        topPanel.add(Box.createHorizontalStrut(4));

        bulkEditButton = new EditButton();
        bulkEditButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BULK_EDIT));
        bulkEditButton.addActionListener(e -> showBulkEditDialog());
        topPanel.add(bulkEditButton);
        topPanel.add(Box.createHorizontalStrut(4));

        String[] rawTypes = {RAW_TYPE_JSON, RAW_TYPE_XML, RAW_TYPE_TEXT};
        rawTypeComboBox = new EasyComboBox<>(rawTypes, EasyComboBox.WidthMode.DYNAMIC);
        rawTypeComboBox.setSelectedItem(RAW_TYPE_JSON);
        boolean showFormatControls = isBodyTypeRAW();
        rawTypeComboBox.setVisible(showFormatControls);
        topPanel.add(rawTypeComboBox);
        topPanel.add(Box.createHorizontalStrut(2));

        // 搜索按钮 - 点击弹出 SearchReplacePanel
        searchButton = new SearchButton();
        searchButton.addActionListener(e -> {
            if (searchableTextArea != null) {
                searchableTextArea.getTextArea().requestFocusInWindow();
                searchableTextArea.showSearch();
            }
        });
        topPanel.add(searchButton);
        topPanel.add(Box.createHorizontalStrut(1));

        // 换行按钮
        wrapButton = new WrapToggleButton();
        wrapButton.addActionListener(e -> toggleLineWrap());
        topPanel.add(wrapButton);
        topPanel.add(Box.createHorizontalStrut(1));

        formatButton = new FormatButton();
        formatButton.addActionListener(e -> formatBody());
        formatButton.setVisible(isBodyTypeRAW());
        topPanel.add(formatButton);
        topPanel.add(Box.createHorizontalStrut(1));

        compressButton = new CompressButton();
        compressButton.addActionListener(e -> compressBody());
        compressButton.setVisible(isBodyTypeRAW());
        topPanel.add(compressButton);
        topPanel.add(Box.createHorizontalGlue());

        bodyTypePanel.add(topPanel, BorderLayout.NORTH);

        add(bodyTypePanel, BorderLayout.NORTH);
        bodyCardLayout = new CardLayout();
        bodyCardPanel = new JPanel(bodyCardLayout);
        ToolWindowSurfaceStyle.applyCard(bodyCardPanel);
        bodyCardPanel.add(createNonePanel(), BODY_TYPE_NONE);
        bodyCardPanel.add(createFormDataPanel(), BODY_TYPE_FORM_DATA);
        bodyCardPanel.add(createFormUrlencodedPanel(), BODY_TYPE_FORM_URLENCODED);
        bodyCardPanel.add(createRawPanel(), BODY_TYPE_RAW);
        add(bodyCardPanel, BorderLayout.CENTER);
        bulkEditSupport = new RequestBodyBulkEditSupport(this, formDataTablePanel, formUrlencodedTablePanel);
        bodyCardLayout.show(bodyCardPanel, currentBodyType);
        updateBodyActionVisibility(currentBodyType);
    }

    /**
     * 初始化 WebSocket 模式下的 Body 面板
     */
    private void initWebSocketBodyPanel() {
        JPanel bodyTypePanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(bodyTypePanel);
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setOpaque(false);
        JLabel bodyTypeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_LABEL_SEND_MESSAGE));
        leftPanel.add(bodyTypeLabel);
        String[] bodyTypes = new String[]{BODY_TYPE_RAW};
        bodyTypeComboBox = new EasyComboBox<>(bodyTypes, EasyComboBox.WidthMode.DYNAMIC);
        bodyTypeComboBox.setSelectedItem(BODY_TYPE_RAW);
        bodyTypeComboBox.setVisible(false);
        leftPanel.add(bodyTypeComboBox);
        rawTypeComboBox = null;
        formatButton = null;
        compressButton = null;
        bodyTypePanel.add(leftPanel, BorderLayout.WEST);
        add(bodyTypePanel, BorderLayout.NORTH);
        bodyCardLayout = new CardLayout();
        bodyCardPanel = new JPanel(bodyCardLayout);
        ToolWindowSurfaceStyle.applyCard(bodyCardPanel);
        JPanel rawPanel = createRawPanel();
        bodyCardPanel.add(rawPanel, BODY_TYPE_RAW);
        add(bodyCardPanel, BorderLayout.CENTER);
        bodyCardLayout.show(bodyCardPanel, BODY_TYPE_RAW);
        // WebSocket底部操作按钮
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        ToolWindowSurfaceStyle.applyCard(bottomPanel);
        wsClearInputCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_CHECKBOX_CLEAR));
        bottomPanel.add(wsClearInputCheckBox);
        bottomPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_LABEL_TIMEOUT)));
        wsIntervalField = new JTextField("1000", 5); // 默认1000ms
        bottomPanel.add(wsIntervalField);

        wsTimedSendButton = new WebSocketTimedSendButton();
        wsTimedSendButton.addActionListener(e -> toggleWsTimer());
        bottomPanel.add(wsTimedSendButton);

        wsSendButton = new WebSocketSendButton();
        wsSendButton.addActionListener(e -> wsSendAndMaybeClear());
        bottomPanel.add(wsSendButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private boolean isBodyTypeRAW() {
        return BODY_TYPE_RAW.equals(currentBodyType);
    }

    private JPanel createNonePanel() {
        JPanel nonePanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(nonePanel);
        nonePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_NONE), SwingConstants.CENTER), BorderLayout.CENTER);
        return nonePanel;
    }

    private JPanel createFormDataPanel() {
        formDataTablePanel = new FormDataTablePanel();
        return formDataTablePanel;
    }

    private JPanel createFormUrlencodedPanel() {
        formUrlencodedTablePanel = new FormUrlencodedTablePanel();
        return formUrlencodedTablePanel;
    }

    private JPanel createRawPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(panel);
        bodyArea = new VariableAwareSyntaxTextArea();
        bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS); // 默认JSON高亮
        bodyArea.setCodeFoldingEnabled(true); // 启用代码折叠
        bodyArea.setLineWrap(false); // 禁用自动换行以提升大文本性能
        bodyArea.setShowMatchedBracketPopup(false);
        bodyArea.setTokenPainterFactory(ignored -> new ViewportClippedTokenPainter());

        // 加载编辑器主题 - 支持亮色和暗色主题自适应
        EditorThemeUtil.loadTheme(bodyArea);

        // 设置字体 - 使用用户设置的字体大小（必须在主题应用之后，避免被主题覆盖）
        updateEditorFont();

        // ====== 添加撤回/重做功能 ======
        UndoManager undoManager = new UndoManager();
        bodyArea.getDocument().addUndoableEditListener(undoManager);

        // Undo
        bodyArea.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        bodyArea.getInputMap().put(KeyStroke.getKeyStroke("meta Z"), "Undo"); // macOS Cmd+Z
        bodyArea.getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) undoManager.undo();
                } catch (CannotUndoException ignored) {
                }
            }
        });

        // Redo
        bodyArea.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        bodyArea.getInputMap().put(KeyStroke.getKeyStroke("meta shift Z"), "Redo"); // macOS Cmd+Shift+Z
        bodyArea.getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) undoManager.redo();
                } catch (CannotRedoException ignored) {
                }
            }
        });

        // ====== 添加变量自动补全功能 ======
        autocompleteController = new RequestBodyVariableAutocompleteController(this, bodyArea);
        autocompleteController.install();

        // 使用 SearchableTextArea 包装 bodyArea，集成搜索替换功能
        searchableTextArea = new SearchableTextArea(bodyArea);
        panel.add(searchableTextArea, BorderLayout.CENTER);

        // ====== 变量高亮和悬浮提示 ======
        bodyArea.getDocument().addDocumentListener(new DocumentListener() {
            void repaintBadges() {
                if (bodyArea != null) {
                    bodyArea.repaint();
                }
            }

            public void insertUpdate(DocumentEvent e) {
                repaintBadges();
            }

            public void removeUpdate(DocumentEvent e) {
                repaintBadges();
            }

            public void changedUpdate(DocumentEvent e) {
                repaintBadges();
            }
        });
        SwingUtilities.invokeLater(bodyArea::repaint);
        // 悬浮提示
        bodyArea.addMouseMotionListener(new MouseInputAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int pos = bodyArea.viewToModel2D(e.getPoint());
                String text = bodyArea.getText();
                java.util.List<VariableSegment> segments = VariableParser.getVariableSegments(text);
                for (VariableSegment seg : segments) {
                    if (pos >= seg.start && pos <= seg.end) {
                        String varName = seg.name;

                        // 获取变量类型和值
                        VariableType varType = VariableResolver.getVariableType(varName);
                        String varValue = VariableResolver.resolveVariable(varName);

                        if (varType != null && varValue != null) {
                            // 变量已定义
                            bodyArea.setToolTipText(RequestBodyVariableTooltipBuilder.variableTooltip(varName, varValue, varType));
                        } else {
                            // 变量未定义
                            bodyArea.setToolTipText(RequestBodyVariableTooltipBuilder.undefinedVariableTooltip(varName));
                        }
                        return;
                    }
                }
                bodyArea.setToolTipText(null);
            }
        });
        // 监听 rawTypeComboBox 选项变化，切换高亮风格
        if (rawTypeComboBox != null) {
            rawTypeComboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selected = (String) e.getItem();
                    switch (selected) {
                        case RAW_TYPE_JSON:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
                            break;
                        case RAW_TYPE_XML:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                            break;
                        case RAW_TYPE_TEXT:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                            break;
                        default:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                    }
                }
            });
        }
        return panel;
    }

    private class VariableAwareSyntaxTextArea extends RSyntaxTextArea {
        private VariableAwareSyntaxTextArea() {
            super(5, 20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            String text = getText();
            for (VariableSegment seg : VariableParser.getVariableSegments(text)) {
                boolean isDefined = VariableResolver.isVariableDefined(seg.name);
                Color fillColor = isDefined ? RequestBodyTheme.definedVariableHighlight() : RequestBodyTheme.undefinedVariableHighlight();
                Color borderColor = isDefined ? RequestBodyTheme.definedVariableBorder() : RequestBodyTheme.undefinedVariableBorder();
                paintVariableBadge((Graphics2D) g, seg, text, fillColor, borderColor);
            }

            paintCaretAboveBadges(g);
        }

        private void paintCaretAboveBadges(Graphics g) {
            Caret caret = getCaret();
            if (caret == null || !hasFocus() || !isEnabled()) {
                return;
            }
            caret.paint(g);
        }

        private void paintVariableBadge(Graphics2D g, VariableSegment seg, String text, Color fillColor, Color borderColor) {
            try {
                Rectangle startRect = modelToView2D(seg.start).getBounds();
                Rectangle endRect = modelToView2D(Math.max(seg.start, seg.end - 1)).getBounds();
                if (startRect == null || endRect == null || startRect.y != endRect.y) {
                    return;
                }

                int x = startRect.x;
                int width = Math.max(10, endRect.x + endRect.width - startRect.x);
                int y = startRect.y + 1;
                int height = Math.max(14, startRect.height - 2);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try {
                    Stroke oldStroke = g2.getStroke();
                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x, y, width, height, 10, 10);
                    g2.setStroke(oldStroke);
                } finally {
                    g2.dispose();
                }
            } catch (Exception ex) {
                log.debug("paintVariableBadge failed", ex);
            }
        }
    }

    // WebSocket发送并根据checkbox清空输入
    private void wsSendAndMaybeClear() {
        if (wsSendActionListener != null) {
            wsSendActionListener.actionPerformed(new ActionEvent(wsSendButton, ActionEvent.ACTION_PERFORMED, null));
        }
        if (wsClearInputCheckBox != null && wsClearInputCheckBox.isSelected()) {
            bodyArea.setText("");
        }
    }

    // 定时发送逻辑
    private void toggleWsTimer() {
        // 只有已连接WebSocket时才能启动定时器
        if (wsSendButton == null || !wsSendButton.isEnabled()) {
            // 未连接时，直接返回，不允许启动定时器
            return;
        }
        if (wsTimer != null && wsTimer.isRunning()) {
            wsTimer.stop();
            wsTimedSendButton.setText(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_START));
            wsIntervalField.setEnabled(true);
            wsClearInputCheckBox.setEnabled(true);
            wsSendButton.setEnabled(true);
        } else {
            int interval = 1000;
            try {
                interval = Integer.parseInt(wsIntervalField.getText().trim());
                if (interval < 100) interval = 100; // 最小100ms
            } catch (Exception ignored) {
            }
            wsTimer = new Timer(interval, e -> {
                if (wsSendButton.isEnabled()) {
                    wsSendAndMaybeClear();
                }
            });
            wsTimer.start();
            wsTimedSendButton.setText(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_STOP));
            wsIntervalField.setEnabled(false);
            wsClearInputCheckBox.setEnabled(false);
            wsSendButton.setEnabled(true);
        }
    }

    /**
     * WebSocket连接状态变化时调用，控制发送和定时按钮的可用性
     *
     * @param connected 是否已连接
     */
    public void setWebSocketConnected(boolean connected) {
        if (wsSendButton != null) wsSendButton.setEnabled(connected);
        if (wsTimedSendButton != null) wsTimedSendButton.setEnabled(connected);
    }

    private void switchBodyType(String bodyType) {
        currentBodyType = bodyType;
        bodyCardLayout.show(bodyCardPanel, bodyType);
        updateBodyActionVisibility(bodyType);
    }

    private void updateBodyActionVisibility(String bodyType) {
        if (isWebSocketMode) {
            return;
        }
        boolean isRaw = BODY_TYPE_RAW.equals(bodyType);
        boolean isBulkEditable = BODY_TYPE_FORM_DATA.equals(bodyType) || BODY_TYPE_FORM_URLENCODED.equals(bodyType);
        if (rawTypeComboBox != null) {
            rawTypeComboBox.setVisible(isRaw);
        }
        if (formatButton != null) {
            formatButton.setVisible(editable && isRaw);
        }
        if (compressButton != null) {
            compressButton.setVisible(editable && isRaw);
        }
        if (searchButton != null) {
            searchButton.setVisible(isRaw);
        }
        if (wrapButton != null) {
            wrapButton.setVisible(isRaw);
        }
        if (bulkEditButton != null) {
            bulkEditButton.setVisible(editable && isBulkEditable);
        }
    }

    private void showBulkEditDialog() {
        if (bulkEditSupport != null) {
            bulkEditSupport.showBulkEditDialog(currentBodyType);
        }
    }

    private void formatBody() {
        if (!isBodyTypeRAW()) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_ONLY_RAW));
            return;
        }
        String bodyText = bodyArea.getText();
        if (CharSequenceUtil.isBlank(bodyText)) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_EMPTY));
            return;
        }
        String selectedFormat = (String) rawTypeComboBox.getSelectedItem();
        Optional<String> formatted = formatBodyTextForDisplay(selectedFormat, bodyText);
        if (formatted.isPresent()) {
            bodyArea.setText(formatted.get());
            return;
        }

        JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_INVALID));
    }

    static Optional<String> formatBodyTextForDisplay(String selectedFormat, String bodyText) {
        if (RAW_TYPE_JSON.equals(selectedFormat)) {
            if (!JsonUtil.isTypeJSON(bodyText)) {
                return Optional.empty();
            }
            try {
                return Optional.of(JsonUtil.toJsonPrettyStr(bodyText));
            } catch (RuntimeException ex) {
                log.debug("JSON body format failed", ex);
                return Optional.empty();
            }
        }
        if (RAW_TYPE_XML.equals(selectedFormat)) {
            return Optional.of(XmlUtil.formatXml(bodyText));
        }
        log.debug("Unsupported format type or content is not JSON/XML");
        return Optional.empty();
    }

    private void compressBody() {
        if (!isBodyTypeRAW()) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_COMPRESS_ONLY_RAW));
            return;
        }
        String bodyText = bodyArea.getText();
        if (CharSequenceUtil.isBlank(bodyText)) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_COMPRESS_EMPTY));
            return;
        }
        String selectedFormat = (String) rawTypeComboBox.getSelectedItem();
        Optional<String> compressed = compressBodyTextForDisplay(selectedFormat, bodyText);
        if (compressed.isPresent()) {
            bodyArea.setText(compressed.get());
            return;
        }

        JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_COMPRESS_INVALID));
    }

    static Optional<String> compressBodyTextForDisplay(String selectedFormat, String bodyText) {
        if (!RAW_TYPE_JSON.equals(selectedFormat)) {
            log.debug("Unsupported compress type or content is not JSON");
            return Optional.empty();
        }
        if (!JsonUtil.isTypeJSON(bodyText)) {
            return Optional.empty();
        }
        try {
            return Optional.of(JsonUtil.cleanJsonComments(bodyText));
        } catch (RuntimeException ex) {
            log.debug("JSON body compress failed", ex);
            return Optional.empty();
        }
    }

    /**
     * 切换自动换行状态
     */
    private void toggleLineWrap() {
        if (bodyArea != null && wrapButton != null) {
            boolean isWrapEnabled = wrapButton.isSelected();
            bodyArea.setLineWrap(isWrapEnabled);
        }
    }


    // getter方法，供主面板调用
    public String getBodyType() {
        return currentBodyType;
    }

    public String getRawBody() {
        return bodyArea != null ? bodyArea.getText() : null;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        if (bodyTypeComboBox != null) {
            bodyTypeComboBox.setEnabled(editable);
        }
        if (rawTypeComboBox != null) {
            rawTypeComboBox.setEnabled(editable);
        }
        if (formDataTablePanel != null) {
            formDataTablePanel.setEditable(editable);
        }
        if (formUrlencodedTablePanel != null) {
            formUrlencodedTablePanel.setEditable(editable);
        }
        if (bodyArea != null) {
            bodyArea.setEditable(editable);
        }
        if (bulkEditButton != null) {
            bulkEditButton.setVisible(editable && (BODY_TYPE_FORM_DATA.equals(currentBodyType)
                    || BODY_TYPE_FORM_URLENCODED.equals(currentBodyType)));
            bulkEditButton.setEnabled(editable);
        }
        if (formatButton != null) {
            formatButton.setVisible(editable && isBodyTypeRAW());
            formatButton.setEnabled(editable);
        }
        if (compressButton != null) {
            compressButton.setVisible(editable && isBodyTypeRAW());
            compressButton.setEnabled(editable);
        }
        if (wsClearInputCheckBox != null) {
            wsClearInputCheckBox.setEnabled(editable);
        }
        if (wsIntervalField != null) {
            wsIntervalField.setEditable(editable);
            wsIntervalField.setEnabled(editable);
        }
        if (wsTimedSendButton != null) {
            wsTimedSendButton.setVisible(editable);
            wsTimedSendButton.setEnabled(editable);
        }
        if (wsSendButton != null) {
            wsSendButton.setVisible(editable);
            wsSendButton.setEnabled(editable);
        }
    }

    /**
     * 更新编辑器字体
     * 使用用户设置的字体大小
     */
    private void updateEditorFont() {
        if (bodyArea != null) {
            bodyArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        }
    }

}
