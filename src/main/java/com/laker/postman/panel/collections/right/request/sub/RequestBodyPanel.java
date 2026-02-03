package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.button.FormatButton;
import com.laker.postman.common.component.button.SearchButton;
import com.laker.postman.common.component.button.WebSocketSendButton;
import com.laker.postman.common.component.button.WebSocketTimedSendButton;
import com.laker.postman.common.component.table.EasyPostmanFormDataTablePanel;
import com.laker.postman.common.component.table.EasyPostmanFormUrlencodedTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.VariableSegment;
import com.laker.postman.service.VariableResolver;
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
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

/**
 * 请求Body相关的独立面板，支持none、form-data、x-www-form-urlencoded、raw
 */
@Slf4j
public class RequestBodyPanel extends JPanel {
    public static final String BODY_TYPE_NONE = "none";
    public static final String BODY_TYPE_FORM_DATA = "form-data";
    public static final String BODY_TYPE_FORM_URLENCODED = "x-www-form-urlencoded";
    public static final String BODY_TYPE_RAW = "raw";
    public static final String RAW_TYPE_JSON = "JSON";
    public static final String RAW_TYPE_TEXT = "Text";
    public static final String RAW_TYPE_XML = "XML";

    // 自动补全UI颜色 - 与 EasyPostmanTextField 保持一致
    private static final Color BUILTIN_FUNCTION_COLOR = new Color(156, 39, 176); // 紫色 - 内置函数
    private static final Color ENV_VARIABLE_COLOR = new Color(46, 125, 50); // 绿色 - 环境变量
    private static final Color POPUP_BACKGROUND = new Color(255, 255, 255);
    private static final Color POPUP_SELECTION_BG = new Color(232, 242, 252);

    @Getter
    private EasyComboBox<String> bodyTypeComboBox;
    @Getter
    private EasyComboBox<String> rawTypeComboBox;
    @Getter
    private EasyPostmanFormDataTablePanel formDataTablePanel;
    @Getter
    private EasyPostmanFormUrlencodedTablePanel formUrlencodedTablePanel;
    @Getter
    private RSyntaxTextArea bodyArea;
    private CardLayout bodyCardLayout;
    private JPanel bodyCardPanel;
    private String currentBodyType = BODY_TYPE_NONE;
    @Getter
    private WebSocketSendButton wsSendButton;
    private FormatButton formatButton;
    private final boolean isWebSocketMode;

    private Timer wsTimer; // 定时发送用
    private WebSocketTimedSendButton wsTimedSendButton; // 定时发送按钮
    private JTextField wsIntervalField; // 定时间隔输入框
    private JCheckBox wsClearInputCheckBox; // 清空输入复选框
    private SearchableTextArea searchableTextArea; // 集成了搜索功能的文本编辑器（HTTP模式）
    private SearchButton searchButton; // 搜索按钮（HTTP模式）

    // 自动补全相关
    private JWindow autocompleteWindow;
    private JList<String> autocompleteList;
    private DefaultListModel<String> autocompleteModel;
    private Map<String, String> currentVariables;

    @Setter
    private transient ActionListener wsSendActionListener; // 外部注入的发送回调

    /**
     * 获取已定义变量的高亮颜色（主题自适应）
     * 亮模式：浅青色 - 与JSON语法的深红色(163,21,21)形成冷暖对比
     * 暗模式：淡紫色 - 与JSON语法的绿色(105,134,90)形成补色对比
     */
    private static Color getDefinedVariableHighlightColor() {
        if (ModernColors.isDarkTheme()) {
            // 暗色主题：淡紫色背景，与JSON绿色文字形成补色对比
            return new Color(130, 100, 180, 80);
        } else {
            // 亮色主题：浅青色背景，与JSON深红色文字形成冷暖对比
            return new Color(180, 235, 235, 100);
        }
    }

    /**
     * 获取未定义变量的高亮颜色（主题自适应）
     * 亮模式：浅黄色 - 警告色，与JSON语法的深红色(163,21,21)有明显区分
     * 暗模式：浅粉色 - 警告效果，与JSON语法的绿色(105,134,90)有明显区分
     */
    private static Color getUndefinedVariableHighlightColor() {
        if (ModernColors.isDarkTheme()) {
            // 暗色主题：浅粉色背景，警告效果柔和
            return new Color(200, 120, 150, 85);
        } else {
            // 亮色主题：浅黄色背景，温和的警告提示
            return new Color(255, 250, 205, 120);
        }
    }

    public RequestBodyPanel(RequestItemProtocolEnum protocol) {
        this.isWebSocketMode = protocol.isWebSocketProtocol();
        setLayout(new BorderLayout());
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
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 0));

        String[] bodyTypes = new String[]{BODY_TYPE_NONE, BODY_TYPE_FORM_DATA, BODY_TYPE_FORM_URLENCODED, BODY_TYPE_RAW};
        bodyTypeComboBox = new EasyComboBox<>(bodyTypes, EasyComboBox.WidthMode.DYNAMIC);
        bodyTypeComboBox.setSelectedItem(currentBodyType);
        bodyTypeComboBox.addActionListener(e -> switchBodyType((String) bodyTypeComboBox.getSelectedItem()));
        topPanel.add(bodyTypeComboBox);
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

        formatButton = new FormatButton();
        formatButton.addActionListener(e -> formatBody());
        formatButton.setVisible(isBodyTypeRAW());
        topPanel.add(formatButton);
        topPanel.add(Box.createHorizontalGlue());

        bodyTypePanel.add(topPanel, BorderLayout.NORTH);

        add(bodyTypePanel, BorderLayout.NORTH);
        bodyCardLayout = new CardLayout();
        bodyCardPanel = new JPanel(bodyCardLayout);
        bodyCardPanel.add(createNonePanel(), BODY_TYPE_NONE);
        bodyCardPanel.add(createFormDataPanel(), BODY_TYPE_FORM_DATA);
        bodyCardPanel.add(createFormUrlencodedPanel(), BODY_TYPE_FORM_URLENCODED);
        bodyCardPanel.add(createRawPanel(), BODY_TYPE_RAW);
        add(bodyCardPanel, BorderLayout.CENTER);
        bodyCardLayout.show(bodyCardPanel, currentBodyType);

        // 切换body类型时，控制搜索按钮和格式化按钮的显示
        bodyTypeComboBox.addActionListener(e -> {
            boolean isRaw = BODY_TYPE_RAW.equals(bodyTypeComboBox.getSelectedItem());
            rawTypeComboBox.setVisible(isRaw);
            formatButton.setVisible(isRaw);
            searchButton.setVisible(isRaw);
        });
        // 初始化显示状态
        boolean isRaw = BODY_TYPE_RAW.equals(bodyTypeComboBox.getSelectedItem());
        rawTypeComboBox.setVisible(isRaw);
        formatButton.setVisible(isRaw);
        searchButton.setVisible(isRaw);
    }

    /**
     * 初始化 WebSocket 模式下的 Body 面板
     */
    private void initWebSocketBodyPanel() {
        JPanel bodyTypePanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel bodyTypeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_LABEL_SEND_MESSAGE));
        leftPanel.add(bodyTypeLabel);
        String[] bodyTypes = new String[]{BODY_TYPE_RAW};
        bodyTypeComboBox = new EasyComboBox<>(bodyTypes, EasyComboBox.WidthMode.DYNAMIC);
        bodyTypeComboBox.setSelectedItem(BODY_TYPE_RAW);
        bodyTypeComboBox.setVisible(false);
        leftPanel.add(bodyTypeComboBox);
        rawTypeComboBox = null;
        formatButton = null;
        bodyTypePanel.add(leftPanel, BorderLayout.WEST);
        add(bodyTypePanel, BorderLayout.NORTH);
        bodyCardLayout = new CardLayout();
        bodyCardPanel = new JPanel(bodyCardLayout);
        JPanel rawPanel = createRawPanel();
        bodyCardPanel.add(rawPanel, BODY_TYPE_RAW);
        add(bodyCardPanel, BorderLayout.CENTER);
        bodyCardLayout.show(bodyCardPanel, BODY_TYPE_RAW);
        // WebSocket底部操作按钮
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
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
        nonePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_NONE), SwingConstants.CENTER), BorderLayout.CENTER);
        return nonePanel;
    }

    private JPanel createFormDataPanel() {
        formDataTablePanel = new EasyPostmanFormDataTablePanel();
        return formDataTablePanel;
    }

    private JPanel createFormUrlencodedPanel() {
        formUrlencodedTablePanel = new EasyPostmanFormUrlencodedTablePanel();
        return formUrlencodedTablePanel;
    }

    private JPanel createRawPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        bodyArea = new RSyntaxTextArea(5, 20);
        bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS); // 默认JSON高亮
        bodyArea.setCodeFoldingEnabled(true); // 启用代码折叠

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
        initAutocomplete();

        // 使用 SearchableTextArea 包装 bodyArea，集成搜索替换功能
        searchableTextArea = new SearchableTextArea(bodyArea);
        panel.add(searchableTextArea, BorderLayout.CENTER);

        // ====== 变量高亮和悬浮提示 ======
        // 变量高亮 - 使用主题自适应颜色
        DefaultHighlighter highlighter = (DefaultHighlighter) bodyArea.getHighlighter();
        DefaultHighlighter.DefaultHighlightPainter definedPainter = new DefaultHighlighter.DefaultHighlightPainter(getDefinedVariableHighlightColor());
        DefaultHighlighter.DefaultHighlightPainter undefinedPainter = new DefaultHighlighter.DefaultHighlightPainter(getUndefinedVariableHighlightColor());
        bodyArea.getDocument().addDocumentListener(new DocumentListener() {
            void updateHighlights() {
                highlighter.removeAllHighlights();
                String text = bodyArea.getText();
                java.util.List<VariableSegment> segments = VariableUtil.getVariableSegments(text);
                for (VariableSegment seg : segments) {
                    // 判断变量状态：环境变量、临时变量或内置函数 - 与 EasyPostmanTextField 保持一致
                    boolean isDefined = VariableResolver.isVariableDefined(seg.name)
                            || VariableUtil.isBuiltInFunction(seg.name);
                    try {
                        highlighter.addHighlight(seg.start, seg.end, isDefined ? definedPainter : undefinedPainter);
                    } catch (BadLocationException ignored) {
                    }
                }
            }

            public void insertUpdate(DocumentEvent e) {
                updateHighlights();
            }

            public void removeUpdate(DocumentEvent e) {
                updateHighlights();
            }

            public void changedUpdate(DocumentEvent e) {
                updateHighlights();
            }
        });
        // 初始化高亮
        SwingUtilities.invokeLater(() -> {
            String text = bodyArea.getText();
            java.util.List<VariableSegment> segments = VariableUtil.getVariableSegments(text);
            for (VariableSegment seg : segments) {
                // 判断变量状态：环境变量、临时变量或内置函数 - 与 EasyPostmanTextField 保持一致
                boolean isDefined = VariableResolver.isVariableDefined(seg.name)
                        || VariableUtil.isBuiltInFunction(seg.name);
                try {
                    highlighter.addHighlight(seg.start, seg.end, isDefined ? definedPainter : undefinedPainter);
                } catch (BadLocationException ignored) {
                }
            }
        });
        // 悬浮提示 - 与 EasyPostmanTextField 保持一致
        bodyArea.addMouseMotionListener(new MouseInputAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int pos = bodyArea.viewToModel2D(e.getPoint());
                String text = bodyArea.getText();
                java.util.List<VariableSegment> segments = VariableUtil.getVariableSegments(text);
                for (VariableSegment seg : segments) {
                    if (pos >= seg.start && pos <= seg.end) {
                        String varName = seg.name;

                        // 检查是否是内置函数
                        if (VariableUtil.isBuiltInFunction(varName)) {
                            String desc = currentVariables != null ?
                                    currentVariables.get(varName) :
                                    "Dynamic function generates value at runtime";
                            bodyArea.setToolTipText(buildTooltip(varName, desc, true, true));
                            return;
                        }

                        // 环境变量
                        String varValue = VariableResolver.resolveVariable(varName);
                        if (varValue != null) {
                            bodyArea.setToolTipText(buildTooltip(varName, varValue, false, true));
                        } else {
                            bodyArea.setToolTipText(buildTooltip(varName, "Variable not defined in current environment", false, false));
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
        // 只有HTTP模式才需要动态调整format控件的显示
        if (!isWebSocketMode && rawTypeComboBox != null && formatButton != null) {
            boolean isRaw = BODY_TYPE_RAW.equals(bodyType);
            rawTypeComboBox.setVisible(isRaw);
            formatButton.setVisible(isRaw);
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
        if (RAW_TYPE_JSON.equals(selectedFormat)) {
            String prettyJson = JsonUtil.toJsonPrettyStr(bodyText);
            bodyArea.setText(prettyJson);
        } else if (RAW_TYPE_XML.equals(selectedFormat)) {
            bodyArea.setText(XmlUtil.formatXml(bodyText));
        } else {
            log.debug("Unsupported format type or content is not JSON/XML");
        }

    }


    // getter方法，供主面板调用
    public String getBodyType() {
        return currentBodyType;
    }

    public String getRawBody() {
        return bodyArea != null ? bodyArea.getText().trim() : null;
    }

    /**
     * 初始化自动补全功能
     */
    private void initAutocomplete() {
        if (bodyArea == null) return;

        // 创建弹出窗口
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        if (parentWindow == null) {
            // 延迟初始化，等待组件被添加到窗口
            SwingUtilities.invokeLater(() -> {
                Window parent = SwingUtilities.getWindowAncestor(RequestBodyPanel.this);
                if (parent != null) {
                    initAutocompleteWindow(parent);
                }
            });
        } else {
            initAutocompleteWindow(parentWindow);
        }

        // 监听文档变化
        bodyArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> checkForAutocomplete());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> checkForAutocomplete());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> checkForAutocomplete());
            }
        });

        // 文本框键盘事件
        bodyArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (autocompleteWindow != null && autocompleteWindow.isVisible()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            int currentIndex = autocompleteList.getSelectedIndex();
                            int nextIndex = currentIndex + 1;
                            if (nextIndex >= autocompleteModel.getSize()) {
                                nextIndex = 0;
                            }
                            autocompleteList.setSelectedIndex(nextIndex);
                            autocompleteList.ensureIndexIsVisible(nextIndex);
                            e.consume();
                            break;
                        case KeyEvent.VK_UP:
                            int currentUpIndex = autocompleteList.getSelectedIndex();
                            int prevIndex = currentUpIndex - 1;
                            if (prevIndex < 0) {
                                prevIndex = autocompleteModel.getSize() - 1;
                            }
                            autocompleteList.setSelectedIndex(prevIndex);
                            autocompleteList.ensureIndexIsVisible(prevIndex);
                            e.consume();
                            break;
                        case KeyEvent.VK_ENTER:
                        case KeyEvent.VK_TAB:
                            insertSelectedVariable();
                            e.consume();
                            break;
                        case KeyEvent.VK_ESCAPE:
                            hideAutocomplete();
                            e.consume();
                            break;
                        default:
                            // No action needed for other keys
                            break;
                    }
                }
            }
        });
    }

    /**
     * 初始化自动补全窗口 - 与 EasyPostmanTextField 保持一致
     */
    private void initAutocompleteWindow(Window parent) {
        autocompleteWindow = new JWindow(parent);
        autocompleteWindow.setFocusableWindowState(false);

        autocompleteModel = new DefaultListModel<>();
        autocompleteList = new JList<>(autocompleteModel);
        autocompleteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        autocompleteList.setVisibleRowCount(10);
        autocompleteList.setBackground(POPUP_BACKGROUND);
        autocompleteList.setSelectionBackground(POPUP_SELECTION_BG);
        autocompleteList.setSelectionForeground(Color.BLACK);
        autocompleteList.setFont(bodyArea.getFont());
        // 固定列表宽度，防止内容过长导致横向滚动
        autocompleteList.setFixedCellWidth(384); // 400 - 边框和内边距

        // 自定义列表渲染器 - 显示图标、变量名和值/描述，支持长文本截断
        autocompleteList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JPanel panel = new JPanel(new BorderLayout(8, 0));
                panel.setOpaque(true);
                panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                // 设置固定大小，防止横向滚动
                panel.setPreferredSize(new Dimension(384, 32));
                panel.setMaximumSize(new Dimension(384, 32));

                if (isSelected) {
                    panel.setBackground(POPUP_SELECTION_BG);
                } else {
                    panel.setBackground(POPUP_BACKGROUND);
                }

                if (value != null && currentVariables != null) {
                    String varName = value.toString();
                    String varValue = currentVariables.get(varName);

                    // 判断是内置函数还是环境变量
                    boolean isBuiltIn = varName.startsWith("$");
                    Color labelColor = isBuiltIn ? BUILTIN_FUNCTION_COLOR : ENV_VARIABLE_COLOR;

                    // 使用彩色圆点代替 Emoji（更好的跨平台兼容性）
                    JPanel iconPanel = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                            // 获取字体度量信息以实现垂直对齐
                            int panelHeight = getHeight();

                            // 计算圆点垂直居中位置
                            int circleSize = 12;
                            int circleY = (panelHeight - circleSize) / 2;

                            // 绘制圆形图标
                            g2d.setColor(isBuiltIn ? BUILTIN_FUNCTION_COLOR : ENV_VARIABLE_COLOR);
                            g2d.fillOval(2, circleY, circleSize, circleSize);

                            // 绘制白色符号 - 垂直居中对齐
                            g2d.setColor(Color.WHITE);
                            g2d.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
                            FontMetrics symbolFm = g2d.getFontMetrics();
                            String symbol = isBuiltIn ? "$" : "E";
                            int symbolWidth = symbolFm.stringWidth(symbol);
                            int symbolAscent = symbolFm.getAscent();
                            int symbolDescent = symbolFm.getDescent();
                            int symbolHeight = symbolAscent + symbolDescent;

                            // 符号在圆点内垂直居中
                            int symbolX = 2 + (circleSize - symbolWidth) / 2;
                            int symbolY = circleY + (circleSize - symbolHeight) / 2 + symbolAscent;
                            g2d.drawString(symbol, symbolX, symbolY);

                            g2d.dispose();
                        }

                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(16, 24);
                        }
                    };
                    iconPanel.setOpaque(false);
                    panel.add(iconPanel, BorderLayout.WEST);

                    // 中间区域：使用固定宽度的面板容纳name和value，让它们各占一半空间
                    JPanel contentPanel = new JPanel(new GridLayout(1, 2, 8, 0));
                    contentPanel.setOpaque(false);

                    // 左侧：变量名（占一半空间）
                    JPanel namePanel = new JPanel(new BorderLayout());
                    namePanel.setOpaque(false);

                    String displayName = varName;
                    String fullName = varName;

                    // 变量名最大长度（约占一半宽度）
                    int maxNameLength = 25;
                    if (varName.length() > maxNameLength) {
                        displayName = varName.substring(0, maxNameLength - 3) + "...";
                    }

                    JLabel nameLabel = new JLabel(displayName);
                    nameLabel.setFont(bodyArea.getFont().deriveFont(Font.BOLD));
                    nameLabel.setForeground(labelColor);

                    // 为变量名添加工具提示
                    if (!displayName.equals(fullName)) {
                        nameLabel.setToolTipText(fullName);
                    }

                    namePanel.add(nameLabel, BorderLayout.WEST);
                    contentPanel.add(namePanel);

                    // 右侧：变量值或描述（占一半空间）
                    JPanel valuePanel = new JPanel(new BorderLayout());
                    valuePanel.setOpaque(false);

                    if (varValue != null && !varValue.isEmpty()) {
                        String displayValue = varValue;
                        String fullValue = varValue;

                        // 变量值最大长度（约占一半宽度）
                        int maxValueLength = 25;
                        if (varValue.length() > maxValueLength) {
                            displayValue = varValue.substring(0, maxValueLength - 3) + "...";
                        }

                        JLabel valueLabel = new JLabel(displayValue);
                        valueLabel.setFont(bodyArea.getFont().deriveFont(Font.PLAIN, bodyArea.getFont().getSize() - 1));
                        valueLabel.setForeground(Color.GRAY);

                        // 为值添加工具提示（显示完整内容）
                        if (!displayValue.equals(fullValue) || fullValue.length() > 20) {
                            // 格式化工具提示，支持换行
                            String tooltipText = formatTooltipText(fullValue);
                            valueLabel.setToolTipText("<html>" + tooltipText + "</html>");
                        }

                        valuePanel.add(valueLabel, BorderLayout.WEST);
                    }
                    contentPanel.add(valuePanel);

                    panel.add(contentPanel, BorderLayout.CENTER);

                    // 为整个面板添加工具提示（显示完整信息）
                    StringBuilder tooltipBuilder = new StringBuilder("<html>");
                    tooltipBuilder.append("<b>").append(escapeHtml(varName)).append("</b>");
                    if (varValue != null && !varValue.isEmpty()) {
                        tooltipBuilder.append("<br/>").append(escapeHtml(varValue));
                    }
                    tooltipBuilder.append("</html>");
                    panel.setToolTipText(tooltipBuilder.toString());
                }

                return panel;
            }
        });

        JScrollPane scrollPane = new JScrollPane(autocompleteList);
        // 禁用横向滚动条，只保留纵向滚动
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        autocompleteWindow.add(scrollPane);

        // 列表鼠标点击事件
        autocompleteList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 || e.getClickCount() == 1) {
                    insertSelectedVariable();
                }
            }
        });
    }

    /**
     * 检查是否需要显示自动补全 - 与 EasyPostmanTextField 保持一致
     */
    private void checkForAutocomplete() {
        if (bodyArea == null || autocompleteWindow == null) return;

        String text = bodyArea.getText();
        int caretPos = bodyArea.getCaretPosition();

        if (text == null || caretPos < 2) {
            hideAutocomplete();
            return;
        }

        // 查找光标前最近的 {{
        int openBracePos = text.lastIndexOf("{{", caretPos - 1);
        if (openBracePos == -1) {
            hideAutocomplete();
            return;
        }

        // 检查 {{ 之后是否有 }}
        int closeBracePos = text.indexOf("}}", openBracePos);
        if (closeBracePos != -1 && closeBracePos < caretPos) {
            hideAutocomplete();
            return;
        }

        // 获取 {{ 之后的文本作为过滤前缀
        String prefix = text.substring(openBracePos + 2, caretPos);

        // 过滤变量列表
        currentVariables = VariableResolver.filterVariables(prefix);

        if (currentVariables.isEmpty()) {
            hideAutocomplete();
            return;
        }

        autocompleteModel.clear();
        for (String varName : currentVariables.keySet()) {
            autocompleteModel.addElement(varName);
        }

        // 默认选中第一项
        if (autocompleteModel.getSize() > 0) {
            autocompleteList.setSelectedIndex(0);
            autocompleteList.ensureIndexIsVisible(0);
        }

        // 显示弹出菜单
        showAutocomplete();
    }

    /**
     * 显示自动补全弹出窗口 - 与 EasyPostmanTextField 保持一致
     */
    private void showAutocomplete() {
        if (autocompleteWindow == null || autocompleteModel.getSize() == 0) {
            return;
        }

        try {
            // 获取光标位置
            Rectangle rect = bodyArea.modelToView2D(bodyArea.getCaretPosition()).getBounds();
            Point screenPos = bodyArea.getLocationOnScreen();

            // 计算弹出窗口大小 - 固定宽度为400
            int itemHeight = 32; // 每项高度
            int popupWidth = 400;
            int popupHeight = Math.min(autocompleteModel.getSize() * itemHeight + 10, 320);

            // 设置窗口大小和位置
            autocompleteWindow.setSize(popupWidth, popupHeight);
            autocompleteWindow.setLocation(
                    screenPos.x + rect.x,
                    screenPos.y + rect.y + rect.height + 2
            );

            if (!autocompleteWindow.isVisible()) {
                autocompleteWindow.setVisible(true);
            }
        } catch (Exception e) {
            log.error("showAutocomplete error", e);
        }
    }

    /**
     * 隐藏自动补全列表
     */
    private void hideAutocomplete() {
        if (autocompleteWindow != null) {
            autocompleteWindow.setVisible(false);
        }
    }

    /**
     * 格式化工具提示文本，支持换行 - 与 EasyPostmanTextField 保持一致
     */
    private String formatTooltipText(String text) {
        if (text == null || text.length() <= 60) {
            return text;
        }

        StringBuilder formatted = new StringBuilder();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + 60, text.length());
            formatted.append(text, start, end);
            if (end < text.length()) {
                formatted.append("<br/>");
            }
            start = end + 1;
        }
        return formatted.toString();
    }

    /**
     * HTML转义，防止特殊字符破坏工具提示 - 与 EasyPostmanTextField 保持一致
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 构建美观的工具提示HTML - 与 EasyPostmanTextField 保持一致
     */
    private String buildTooltip(String varName, String content, boolean isBuiltIn, boolean isDefined) {
        StringBuilder tooltip = new StringBuilder();
        tooltip.append("<html><body style='padding: 8px; font-family: Arial, sans-serif;'>");

        // 标题部分 - 变量名
        String titleColor = isBuiltIn ? "#9C27B0" : (isDefined ? "#2E7D32" : "#D32F2F");
        String typeLabel = isBuiltIn ? "Built-in Function" : (isDefined ? "Environment Variable" : "Undefined Variable");
        String typeIcon = isBuiltIn ? "⚡" : (isDefined ? "✓" : "✗");

        tooltip.append("<div style='margin-bottom: 6px;'>");
        tooltip.append("<span style='font-size: 10px; color: ").append(titleColor).append(";'>");
        tooltip.append(typeIcon).append(" ").append(typeLabel);
        tooltip.append("</span></div>");

        // 变量名 - 粗体显示
        tooltip.append("<div style='margin-bottom: 1px;'>");
        tooltip.append("<b style='font-size: 10px; color: ").append(titleColor).append(";'>");
        tooltip.append(escapeHtml(varName));
        tooltip.append("</b></div>");

        // 分隔线
        tooltip.append("<hr style='border: none; border-top: 1px solid #E0E0E0; margin: 1px 0;'/>");

        // 内容部分 - 变量值或描述
        tooltip.append("<div style='margin-top: 1px; color: #424242; font-size: 10px;'>");

        if (isDefined && !isBuiltIn) {
            // 环境变量值
            tooltip.append("<span style='color: #757575;'>Value:</span><br/>");
            tooltip.append("<span style='font-family: Consolas, monospace; background-color: #F5F5F5; ");
            tooltip.append("padding: 4px 6px; border-radius: 3px; display: inline-block; margin-top: 1px;'>");

            // 限制显示长度，超过150字符截断
            String displayContent = content.length() > 150 ? content.substring(0, 150) + "..." : content;
            tooltip.append(escapeHtml(displayContent));
            tooltip.append("</span>");
        } else if (isBuiltIn) {
            // 内置函数描述
            tooltip.append("<span style='color: #757575; font-style: italic;'>");
            tooltip.append(escapeHtml(content));
            tooltip.append("</span>");
        } else {
            // 未定义变量警告
            tooltip.append("<span style='color: #D32F2F; font-weight: bold;'>⚠ ");
            tooltip.append(escapeHtml(content));
            tooltip.append("</span>");
        }

        tooltip.append("</div>");
        tooltip.append("</body></html>");

        return tooltip.toString();
    }

    /**
     * 插入选中的变量 - 与 EasyPostmanTextField 保持一致
     */
    private void insertSelectedVariable() {
        if (autocompleteList == null || bodyArea == null) return;

        String selected = autocompleteList.getSelectedValue();
        if (selected == null) return;

        try {
            String text = bodyArea.getText();
            int caretPos = bodyArea.getCaretPosition();

            // 查找光标前最近的双花括号开始位置
            int openBracePos = text.lastIndexOf("{{", caretPos - 1);
            if (openBracePos == -1) {
                hideAutocomplete();
                return;
            }

            // 构建新文本
            String before = text.substring(0, openBracePos);
            String after = text.substring(caretPos);

            // 检查光标后面是否已经有结束的双花括号
            boolean hasClosingBraces = after.startsWith("}}");
            String newText;
            int newCaretPos;

            if (hasClosingBraces) {
                // 如果后面已经有结束符，则只插入开始符和变量名，不再添加结束符
                newText = before + "{{" + selected + after;
                newCaretPos = before.length() + selected.length() + 4; // 光标位置在已存在的结束符之后
            } else {
                // 如果后面没有结束符，则添加完整的变量引用语法
                newText = before + "{{" + selected + "}}" + after;
                newCaretPos = before.length() + selected.length() + 4; // 光标位置在新添加的结束符之后
            }

            // 设置新文本并移动光标
            bodyArea.setText(newText);
            bodyArea.setCaretPosition(newCaretPos);

        } catch (Exception e) {
            log.error("insertSelectedVariable error", e);
        }

        hideAutocomplete();
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
